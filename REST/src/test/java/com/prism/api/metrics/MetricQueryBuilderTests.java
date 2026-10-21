package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain JUnit: the SQL generated for each metric type, filters and the guards. No database needed. */
class MetricQueryBuilderTests {

    private static final FocusLevelDefinition YEAR = new FocusLevelDefinition(1, 1, "Year", "sprint_year");
    private static final FocusLevelDefinition TEAM = new FocusLevelDefinition(12, 2, "Team", "team_name");

    private static MetricDefinition metric(MetricType type, String numerator, String denominator, String condition) {
        return new MetricDefinition(1, "m", type, numerator, denominator, condition, "M", "%", 2, null, null, null, true);
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
        assertTrue(query.sql().startsWith("SELECT team_name AS label, "), query.sql());
        assertTrue(query.sql().contains("WHERE sprint_year = ? AND team_name IS NULL\n"), query.sql());
        assertTrue(query.sql().endsWith("GROUP BY team_name\nORDER BY team_name"), query.sql());
        assertTrue(!query.sql().contains("OR '1'='1"), query.sql());
        assertEquals(List.of("2024' OR '1'='1"), query.parameters());
    }

    @Test
    void noFiltersNoWhere() {
        var query = MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), YEAR, List.of());
        assertTrue(!query.sql().contains("WHERE"), query.sql());
        assertTrue(query.parameters().isEmpty());
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
        var bad = new FocusLevelDefinition(99, 1, "Bad", "sprint_year; DROP TABLE x");
        assertThrows(IllegalStateException.class,
                () -> MetricQueryBuilder.build(metric(MetricType.SUM, "smf.a", null, null), bad, List.of()));
    }
}
