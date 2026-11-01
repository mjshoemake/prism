package com.prism.api.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain JUnit: {@code draft_metric} rules, warnings and generated SQL. */
class MetricDraftServiceTests {

    private final MetricDraftService service = new MetricDraftService();

    private MetricDraft draft(String type, String numerator, String denominator) {
        return service.draft("Change Failure Rate", type, numerator, denominator, null, null, "%", null);
    }

    @Test
    void validPercentageGivesInsertAndPreview() {
        var result = draft("percentage", "smf.failed_deployments", "smf.deployments");
        assertTrue(result.valid(), result.errors().toString());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        assertTrue(result.insertSql().contains("'Change Failure Rate', 'percentage', 'smf.failed_deployments', 'smf.deployments', NULL, 'Change Failure Rate', '%', 2,"),
                result.insertSql());
        assertTrue(result.insertSql().contains("ON CONFLICT (name) DO NOTHING"));
        assertTrue(result.insertSql().contains("SELECT f.fact_pk, ") && result.insertSql().contains("WHERE f.table_alias = 'smf'"),
                result.insertSql());
        assertTrue(result.previewSql().contains("NULLIF"), result.previewSql());
        assertTrue(result.previewSql().contains("SUM(smf.failed_deployments)"), result.previewSql());
        assertTrue(result.previewSql().contains("GROUP BY sd.year_month"), result.previewSql());
        assertTrue(result.previewSql().contains("td.team_name = ?"), result.previewSql());
        assertTrue(result.previewSql().contains("FROM \"demo_star\".\"Sprint_Metrics_Fact\" smf\n"), result.previewSql());
        assertTrue(result.calculation().startsWith("SUM(numerator) / SUM(denominator)"));
    }

    @Test
    void rowConditionAndTitleAreUsed() {
        var result = service.draft("Prod CFR", "percentage", "smf.failed", "smf.deployments",
                "smf.environment = 'production'", "Change failure rate (production)", "%", 1);
        assertTrue(result.valid(), result.errors().toString());
        assertTrue(result.previewSql().contains("FILTER (WHERE smf.environment = 'production')"), result.previewSql());
        assertTrue(result.insertSql().contains("'smf.environment = ''production'''"), result.insertSql()); // quotes doubled
        assertTrue(result.insertSql().contains("'Change failure rate (production)', '%', 1,"), result.insertSql());
    }

    @Test
    void percentageNeedsADenominator() {
        var result = draft("percentage", "smf.failed_deployments", null);
        assertFalse(result.valid());
        assertTrue(result.errors().get(0).contains("needs a denominator"));
        assertNull(result.insertSql());
        assertNull(result.previewSql());
    }

    @Test
    void sumTakesNoDenominator() {
        var result = draft("sum", "smf.failed_deployments", "smf.deployments");
        assertFalse(result.valid());
        assertTrue(result.errors().get(0).contains("single value"));
    }

    @Test
    void countTypeExplainsSumOfOne() {
        var result = draft("count", "1", null);
        assertFalse(result.valid());
        assertTrue(result.errors().get(0).contains("There is no Count type"));
    }

    @Test
    void unknownTypeListsTheTypes() {
        var result = draft("median", "smf.x", null);
        assertTrue(result.errors().get(0).contains("percentage, ratio, average, sum"), result.errors().toString());
    }

    @Test
    void forbiddenSequencesAreRejected() {
        var result = draft("percentage", "smf.a; DROP TABLE x", "smf.b");
        assertFalse(result.valid());
        assertTrue(result.errors().get(0).contains("forbidden"), result.errors().toString());
    }

    @Test
    void limitsAreChecked() {
        var result = service.draft("x".repeat(41), "sum", "smf.a", null, null, "t".repeat(61), "u".repeat(21), 7);
        assertEquals(4, result.errors().size(), result.errors().toString());
        assertTrue(service.draft("n", "sum", "x".repeat(4001), null, null, null, null, null).errors().stream()
                .anyMatch(e -> e.contains("longer than 4000")));
    }

    @Test
    void missingNameAndNumerator() {
        var result = service.draft(" ", "average", null, null, null, null, null, null);
        assertEquals(List.of("A name is required.", "A value expression is required (passed as the numerator)."),
                result.errors());
    }

    @Test
    void storedPercentageInARatioIsWarned() {
        var result = draft("percentage", "smf.pass_rate", "smf.tests");
        assertTrue(result.valid());
        assertEquals(1, result.warnings().size());
        assertTrue(result.warnings().get(0).contains("'pass_rate'") && result.warnings().get(0).contains("raw counts"));
    }

    @Test
    void averageOfARatioIsWarned() {
        var result = draft("average", "smf.completed / NULLIF(smf.committed, 0)", null);
        assertTrue(result.valid());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("average of averages")), result.warnings().toString());
    }

    @Test
    void sameNumeratorAndDenominatorIsWarned() {
        var result = draft("percentage", "smf.deployments", "smf.deployments");
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("always 100")), result.warnings().toString());
    }

    @Test
    void derivedLookingColumnsUseWholeWordParts() {
        assertEquals(List.of("pass_rate", "avg_velocity_3_sprints"),
                MetricDraftService.derivedLookingColumns("smf.pass_rate + smf.avg_velocity_3_sprints"));
        assertEquals(List.of(), MetricDraftService.derivedLookingColumns("smf.accurate_estimates + smf.ratings"));
        assertEquals(List.of(), MetricDraftService.derivedLookingColumns(null));
    }

    @Test
    void insertUsesTheFactOfTheNumeratorsAlias() {
        assertEquals("dep", MetricDraftService.factAlias("SUM(1) + dep.failed"));
        assertEquals("smf", MetricDraftService.factAlias("1"));
        assertEquals("smf", MetricDraftService.factAlias("\"Odd\".x"));
        var result = service.draft("Deploys", "sum", "dep.deployments", null, null, null, null, 0);
        assertTrue(result.insertSql().contains("WHERE f.table_alias = 'dep'"), result.insertSql());
    }

    @Test
    void literalDoublesQuotes() {
        assertEquals("'O''Brien'", MetricDraftService.literal("O'Brien"));
        assertEquals("NULL", MetricDraftService.literal(null));
    }
}
