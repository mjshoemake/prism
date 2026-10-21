package com.prism.api.metrics;

import java.util.List;

/**
 * A generated chart query: SQL with {@code ?} placeholders and the values to bind (filter values only; never SQL).
 */
public record MetricQuery(String sql, List<String> parameters) {
}
