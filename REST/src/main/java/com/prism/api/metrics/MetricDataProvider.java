package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

/**
 * The one way the server reaches metric data (docs/OVERVIEW.md §4.3, "metric data provider" seam). The request is
 * source-neutral (metric, focus level, filters, all resolved from the metadata), so a provider can be a database
 * connection that builds SQL with {@link MetricQueryBuilder}, or later a client-run API that receives the same
 * request.
 * <p>
 * Each provider serves one data-source connection method; {@link MetricDataProviders} picks it by the metric's data
 * source.
 */
public interface MetricDataProvider {

    /** The {@link DataSourceDefinition#connectionMethod()} this provider serves, e.g. {@code prism_database}. */
    String connectionMethod();

    /**
     * Chart rows {@code [{key, label, value}, ...]}: the metric grouped by {@code groupBy}'s key, filtered by
     * {@code filters} (key values, combined with AND), in key order. {@code key} is text (or null).
     *
     * @throws IllegalArgumentException a request that can't be answered (e.g. a focus level the metric's fact
     *                                  can't be sliced by); the controller answers 400
     */
    List<Map<String, Object>> chartRows(DataSourceDefinition dataSource, MetricDefinition metric,
                                        FocusLevelDefinition groupBy, List<MetricQueryBuilder.ResolvedFilter> filters);
}
