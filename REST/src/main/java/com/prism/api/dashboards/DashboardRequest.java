package com.prism.api.dashboards;

import java.util.List;

/**
 * Body of POST /api/dashboards and PUT /api/dashboards/{pk}: the whole dashboard (name, layout, chart areas). A PUT
 * replaces the chart areas with the ones sent; an area that isn't sent has no metric.
 */
public record DashboardRequest(String name, Integer layout, List<Dashboard.ChartArea> charts) {
}
