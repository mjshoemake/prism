package com.prism.api.metrics;

/**
 * Request body for {@code POST /api/metrics/sprint-metrics}.
 *
 * @param focusLevelColumn            column to group and order by (e.g. {@code year_month})
 * @param valueColumnsForSelectClause aggregate expression(s) appended to the SELECT list
 * @param whereClause                 full WHERE clause, including the {@code WHERE} keyword (may be empty)
 */
public record SprintMetricsRequest(
        String focusLevelColumn,
        String valueColumnsForSelectClause,
        String whereClause
) {
}
