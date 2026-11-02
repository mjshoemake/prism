package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * Metric data from a STAR schema inside the Prism database itself (connection method {@code prism_database}: the
 * demo schema {@code demo_star}, SQL/023). Runs the query ({@link SqlMetricQueries}) on the read-only metric query
 * login ({@link MetricQueryConnection}, SQL/022), never on Prism's own.
 * <p>
 * Client databases get a provider of their own ({@link ClientDatabaseDataProvider}); so far only the temporary
 * development method {@code direct_dev}, until the secrets store exists (roadmap).
 */
@Component
public class PrismDatabaseDataProvider implements MetricDataProvider {

    private static final Logger log = LogCategory.SERVICE.logger(PrismDatabaseDataProvider.class);

    public static final String CONNECTION_METHOD = "prism_database";

    private final MetricQueryConnection connection;

    public PrismDatabaseDataProvider(MetricQueryConnection connection) {
        this.connection = connection;
    }

    @Override
    public String connectionMethod() {
        log.debug("Started");
        return CONNECTION_METHOD;
    }

    /**
     * @throws MetricDataUnavailableException the read-only login isn't usable, or the query ran past its time limit
     */
    @Override
    public List<Map<String, Object>> chartRows(DataSourceDefinition dataSource, MetricDefinition metric,
                                               FocusLevelDefinition groupBy,
                                               List<MetricQueryBuilder.ResolvedFilter> filters) {
        log.debug("Started");
        return SqlMetricQueries.chartRows(connection.jdbc(), connection.timeout(), dataSource, metric, groupBy,
                filters);
    }
}
