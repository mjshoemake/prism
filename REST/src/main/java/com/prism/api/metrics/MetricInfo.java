package com.prism.api.metrics;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A metric as the UI sees it (GET /api/metrics/list): everything needed to pick and display it, and nothing about
 * how it is calculated.
 *
 * @param scaleMode always set: zero_to_100 | fit | fixed (the type's default when the metric has none)
 */
public record MetricInfo(@JsonProperty("metric_pk") int metricPk,
                         String name,
                         @JsonProperty("metric_type") String metricType,
                         String title,
                         String unit,
                         @JsonProperty("decimal_places") int decimalPlaces,
                         @JsonProperty("scale_mode") String scaleMode,
                         @JsonProperty("scale_min") BigDecimal scaleMin,
                         @JsonProperty("scale_max") BigDecimal scaleMax) {
}
