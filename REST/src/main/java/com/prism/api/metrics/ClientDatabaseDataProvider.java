package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * Metric data from a database outside Prism's own. So far only the TEMPORARY development-only method
 * {@code direct_dev} (SQL/024; login from the data source's properties, refused unless
 * {@code prism.data-sources.allow-dev-connections=true}). The query runs exactly like the Prism database's
 * ({@link SqlMetricQueries}), on the data source's own checked, read-only pool ({@link ClientDatabaseConnections}).
 */
@Component
public class ClientDatabaseDataProvider implements MetricDataProvider {

    private static final Logger log = LogCategory.SERVICE.logger(ClientDatabaseDataProvider.class);

    private final ClientDatabaseConnections connections;

    public ClientDatabaseDataProvider(ClientDatabaseConnections connections) {
        this.connections = connections;
    }

    @Override
    public String connectionMethod() {
        log.debug("Started");
        return ClientDatabaseConnections.DIRECT_DEV;
    }

    /**
     * @throws MetricDataUnavailableException the connection isn't usable, or the query ran past its time limit
     */
    @Override
    public List<Map<String, Object>> chartRows(DataSourceDefinition dataSource, MetricDefinition metric,
                                               FocusLevelDefinition groupBy,
                                               List<MetricQueryBuilder.ResolvedFilter> filters) {
        log.debug("Started");
        var connection = connections.connection(dataSource.dataSourcePk());
        return SqlMetricQueries.chartRows(connection.jdbc(), connections.timeout(), dataSource, metric, groupBy,
                filters);
    }
}
