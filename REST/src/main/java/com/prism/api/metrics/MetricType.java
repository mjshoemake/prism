package com.prism.api.metrics;

import java.util.Locale;

/**
 * How Prism calculates a metric ("prism"."Metrics".metric_type, SQL/012). Every type re-aggregates the raw fact
 * rows, so results stay correct at every drill-down level (never an average of averages; docs/OVERVIEW.md §4.1).
 */
public enum MetricType {
    /** SUM(numerator) / SUM(denominator) × 100. */
    PERCENTAGE("percentage", true, "zero_to_100"),
    /** SUM(numerator) / SUM(denominator). */
    RATIO("ratio", true, "fit"),
    /** Average of the value per fact row; rows without a value are ignored, not counted as zero. */
    AVERAGE("average", false, "fit"),
    /** SUM(value). */
    SUM("sum", false, "fit");

    private final String dbValue;
    private final boolean needsDenominator;
    private final String defaultScaleMode;

    MetricType(String dbValue, boolean needsDenominator, String defaultScaleMode) {
        this.dbValue = dbValue;
        this.needsDenominator = needsDenominator;
        this.defaultScaleMode = defaultScaleMode;
    }

    public String dbValue() {
        return dbValue;
    }

    public boolean needsDenominator() {
        return needsDenominator;
    }

    /** Y-axis scale used when the metric doesn't set one: percentages 0–100, everything else fitted to the data. */
    public String defaultScaleMode() {
        return defaultScaleMode;
    }

    /** @throws IllegalArgumentException for an unknown type */
    public static MetricType fromDbValue(String value) {
        if (value != null) {
            var normalized = value.trim().toLowerCase(Locale.ROOT);
            for (var type : values()) {
                if (type.dbValue.equals(normalized)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Unknown metric type: " + value);
    }
}
