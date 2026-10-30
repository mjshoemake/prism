package com.prism.api.metrics;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;

/**
 * In-memory copy of the metric metadata used to build chart queries: Metrics, their Facts with the linked
 * Dimensions (Facts_Dimensions_Assoc), and Focus_Levels with their area's dimension (SQL/013, 015, 016).
 * <ul>
 * <li>Loaded on first use and reloaded as a whole when older than {@code prism.metadata.cache-ttl} (default 15
 *     minutes). Metadata rarely changes, so this is the only refresh needed; a restart always starts fresh.</li>
 * <li>An ID that isn't cached triggers one reload (at most every {@link #MISS_RELOAD_INTERVAL}, so unknown IDs
 *     can't hammer the database); if it's still unknown, the request is rejected.</li>
 * <li>Rows that can't be used (unknown type, bad column name, a metric without a usable fact, two tables of one
 *     fact sharing an alias…) are skipped with a warning instead of failing the whole cache.</li>
 * </ul>
 * TODO: key by enterprise once metadata is enterprise-scoped (docs/OVERVIEW.md §4.2).
 */
@Component
public class MetricsMetadataCache {

    private static final Logger log = LogCategory.SERVICE.logger(MetricsMetadataCache.class);
    private static final Logger sqlLog = LogCategory.SQL.logger(MetricsMetadataCache.class);

    static final Duration MISS_RELOAD_INTERVAL = Duration.ofSeconds(10);

    private static final String METRICS_SQL = """
            SELECT metric_pk, name, metric_type, numerator_sql, denominator_sql, row_condition_sql, title, unit,
                   decimal_places, scale_mode, scale_min, scale_max, is_active, fact_pk
            FROM "prism"."Metrics"
            ORDER BY sort_order NULLS LAST, metric_pk
            """;

    private static final String FOCUS_LEVELS_SQL = """
            SELECT fl.focus_level_pk, fl.focus_area_pk, fa.dimension_pk, fl.name, fl.column_name
            FROM "prism"."Focus_Levels" fl
            JOIN "prism"."Focus_Areas" fa ON fa.focus_area_pk = fl.focus_area_pk
            """;

    private static final String FACTS_SQL = """
            SELECT fact_pk, schema_name, table_name, table_alias
            FROM "prism"."Facts"
            """;

    private static final String DIMENSIONS_SQL = """
            SELECT dimension_pk, schema_name, table_name, table_alias
            FROM "prism"."Dimensions"
            """;

    private static final String FACT_DIMENSIONS_SQL = """
            SELECT fact_pk, dimension_pk, fact_column, dimension_column
            FROM "prism"."Facts_Dimensions_Assoc"
            ORDER BY fact_pk, dimension_pk
            """;

    /** One consistent copy of the metadata. */
    private record Snapshot(Map<Integer, MetricDefinition> metrics, List<MetricDefinition> metricsInOrder,
                            Map<Integer, FocusLevelDefinition> focusLevels, Instant loadedAt) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final Duration ttl;
    private volatile Snapshot snapshot;

    public MetricsMetadataCache(JdbcTemplate jdbcTemplate,
                                @Value("${prism.metadata.cache-ttl:15m}") Duration ttl) {
        this.jdbcTemplate = jdbcTemplate;
        this.ttl = ttl;
    }

    /** Active metrics in display order (sort_order). */
    public List<MetricDefinition> activeMetrics() {
        log.debug("Started");
        return current().metricsInOrder().stream().filter(MetricDefinition::active).toList();
    }

    /** An active metric by ID; empty if unknown or inactive (after one reload attempt). */
    public Optional<MetricDefinition> metric(int metricPk) {
        log.debug("Started");
        var found = current().metrics().get(metricPk);
        if (found == null) {
            found = reloadAfterMiss().metrics().get(metricPk);
        }
        return Optional.ofNullable(found).filter(MetricDefinition::active);
    }

    /** A focus level by ID; empty if unknown (after one reload attempt). */
    public Optional<FocusLevelDefinition> focusLevel(int focusLevelPk) {
        log.debug("Started");
        var found = current().focusLevels().get(focusLevelPk);
        if (found == null) {
            found = reloadAfterMiss().focusLevels().get(focusLevelPk);
        }
        return Optional.ofNullable(found);
    }

    /** Drops the cache so the next request reloads it (e.g. after metadata is edited through Prism). */
    public void invalidate() {
        log.debug("Started");
        snapshot = null;
    }

    private Snapshot current() {
        log.debug("Started");
        var s = snapshot;
        if (s == null || s.loadedAt().plus(ttl).isBefore(Instant.now())) {
            s = reload(false);
        }
        return s;
    }

    private Snapshot reloadAfterMiss() {
        log.debug("Started");
        return reload(true);
    }

    /**
     * Reloads unless another thread just did. {@code afterMiss}: reload only if the copy is older than
     * {@link #MISS_RELOAD_INTERVAL}.
     */
    private synchronized Snapshot reload(boolean afterMiss) {
        log.debug("Started");
        var s = snapshot;
        var now = Instant.now();
        if (s != null) {
            var age = Duration.between(s.loadedAt(), now);
            boolean fresh = afterMiss ? age.compareTo(MISS_RELOAD_INTERVAL) < 0 : age.compareTo(ttl) <= 0;
            if (fresh) {
                return s;
            }
        }
        s = load(now);
        snapshot = s;
        return s;
    }

    private Snapshot load(Instant now) {
        log.debug("Started");
        var facts = loadFacts();

        sqlLog.debug("SQL: {}", METRICS_SQL);
        var metrics = new HashMap<Integer, MetricDefinition>();
        var inOrder = new ArrayList<MetricDefinition>();
        for (var row : jdbcTemplate.queryForList(METRICS_SQL)) {
            var metric = toMetric(row, facts);
            if (metric != null) {
                metrics.put(metric.metricPk(), metric);
                inOrder.add(metric);
            }
        }

        sqlLog.debug("SQL: {}", FOCUS_LEVELS_SQL);
        var levels = new HashMap<Integer, FocusLevelDefinition>();
        for (var row : jdbcTemplate.queryForList(FOCUS_LEVELS_SQL)) {
            var pk = intValue(row, "focus_level_pk");
            if (row.get("dimension_pk") == null) {
                log.warn("Focus level {} skipped: its focus area has no dimension_pk", pk);
                continue;
            }
            var level = new FocusLevelDefinition(pk, intValue(row, "focus_area_pk"), intValue(row, "dimension_pk"),
                    (String) row.get("name"), (String) row.get("column_name"));
            if (MetricQueryBuilder.isIdentifier(level.columnName())) {
                levels.put(level.focusLevelPk(), level);
            } else {
                log.warn("Focus level {} skipped: column_name is not a plain identifier", level.focusLevelPk());
            }
        }
        log.info("Metric metadata loaded: {} metrics, {} facts, {} focus levels", metrics.size(), facts.size(),
                levels.size());
        return new Snapshot(Map.copyOf(metrics), List.copyOf(inOrder), Map.copyOf(levels), now);
    }

    /** Facts with their linked dimensions. A fact or link that can't be used is skipped with a warning. */
    private Map<Integer, FactDefinition> loadFacts() {
        log.debug("Started");
        sqlLog.debug("SQL: {}", DIMENSIONS_SQL);
        var dimensions = new HashMap<Integer, TableDefinition>();
        for (var row : jdbcTemplate.queryForList(DIMENSIONS_SQL)) {
            var pk = intValue(row, "dimension_pk");
            try {
                dimensions.put(pk, table(row));
            } catch (IllegalArgumentException e) {
                log.warn("Dimension {} skipped: {}", pk, e.getMessage());
            }
        }

        sqlLog.debug("SQL: {}", FACT_DIMENSIONS_SQL);
        var joins = new HashMap<Integer, List<FactDefinition.Join>>();
        for (var row : jdbcTemplate.queryForList(FACT_DIMENSIONS_SQL)) {
            var factPk = intValue(row, "fact_pk");
            var dimensionPk = intValue(row, "dimension_pk");
            var dimension = dimensions.get(dimensionPk);
            if (dimension == null) {
                log.warn("Link fact {} -> dimension {} skipped: the dimension wasn't loaded", factPk, dimensionPk);
                continue;
            }
            try {
                joins.computeIfAbsent(factPk, k -> new ArrayList<>()).add(new FactDefinition.Join(dimensionPk, dimension,
                        (String) row.get("fact_column"), (String) row.get("dimension_column")));
            } catch (IllegalArgumentException e) {
                log.warn("Link fact {} -> dimension {} skipped: {}", factPk, dimensionPk, e.getMessage());
            }
        }

        sqlLog.debug("SQL: {}", FACTS_SQL);
        var facts = new HashMap<Integer, FactDefinition>();
        for (var row : jdbcTemplate.queryForList(FACTS_SQL)) {
            var pk = intValue(row, "fact_pk");
            try {
                facts.put(pk, new FactDefinition(pk, table(row), joins.getOrDefault(pk, List.of())));
            } catch (IllegalArgumentException e) {
                log.warn("Fact {} skipped: {}", pk, e.getMessage());
            }
        }
        return facts;
    }

    private static TableDefinition table(Map<String, Object> row) {
        log.debug("Started");
        return new TableDefinition((String) row.get("schema_name"), (String) row.get("table_name"),
                (String) row.get("table_alias"));
    }

    /** Maps a Metrics row, or returns null (with a warning) if it can't be used. */
    private static MetricDefinition toMetric(Map<String, Object> row, Map<Integer, FactDefinition> facts) {
        log.debug("Started");
        var pk = intValue(row, "metric_pk");
        try {
            var fact = row.get("fact_pk") == null ? null : facts.get(intValue(row, "fact_pk"));
            if (fact == null) {
                throw new IllegalArgumentException("no usable fact (fact_pk " + row.get("fact_pk") + ")");
            }
            var type = MetricType.fromDbValue((String) row.get("metric_type"));
            var numerator = (String) row.get("numerator_sql");
            var denominator = (String) row.get("denominator_sql");
            if (numerator == null || numerator.isBlank()) {
                throw new IllegalArgumentException("numerator_sql is empty");
            }
            if (type.needsDenominator() != (denominator != null && !denominator.isBlank())) {
                throw new IllegalArgumentException("denominator_sql doesn't match type " + type.dbValue());
            }
            var metric = new MetricDefinition(pk, (String) row.get("name"), type, numerator,
                    type.needsDenominator() ? denominator : null, blankToNull((String) row.get("row_condition_sql")),
                    (String) row.get("title"), (String) row.get("unit"), intValue(row, "decimal_places"),
                    (String) row.get("scale_mode"), (BigDecimal) row.get("scale_min"), (BigDecimal) row.get("scale_max"),
                    Boolean.TRUE.equals(row.get("is_active")), fact);
            MetricQueryBuilder.calculation(metric); // rejects forbidden sequences now rather than per request
            return metric;
        } catch (RuntimeException e) {
            log.warn("Metric {} skipped: {}", pk, e.getMessage());
            return null;
        }
    }

    private static int intValue(Map<String, Object> row, String column) {
        log.debug("Started");
        return ((Number) row.get(column)).intValue();
    }

    private static String blankToNull(String value) {
        log.debug("Started");
        return value == null || value.isBlank() ? null : value;
    }
}
