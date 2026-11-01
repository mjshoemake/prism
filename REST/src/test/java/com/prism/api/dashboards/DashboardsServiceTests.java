package com.prism.api.dashboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain JUnit: the rules a dashboard must follow before it is saved. */
class DashboardsServiceTests {

    private static Dashboard.ChartArea area(Integer position, Integer metricPk) {
        return new Dashboard.ChartArea(position, metricPk);
    }

    private static String refused(DashboardRequest request) {
        return assertThrows(IllegalArgumentException.class, () -> DashboardsService.validate(request)).getMessage();
    }

    @Test
    void validDashboardIsTrimmedAndSortedByPosition() {
        var valid = DashboardsService.validate(new DashboardRequest("  Team health ", 4,
                List.of(area(3, 2), area(1, 1), area(4, 5))));
        assertEquals("Team health", valid.name());
        assertEquals(4, valid.layout());
        assertEquals(List.of(1, 3, 4), valid.charts().stream().map(Dashboard.ChartArea::position).toList());
    }

    @Test
    void hiddenAreasBeyondTheLayoutAreKept() {
        // Switching 4 -> 1 keeps the metrics of areas 2-4 (owner, 2026-10-31): they come back when switching to 4.
        var valid = DashboardsService.validate(new DashboardRequest("Mine", 1,
                List.of(area(1, 1), area(2, 2), area(3, 3), area(4, 4))));
        assertEquals(4, valid.charts().size());
    }

    @Test
    void noChartsYetIsFine() {
        assertTrue(DashboardsService.validate(new DashboardRequest("Empty", 2, null)).charts().isEmpty());
    }

    @Test
    void nameIsRequiredAndLimited() {
        assertTrue(refused(new DashboardRequest("   ", 1, List.of())).contains("name is required"));
        assertTrue(refused(new DashboardRequest(null, 1, List.of())).contains("name is required"));
        assertTrue(refused(new DashboardRequest("x".repeat(61), 1, List.of())).contains("60"));
        DashboardsService.validate(new DashboardRequest("x".repeat(60), 1, List.of()));
    }

    @Test
    void layoutIsOneTwoOrFour() {
        for (var bad : Arrays.asList(null, 0, 3, 5)) {
            assertTrue(refused(new DashboardRequest("d", bad, List.of())).contains("1, 2 or 4"), String.valueOf(bad));
        }
    }

    @Test
    void chartAreasAreChecked() {
        assertTrue(refused(new DashboardRequest("d", 4, List.of(area(5, 1)))).contains("1 to 4"));
        assertTrue(refused(new DashboardRequest("d", 4, List.of(area(0, 1)))).contains("1 to 4"));
        assertTrue(refused(new DashboardRequest("d", 4, List.of(area(null, 1)))).contains("1 to 4"));
        assertTrue(refused(new DashboardRequest("d", 4, List.of(area(2, null)))).contains("needs a metric_pk"));
        assertTrue(refused(new DashboardRequest("d", 4, List.of(area(2, 1), area(2, 3)))).contains("given twice"));
        assertTrue(refused(new DashboardRequest("d", 4, Arrays.asList(area(1, 1), null))).contains("1 to 4"));
    }

    @Test
    void nullRequestIsRefused() {
        assertTrue(refused(null).contains("required"));
    }

    // ---- Y-axis scale per chart (SQL/021) --------------------------------------------------------------------------

    private static Dashboard.ChartArea scaled(String mode, String min, String max) {
        return new Dashboard.ChartArea(1, 7, mode, min == null ? null : new BigDecimal(min),
                max == null ? null : new BigDecimal(max));
    }

    private static Dashboard.ChartArea validChart(Dashboard.ChartArea chart) {
        return DashboardsService.validate(new DashboardRequest("d", 1, List.of(chart))).charts().get(0);
    }

    private static String refusedChart(Dashboard.ChartArea chart) {
        return refused(new DashboardRequest("d", 1, List.of(chart)));
    }

    @Test
    void scaleDefaultsToTheMetricsOwn() {
        assertEquals("metric", validChart(area(1, 7)).yAxisMode());
        assertEquals("metric", validChart(scaled(null, null, null)).yAxisMode());
        assertEquals("metric", validChart(scaled("  ", null, null)).yAxisMode());
    }

    @Test
    void namedScalesAreKept() {
        for (var mode : List.of("metric", "zero_100", "fit")) {
            var chart = validChart(scaled(mode, null, null));
            assertEquals(mode, chart.yAxisMode());
            assertNull(chart.yAxisMin());
            assertNull(chart.yAxisMax());
        }
    }

    @Test
    void boundsAreDroppedForScalesWithoutThem() {
        // A form may still hold the custom bounds after the user switched to another scale.
        var chart = validChart(scaled("fit", "10", "90"));
        assertEquals("fit", chart.yAxisMode());
        assertNull(chart.yAxisMin());
        assertNull(chart.yAxisMax());
    }

    @Test
    void customScaleNeedsAMinimumAMaximumOrBoth() {
        var both = validChart(scaled("custom", "60", "100"));
        assertEquals(new BigDecimal("60"), both.yAxisMin());
        assertEquals(new BigDecimal("100"), both.yAxisMax());
        assertNull(validChart(scaled("custom", "60", null)).yAxisMax());
        assertNull(validChart(scaled("custom", null, "150.5")).yAxisMin());
        validChart(scaled("custom", "-20", "0"));
        assertTrue(refusedChart(scaled("custom", null, null)).contains("minimum, a maximum or both"));
    }

    @Test
    void customMinimumMustBeBelowTheMaximum() {
        assertTrue(refusedChart(scaled("custom", "50", "50")).contains("less than the maximum"));
        assertTrue(refusedChart(scaled("custom", "80", "20")).contains("less than the maximum"));
    }

    @Test
    void customBoundsMustFitTheColumn() {
        assertTrue(refusedChart(scaled("custom", null, "10000000000")).contains("between"));
        assertTrue(refusedChart(scaled("custom", "-10000000000", null)).contains("between"));
        validChart(scaled("custom", null, "9999999999.9999"));
    }

    @Test
    void storedBoundsComeBackPlain() {
        // NUMERIC(14,4) reads back as 60.0000; the JSON should say 60, not 60.0000 or 6E+1.
        assertEquals("60", DashboardsService.decimal(new BigDecimal("60.0000")).toString());
        assertEquals("150.5", DashboardsService.decimal(new BigDecimal("150.5000")).toString());
        assertEquals("0", DashboardsService.decimal(new BigDecimal("0.0000")).toString());
        assertNull(DashboardsService.decimal(null));
    }

    @Test
    void unknownScaleIsRefused() {
        assertTrue(refusedChart(scaled("zero_to_100", null, null)).contains("unknown y-axis scale"));
    }
}
