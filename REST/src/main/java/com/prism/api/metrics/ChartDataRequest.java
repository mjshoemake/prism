package com.prism.api.metrics;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of POST /api/metrics/data. Only IDs and values: the server builds all SQL from the metadata tables.
 * <pre>{"metric_pk": 1, "focus_level_pk": 3, "filters": [{"focus_level_pk": 1, "operator": "eq", "value": "2024"}]}</pre>
 *
 * @param filters drill-down filters, combined with AND; null or empty = none
 */
public record ChartDataRequest(@JsonProperty("metric_pk") Integer metricPk,
                               @JsonProperty("focus_level_pk") Integer focusLevelPk,
                               List<Filter> filters) {

    /**
     * "focus level = value".
     *
     * @param operator only "eq" today (null = "eq"); a field so other operators can be added without changing the contract
     * @param value    the bar label clicked; null matches rows where the column is empty
     */
    public record Filter(@JsonProperty("focus_level_pk") Integer focusLevelPk, String operator, String value) {
    }
}
