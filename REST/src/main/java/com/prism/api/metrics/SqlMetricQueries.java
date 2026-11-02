package com.prism.api.metrics;

import java.sql.SQLException;
import java.sql.Types;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * Runs a metric's chart query on a database connection: shared by every provider that reaches the STAR schema with
 * SQL ({@link PrismDatabaseDataProvider}, {@link ClientDatabaseDataProvider}), so they build, time out and audit
 * queries the same way. The query comes from {@link MetricQueryBuilder}; each run is audited
 * ({@code METRIC_QUERY ...}, without filter values).
 */
public final class SqlMetricQueries {

    private static final Logger log = LogCategory.SERVICE.logger(SqlMetricQueries.class);
    private static final Logger sqlLog = LogCategory.SQL.logger(SqlMetricQueries.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    /** Postgres SQLSTATE for a statement cancelled by statement_timeout or a JDBC query timeout. */
    static final String QUERY_CANCELED = "57014";

    private SqlMetricQueries() {
    }

    /**
     * Chart rows for the metric, grouped by {@code groupBy} and filtered by {@code filters}.
     *
     * @param timeout the time limit already set on {@code jdbc} (named in the timeout message)
     * @throws MetricDataUnavailableException the query ran past its time limit
     */
    public static List<Map<String, Object>> chartRows(JdbcTemplate jdbc, Duration timeout,
                                                      DataSourceDefinition dataSource, MetricDefinition metric,
                                                      FocusLevelDefinition groupBy,
                                                      List<MetricQueryBuilder.ResolvedFilter> filters) {
        log.debug("Started");
        var query = MetricQueryBuilder.build(metric, groupBy, filters);
        sqlLog.debug("SQL: {} {}", query.sql(), query.parameters());
        // Types.OTHER sends each value untyped, so Postgres reads it like the quoted literal the old UI sent
        // (e.g. '2024' compared with an integer column), with the same results.
        Object[] args = query.parameters().stream().map(v -> new SqlParameterValue(Types.OTHER, v)).toArray();
        long start = System.nanoTime();
        try {
            var rows = PerformanceLog.time("fetchChartData", () -> jdbc.queryForList(query.sql(), args));
            audit(dataSource, metric, groupBy, filters, rows.size(), start, "ok");
            return rows;
        } catch (DataAccessException e) {
            if (QUERY_CANCELED.equals(sqlState(e))) {
                audit(dataSource, metric, groupBy, filters, 0, start, "timeout");
                throw new MetricDataUnavailableException("The query for '" + metric.name() + "' took longer than "
                        + timeout.toSeconds() + " s and was stopped. Try fewer filters or a coarser focus level.", e);
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
