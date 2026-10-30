package com.prism.api.metrics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds a chart query from cached metadata. Nothing the browser sends ends up in the SQL text: the metric's
 * expressions, its fact table, the dimensions and the focus-level columns come from the metadata tables, and filter
 * values are bound as parameters.
 * <p>
 * Shape of every query:
 * <pre>
 * SELECT &lt;dim&gt;.&lt;column&gt; AS label, &lt;calculation&gt; AS value
 * FROM &lt;metric's fact table&gt; &lt;alias&gt;
 * [JOIN &lt;dimension&gt; &lt;alias&gt; ON &lt;fact&gt;.&lt;column&gt; = &lt;dimension&gt;.&lt;column&gt; ...]
 * [WHERE &lt;dim&gt;.&lt;column&gt; = ? AND ...]
 * GROUP BY &lt;dim&gt;.&lt;column&gt;
 * ORDER BY &lt;dim&gt;.&lt;column&gt;
 * </pre>
 * Only the dimensions a query needs are joined (SQL/016): the grouping level's, the filters', and any dimension whose
 * alias the metric's expressions use. Joins are INNER, so every fact row must have its dimension row (a foreign key;
 * {@code review_star_schema} rule S4).
 */
public final class MetricQueryBuilder {

    /** Focus-level and join columns must be plain identifiers (they are written into the SQL). */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** {@code alias.} in a metric expression: an alias not preceded by part of another name. */
    private static final Pattern ALIAS_REFERENCE = Pattern.compile("(?<![A-Za-z0-9_.\"])([a-z_][a-z0-9_]*)\\s*\\.");

    /**
     * Cheap guard for metric expressions until they are parsed against an allow-list (backlog #1c): no statement
     * separators or comments. Metric rows are written by trusted admins only.
     */
    private static final Pattern FORBIDDEN_IN_EXPRESSION = Pattern.compile(";|--|/\\*|\\*/");

    private MetricQueryBuilder() {
    }

    /** A drill-down filter resolved against the metadata: its focus level and the value to match (null = empty). */
    public record ResolvedFilter(FocusLevelDefinition level, String value) {
    }

    /**
     * @throws IllegalArgumentException a focus level whose dimension isn't linked to the metric's fact (a request the
     *                                  caller should answer with 400)
     * @throws IllegalStateException    unusable metadata (no fact, a bad column name or expression)
     */
    public static MetricQuery build(MetricDefinition metric, FocusLevelDefinition groupBy, List<ResolvedFilter> filters) {
        var fact = metric.fact();
        if (fact == null) {
            throw new IllegalStateException("Metric " + metric.metricPk() + " has no fact table.");
        }
        var joined = new HashSet<Integer>(dimensionsUsedBy(metric, fact));
        var groupColumn = column(metric, fact, groupBy, joined);
        var calculation = calculation(metric);

        var parameters = new ArrayList<String>();
        var conditions = new ArrayList<String>();
        for (var filter : filters) {
            var column = column(metric, fact, filter.level(), joined);
            if (filter.value() == null) {
                conditions.add(column + " IS NULL");
            } else {
                conditions.add(column + " = ?");
                parameters.add(filter.value());
            }
        }

        var sql = new StringBuilder()
                .append("SELECT ").append(groupColumn).append(" AS label, ").append(calculation).append(" AS value\n")
                .append(fromClause(fact, joined)).append('\n');
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append('\n');
        }
        sql.append("GROUP BY ").append(groupColumn).append('\n')
           .append("ORDER BY ").append(groupColumn);
        return new MetricQuery(sql.toString(), List.copyOf(parameters));
    }

    /** {@code FROM fact alias} plus a JOIN for each needed dimension, in the fact's link order. */
    static String fromClause(FactDefinition fact, Set<Integer> dimensionPks) {
        var from = new StringBuilder("FROM ").append(fact.table().sqlReference());
        for (var join : fact.joins()) {
            if (dimensionPks.contains(join.dimensionPk())) {
                from.append("\nJOIN ").append(join.dimension().sqlReference())
                    .append(" ON ").append(fact.table().alias()).append('.').append(join.factColumn())
                    .append(" = ").append(join.dimension().alias()).append('.').append(join.dimensionColumn());
            }
        }
        return from.toString();
    }

    /** Linked dimensions whose alias appears in the metric's numerator, denominator or row condition. */
    static Set<Integer> dimensionsUsedBy(MetricDefinition metric, FactDefinition fact) {
        var aliases = new HashSet<String>();
        for (var expression : new String[] {metric.numeratorSql(), metric.denominatorSql(), metric.rowConditionSql()}) {
            if (expression != null) {
                var matcher = ALIAS_REFERENCE.matcher(expression);
                while (matcher.find()) {
                    aliases.add(matcher.group(1));
                }
            }
        }
        var used = new HashSet<Integer>();
        for (var join : fact.joins()) {
            if (aliases.contains(join.dimension().alias())) {
                used.add(join.dimensionPk());
            }
        }
        return used;
    }

    /** The metric's value expression, built from its type (see {@link MetricType}). */
    static String calculation(MetricDefinition metric) {
        var decimals = metric.decimalPlaces();
        var condition = metric.rowConditionSql();
        var value = expression(metric.numeratorSql(), "numerator");
        return switch (metric.type()) {
            case PERCENTAGE -> "ROUND((%s)::DECIMAL / NULLIF((%s)::DECIMAL, 0) * 100, %d)".formatted(
                    aggregate("SUM", value, condition),
                    aggregate("SUM", expression(metric.denominatorSql(), "denominator"), condition), decimals);
            case RATIO -> "ROUND((%s)::DECIMAL / NULLIF((%s)::DECIMAL, 0), %d)".formatted(
                    aggregate("SUM", value, condition),
                    aggregate("SUM", expression(metric.denominatorSql(), "denominator"), condition), decimals);
            // AVG ignores NULLs: rows without a value don't count as zero.
            case AVERAGE -> "ROUND(%s, %d)".formatted(aggregate("AVG", "(" + value + ")::DECIMAL", condition), decimals);
            case SUM -> "ROUND((%s)::DECIMAL, %d)".formatted(aggregate("SUM", value, condition), decimals);
        };
    }

    /** {@code SUM(x)}, or {@code SUM(x) FILTER (WHERE condition)} when the metric has a row condition. */
    private static String aggregate(String function, String expression, String condition) {
        var call = function + "(" + expression + ")";
        return condition == null ? call : call + " FILTER (WHERE " + expression(condition, "row condition") + ")";
    }

    private static String expression(String sql, String what) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalStateException("Metric " + what + " is empty.");
        }
        if (FORBIDDEN_IN_EXPRESSION.matcher(sql).find()) {
            throw new IllegalStateException("Metric " + what + " contains a forbidden character sequence.");
        }
        return sql.trim();
    }

    static boolean isIdentifier(String name) {
        return name != null && IDENTIFIER.matcher(name).matches();
    }

    /** {@code alias.column} for a focus level; records the dimension as joined. */
    private static String column(MetricDefinition metric, FactDefinition fact, FocusLevelDefinition level,
                                 Set<Integer> joined) {
        if (!isIdentifier(level.columnName())) {
            throw new IllegalStateException("Focus level " + level.focusLevelPk() + " has an invalid column name.");
        }
        var join = fact.join(level.dimensionPk()).orElseThrow(() -> new IllegalArgumentException(
                "Focus level '" + level.name() + "' can't be used with metric '" + metric.name()
                        + "': its dimension isn't linked to the metric's fact table."));
        joined.add(join.dimensionPk());
        return join.dimension().alias() + "." + level.columnName();
    }
}
