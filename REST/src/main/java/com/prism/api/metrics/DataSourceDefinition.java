package com.prism.api.metrics;

/**
 * One row of "prism"."Data_Sources" (SQL/013), as cached by {@link MetricsMetadataCache}: only what's needed to pick
 * the {@link MetricDataProvider} for it. Connection details and credentials stay out of the cache.
 *
 * @param connectionMethod {@code db_connection_method} for a database source (e.g. {@code prism_database},
 *                         {@code firewall_allow_list}), or {@link #API} for a client-run API source
 */
public record DataSourceDefinition(int dataSourcePk, String name, String connectionMethod) {

    /** Connection method of a data source reached through a client-run API (second phase, OVERVIEW §4.3). */
    public static final String API = "api";
}
