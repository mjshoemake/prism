package com.prism.api.metrics;

import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * Metric data from a STAR schema inside the Prism database itself (connection method {@code prism_database}: the
 * demo schema, SQL/013). Builds the query with {@link MetricQueryBuilder} and runs it on Prism's own connection.
 * <p>
 * TODO (roadmap #3): client databases get their own provider with a read-only login and a statement time limit.
 */
@Component
public class PrismDatabaseDataProvider implements MetricDataProvider {

    private static final Logger log = LogCategory.SERVICE.logger(PrismDatabaseDataProvider.class);
    private static final Logger sqlLog = LogCategory.SQL.logger(PrismDatabaseDataProvider.class);

    public static final String CONNECTION_METHOD = "prism_database";

    private final JdbcTemplate jdbcTemplate;

    public PrismDatabaseDataProvider(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String connectionMethod() {
        log.debug("Started");
        return CONNECTION_METHOD;
    }

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
        return PerformanceLog.time("fetchChartData", () -> jdbcTemplate.queryForList(query.sql(), args));
    }
}
