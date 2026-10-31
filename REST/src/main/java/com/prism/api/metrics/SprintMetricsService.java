package com.prism.api.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;
import com.prism.common.utils.LogUtils;

@Service
public class SprintMetricsService {

    private static final Logger log = LogCategory.SERVICE.logger(SprintMetricsService.class);
    private static final Logger resultSetLog = LogCategory.RESULT_SET.logger(SprintMetricsService.class);
    private static final Logger sqlLog = LogCategory.SQL.logger(SprintMetricsService.class);

    private static final String FOCUS_AREAS_SQL = """
            SELECT focus_area_pk, name
            FROM "prism"."Focus_Areas" fa
            ORDER BY fa.sort_order, fa.focus_area_pk
            """;

    private static final String FOCUS_LEVELS_SQL = """
            SELECT focus_level_pk, focus_area_pk, name, column_name, drill_to_focus_level_pk
            FROM "prism"."Focus_Levels" fl
            WHERE fl.focus_area_pk = ?
            ORDER BY fl.sort_order, fl.focus_level_pk
            """;

    /** Most filters a chart request may carry (a drill-down adds one per click). */
    static final int MAX_FILTERS = 25;

    private final JdbcTemplate jdbcTemplate;
    private final MetricsMetadataCache metadata;
    private final MetricDataProviders dataProviders;

    /**
     * @param jdbcTemplate  Prism's own database: focus areas and levels (metadata). Metric data is reached only
     *                      through {@code dataProviders}.
     */
    public SprintMetricsService(JdbcTemplate jdbcTemplate, MetricsMetadataCache metadata,
                                MetricDataProviders dataProviders) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadata = metadata;
        this.dataProviders = dataProviders;
    }

    /** Active metrics in display order: display fields only, never SQL. */
    public List<MetricInfo> fetchMetricList() {
        log.debug("Started");
        return metadata.activeMetrics().stream().map(MetricDefinition::toInfo).toList();
    }

    /**
     * Chart rows {@code [{key, label, value}, ...]} for one metric grouped by one focus level's key (in key order),
     * filtered by the drill-down filters (each a level and a KEY value: the {@code key} of a row the browser was given)
     * from the data provider of the metric's data source. All SQL comes from the cached metadata; filter
     * values are bound as parameters.
     *
     * @throws IllegalArgumentException with a user-facing message for an unknown metric/focus level, an unsupported
     *                                  operator, too many filters, a focus level the metric can't be sliced by, or
     *                                  a data source this server can't reach (the controller answers 400)
     */
    public List<Map<String, Object>> fetchChartData(ChartDataRequest request) {
        log.debug("Started");
        if (request.metricPk() == null || request.focusLevelPk() == null) {
            throw new IllegalArgumentException("metric_pk and focus_level_pk are required.");
        }
        var metric = metadata.metric(request.metricPk())
                .orElseThrow(() -> new IllegalArgumentException("Unknown metric: " + request.metricPk()));
        var groupBy = metadata.focusLevel(request.focusLevelPk())
                .orElseThrow(() -> new IllegalArgumentException("Unknown focus level: " + request.focusLevelPk()));

        var requested = request.filters() == null ? List.<ChartDataRequest.Filter>of() : request.filters();
        if (requested.size() > MAX_FILTERS) {
            throw new IllegalArgumentException("Too many filters (at most " + MAX_FILTERS + ").");
        }
        var filters = new ArrayList<MetricQueryBuilder.ResolvedFilter>();
        for (var filter : requested) {
            if (filter == null || filter.focusLevelPk() == null) {
                throw new IllegalArgumentException("Each filter needs a focus_level_pk.");
            }
            if (filter.operator() != null && !"eq".equals(filter.operator())) {
                throw new IllegalArgumentException("Unsupported filter operator: " + filter.operator());
            }
            var level = metadata.focusLevel(filter.focusLevelPk())
                    .orElseThrow(() -> new IllegalArgumentException("Unknown focus level: " + filter.focusLevelPk()));
            filters.add(new MetricQueryBuilder.ResolvedFilter(level, filter.value()));
        }

        var dataSource = metadata.dataSource(metric.fact().dataSourcePk())
                .orElseThrow(() -> new IllegalArgumentException("Metric '" + metric.name() + "' has no data source."));
        var rows = dataProviders.forDataSource(dataSource).chartRows(dataSource, metric, groupBy, List.copyOf(filters));
        resultSetLog.debug("Chart rows: metricPk={} focusLevelPk={} size={}", metric.metricPk(),
                groupBy.focusLevelPk(), rows.size());
        return rows;
    }

    public List<Map<String, Object>> fetchFocusAreas() {
        log.debug("Started");
        sqlLog.debug("SQL: {}", FOCUS_AREAS_SQL);
        
        List<Map<String, Object>> result = PerformanceLog.time("fetchFocusAreas", () -> jdbcTemplate.queryForList(FOCUS_AREAS_SQL));
        resultSetLog.debug("Focus Areas: size={}", result.size());
        LogUtils.debug(resultSetLog, result, "   ", true); 
        return result;
    }

    /**
     * Focus levels of one focus area, in display order (sort_order), each with its drill-down target
     * ({@code drill_to_focus_level_pk}, SQL/015; null = no drill-down from that level).
     */
    public List<Map<String, Object>> fetchFocusLevels(int focusAreaPk) {
        log.debug("Started");
        sqlLog.debug("SQL: {} [focusAreaPk={}]", FOCUS_LEVELS_SQL, focusAreaPk);
        List<Map<String, Object>> result = PerformanceLog.time("fetchFocusLevels",
                () -> jdbcTemplate.queryForList(FOCUS_LEVELS_SQL, focusAreaPk));
        resultSetLog.debug("Focus Levels: size={}", result.size());
        LogUtils.debug(resultSetLog, result, "   ", true);
        return result;
    }
}
