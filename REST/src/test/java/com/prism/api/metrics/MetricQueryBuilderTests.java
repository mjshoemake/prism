package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Plain JUnit: the SQL generated for each metric type, the FROM / JOINs from metadata, filters and the guards. */
class MetricQueryBuilderTests {

    private static final int SPRINT_DIM = 1;
    private static final int TEAM_DIM = 2;
    private static final int OTHER_DIM = 9;

    /** The demo schema, as SQL/013 seeds it. */
    static final FactDefinition DEMO_FACT = new FactDefinition(1, new TableDefinition("prism", "Sprint_Metrics_Fact", "smf"),
            List.of(new FactDefinition.Join(SPRINT_DIM, new TableDefinition("prism", "Sprint_Dim", "sd"), "sprint_pk", "sprint_pk"),
                    new FactDefinition.Join(TEAM_DIM, new TableDefinition("prism", "Team_Dim", "td"), "team_pk", "team_pk")));

    private static final FocusLevelDefinition YEAR = new FocusLevelDefinition(1, 1, SPRINT_DIM, "Year", "sprint_year");
    private static final FocusLevelDefinition TEAM = new FocusLevelDefinition(12, 2, TEAM_DIM, "Team", "team_name");

    private static MetricDefinition metric(MetricType type, String numerator, String denominator, String condition) {
        return new MetricDefinition(1, "m", type, numerator, denominator, condition, "M", "%", 2, null, null, null, true,
                DEMO_FACT);
    }

    @Test
    void percentageIsRatioOfSumsTimes100WithZeroGuard() {
        var sql = MetricQueryBuilder.calculation(metric(MetricType.PERCENTAGE, "smf.a", "smf.b", null));
        assertEquals("ROUND((SUM(smf.a))::DECIMAL / NULLIF((SUM(smf.b))::DECIMAL, 0) * 100, 2)", sql);
    }

    @Test
    void ratioHasNo100() {
        var sql = MetricQueryBuilder.calculation(metric(MetricType.RATIO, "smf.a", "smf.b", null));
        assertEquals("ROUND((SUM(smf.a))::DECIMAL / NULLIF((SUM(smf.b))::DECIMAL, 0), 2)", sql);
    }

    @Test
    void averageIgnoresMissingValues() {
        var sql = MetricQueryBuilder.calculation(metric(MetricType.AVERAGE, "smf.v", null, null));
        assertEquals("ROUND(AVG((smf.v)::DECIMAL), 2)", sql);
    }

    @Test
    void sumIsRounded() {
        var sql = MetricQueryBuilder.calculation(metric(MetricType.SUM, "1", null, null));
        assertEquals("ROUND((SUM(1))::DECIMAL, 2)", sql);
    }

    @Test
    void rowConditionAppliesToEveryAggregate() {
        var sql = MetricQueryBuilder.calculation(metric(MetricType.PERCENTAGE, "smf.a", "smf.b", "smf.c > 0"));
        assertEquals("ROUND((SUM(smf.a) FILTER (WHERE smf.c > 0))::DECIMAL"
                + " / NULLIF((SUM(smf.b) FILTER (WHERE smf.c > 0))::DECIMAL, 0) * 100, 2)", sql);
    }

    @Test
    void filtersAreBoundNotWritten() {
        var query = MetricQueryBuilder.build(metric(MetricType.PERCENTAGE, "smf.a", "smf.b", null), TEAM,
                List.of(new MetricQueryBuilder.ResolvedFilter(YEAR, "2024' OR '1'='1"),
                        new MetricQueryBuilder.ResolvedFilter(TEAM, null)));
        assertTrue(query.sql().startsWith("SELECT td.team_name AS label, "), query.sql());
        assertTrue(query.sql().contains("WHERE sd.sprint_year = ? AND td.team_name IS NULL\n"), query.sql());
        assertTrue(query.sql().endsWith("GROUP BY td.team_name\nORDER BY td.team_name"), query.sql());
        assertFalse(query.sql().contains("OR '1'='1"), query.sql());
        assertEquals(List.of("2024' OR '1'='1"), query.parameters());
    }

    @Test
    void bothDimensionsJoinedGiveTheOldDemoFromClause() {
        var query = MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), TEAM,
                List.of(new MetricQueryBuilder.ResolvedFilter(YEAR, "2024")));
        assertTrue(query.sql().contains("""
                FROM "prism"."Sprint_Metrics_Fact" smf
                JOIN "prism"."Sprint_Dim" sd ON smf.sprint_pk = sd.sprint_pk
                JOIN "prism"."Team_Dim" td ON smf.team_pk = td.team_pk
                WHERE"""), query.sql());
    }

    @Test
    void onlyNeededDimensionsAreJoined() {
        var query = MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), YEAR, List.of());
        assertTrue(query.sql().contains("FROM \"prism\".\"Sprint_Metrics_Fact\" smf\nJOIN \"prism\".\"Sprint_Dim\" sd ON smf.sprint_pk = sd.sprint_pk\nGROUP BY"),
                query.sql());
        assertFalse(query.sql().contains("Team_Dim"), query.sql());
        assertFalse(query.sql().contains("WHERE"), query.sql());
        assertTrue(query.parameters().isEmpty());
    }

    @Test
    void dimensionAliasesInTheMetricAreJoined() {
        var withTeam = metric(MetricType.PERCENTAGE, "smf.a", "smf.b", "td.level_1_name IS NOT NULL");
        var query = MetricQueryBuilder.build(withTeam, YEAR, List.of());
        assertTrue(query.sql().contains("JOIN \"prism\".\"Team_Dim\" td ON smf.team_pk = td.team_pk"), query.sql());
        assertEquals(Set.of(TEAM_DIM), MetricQueryBuilder.dimensionsUsedBy(withTeam, DEMO_FACT));
        // Not an alias: a quoted name, part of a longer name, or a number.
        assertEquals(Set.of(), MetricQueryBuilder.dimensionsUsedBy(
                metric(MetricType.SUM, "\"x\".td + smf.std.a + 1.5 + smf.a", null, null), DEMO_FACT));
    }

    @Test
    void levelOfAnUnlinkedDimensionIsRefused() {
        var other = new FocusLevelDefinition(50, 3, OTHER_DIM, "Service", "service_name");
        var error = assertThrows(IllegalArgumentException.class,
                () -> MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), other, List.of()));
        assertTrue(error.getMessage().contains("'Service'"), error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null),
                YEAR, List.of(new MetricQueryBuilder.ResolvedFilter(other, "x"))));
    }

    @Test
    void metricWithoutAFactIsRefused() {
        var noFact = new MetricDefinition(1, "m", MetricType.SUM, "smf.a", null, null, "M", "", 2, null, null, null,
                true, null);
        assertThrows(IllegalStateException.class, () -> MetricQueryBuilder.build(noFact, YEAR, List.of()));
    }

    @Test
    void rejectsStatementSeparatorsAndComments() {
        for (var bad : List.of("smf.a; DROP TABLE x", "smf.a -- x", "smf.a /* x */")) {
            assertThrows(IllegalStateException.class,
                    () -> MetricQueryBuilder.calculation(metric(MetricType.SUM, bad, null, null)), bad);
        }
    }

    @Test
    void rejectsNonIdentifierColumns() {
        var bad = new FocusLevelDefinition(99, 1, SPRINT_DIM, "Bad", "sprint_year; DROP TABLE x");
        assertThrows(IllegalStateException.class,
                () -> MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), bad, List.of()));
    }

    @Test
    void tableAndJoinNamesAreChecked() {
        assertThrows(IllegalArgumentException.class, () -> new TableDefinition("prism", "Bad\" x", "b"));
        assertThrows(IllegalArgumentException.class, () -> new TableDefinition("prism", "T", "Upper"));
        assertThrows(IllegalArgumentException.class, () -> new TableDefinition("prism", "T", "a; drop"));
        assertThrows(IllegalArgumentException.class, () -> new TableDefinition("", "T", "t"));
        assertEquals("\"prism\".\"Sprint_Dim\" sd", new TableDefinition("prism", "Sprint_Dim", "sd").sqlReference());
        assertThrows(IllegalArgumentException.class,
                () -> new FactDefinition.Join(1, new TableDefinition("p", "D", "d"), "a b", "a"));
    }

    @Test
    void aFactAndADimensionCantShareAnAlias() {
        assertThrows(IllegalArgumentException.class, () -> new FactDefinition(1, new TableDefinition("p", "F", "x"),
                List.of(new FactDefinition.Join(1, new TableDefinition("p", "D", "x"), "d_pk", "d_pk"))));
    }
}
