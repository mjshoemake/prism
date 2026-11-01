package com.prism.api.dashboards;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A saved dashboard as the API returns it (SQL/020): name, layout (1, 2 or 4 chart areas) and the metric in each
 * area. {@code charts} can hold positions beyond the layout: they are kept but not shown.
 */
public record Dashboard(@JsonProperty("dashboard_pk") int dashboardPk, String name, int layout,
                        List<ChartArea> charts) {

    /**
     * The metric shown in one chart area, and its y-axis scale (SQL/021). Positions are 1–4 in reading order (in the
     * 2-chart layout: top, bottom). {@code yAxisMode} is one of {@link YAxisMode#ALL}; the bounds are used only by
     * {@link YAxisMode#CUSTOM}.
     */
    public record ChartArea(Integer position,
                            @JsonProperty("metric_pk") Integer metricPk,
                            @JsonProperty("y_axis_mode") String yAxisMode,
                            @JsonProperty("y_axis_min") BigDecimal yAxisMin,
                            @JsonProperty("y_axis_max") BigDecimal yAxisMax) {

        /** An area with the metric's own scale. */
        public ChartArea(Integer position, Integer metricPk) {
            this(position, metricPk, YAxisMode.METRIC, null, null);
        }
    }

    /** Y-axis scale modes of a chart area (SQL/021, docs/OVERVIEW.md §4.3). */
    public static final class YAxisMode {
        /** The metric's own default (Metrics.scale_mode). */
        public static final String METRIC = "metric";
        /** 0 to 100, extended above 100 when a value exceeds it. */
        public static final String ZERO_100 = "zero_100";
        /** 0 to just above the highest value. */
        public static final String FIT = "fit";
        /** Hard minimum and/or maximum; bars beyond the maximum are marked on the chart. */
        public static final String CUSTOM = "custom";
        public static final Set<String> ALL = Set.of(METRIC, ZERO_100, FIT, CUSTOM);

        private YAxisMode() {
        }
    }
}
