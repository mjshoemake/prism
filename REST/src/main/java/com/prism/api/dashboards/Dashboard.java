package com.prism.api.dashboards;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A saved dashboard as the API returns it (SQL/020): name, layout (1, 2 or 4 chart areas) and the metric in each
 * area. {@code charts} can hold positions beyond the layout: they are kept but not shown.
 */
public record Dashboard(@JsonProperty("dashboard_pk") int dashboardPk, String name, int layout,
                        List<ChartArea> charts) {

    /** The metric shown in one chart area. Positions are 1–4 in reading order (in the 2-chart layout: top, bottom). */
    public record ChartArea(Integer position, @JsonProperty("metric_pk") Integer metricPk) {
    }
}
