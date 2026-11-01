package com.prism.api.metrics;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * Metric data from a STAR schema inside the Prism database itself (connection method {@code prism_database}: the
 * demo schema, SQL/013). Builds the query with {@link MetricQueryBuilder} and runs it on the read-only metric query
 * login ({@link MetricQueryConnection}, SQL/022), never on Prism's own. Each query is audited
 * ({@code METRIC_QUERY ...}, without filter values).
 * <p>
 * TODO (roadmap: client data sources): client databases get a provider of their own, with credentials from the secrets
 * store; it can reuse {@link MetricQueryConnection}'s checks.
 */
@Component
public class PrismDatabaseDataProvider implements MetricDataProvider {

    private static final Logger log = LogCategory.SERVICE.logger(PrismDatabaseDataProvider.class);
    private static final Logger sqlLog = LogCategory.SQL.logger(PrismDatabaseDataProvider.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    public static final String CONNECTION_METHOD = "prism_database";

    /** Postgres SQLSTATE for a statement cancelled by statement_timeout or a JDBC query timeout. */
    static final String QUERY_CANCELED = "57014";

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
        var query = MetricQueryBuilder.build(metric, groupBy, filters);
        sqlLog.debug("SQL: {} {}", query.sql(), query.parameters());
        // Types.OTHER sends each value untyped, so Postgres reads it like the quoted literal the old UI sent
        // (e.g. '2024' compared with an integer column), with the same results.
        Object[] args = query.parameters().stream().map(v -> new SqlParameterValue(Types.OTHER, v)).toArray();
        var jdbc = connection.jdbc();
        long start = System.nanoTime();
        try {
            var rows = PerformanceLog.time("fetchChartData", () -> jdbc.queryForList(query.sql(), args));
            audit(dataSource, metric, groupBy, filters, rows.size(), start, "ok");
            return rows;
        } catch (DataAccessException e) {
            if (QUERY_CANCELED.equals(sqlState(e))) {
                audit(dataSource, metric, groupBy, filters, 0, start, "timeout");
                throw new MetricDataUnavailableException("The query for '" + metric.name() + "' took longer than "
                        + connection.timeout().toSeconds() + " s and was stopped. Try fewer filters or a coarser "
                        + "focus level.", e);
            }
            audit(dataSource, metric, groupBy, filters, 0, start, "error");
            throw e;
        }
    }

    private static void audit(DataSourceDefinition dataSource, MetricDefinition metric, FocusLevelDefinition groupBy,
                              List<MetricQueryBuilder.ResolvedFilter> filters, int rows, long start, String outcome) {
        log.debug("Started");
        audit.info("METRIC_QUERY dataSource={} metric={} focusLevel={} filters={} rows={} ms={} outcome={}",
                dataSource.dataSourcePk(), metric.metricPk(), groupBy.focusLevelPk(), filters.size(), rows,
                (System.nanoTime() - start) / 1_000_000, outcome);
    }

    /** The SQLSTATE of the first SQLException in the cause chain, or null. */
    static String sqlState(Throwable e) {
        log.debug("Started");
        for (var t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof SQLException sql && sql.getSQLState() != null) {
                return sql.getSQLState();
            }
        }
        return null;
    }
}
