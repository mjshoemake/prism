package com.prism.api.dashboards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
