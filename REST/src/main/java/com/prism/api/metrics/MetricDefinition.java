package com.prism.api.metrics;

import java.math.BigDecimal;

/**
 * One row of "prism"."Metrics" (SQL/012), as cached by {@link MetricsMetadataCache}. Holds the metric's SQL pieces,
 * so it never leaves the server: the API returns {@link MetricInfo} instead.
 *
 * @param numeratorSql    numerator (percentage, ratio) or value (average, sum)
 * @param denominatorSql  denominator (percentage, ratio); null otherwise
 * @param rowConditionSql optional condition applied to every aggregate of the metric; null if none
 * @param scaleMode       null = the type's default
 * @param fact            the fact table the metric is calculated from, with its linked dimensions (SQL/016)
 */
public record MetricDefinition(int metricPk, String name, MetricType type, String numeratorSql, String denominatorSql,
                               String rowConditionSql, String title, String unit, int decimalPlaces, String scaleMode,
                               BigDecimal scaleMin, BigDecimal scaleMax, boolean active, FactDefinition fact) {

    /** The scale the chart uses: the metric's own setting, else the type's default. */
    public String effectiveScaleMode() {
        return scaleMode != null ? scaleMode : type.defaultScaleMode();
    }

    /** What the browser may see: display fields only, never SQL. */
    public MetricInfo toInfo() {
        return new MetricInfo(metricPk, name, type.dbValue(), title, unit, decimalPlaces, effectiveScaleMode(),
                scaleMin, scaleMax);
    }
}
