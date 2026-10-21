package com.prism.api.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds a chart query from cached metadata. Nothing the browser sends ends up in the SQL text: the metric's
 * expressions and the focus-level columns come from the metadata tables, and filter values are bound as parameters.
 * <p>
 * Shape of every query (grouping and ordering by the focus-level column, as before SQL/012):
 * <pre>
 * SELECT &lt;column&gt; AS label, &lt;calculation&gt; AS value
 * FROM &lt;demo fact table and dimensions&gt;
 * [WHERE &lt;column&gt; = ? AND ...]
 * GROUP BY &lt;column&gt;
 * ORDER BY &lt;column&gt;
 * </pre>
 * The FROM clause is still the demo STAR schema; data sources and fact/dimension metadata replace it later
 * (docs/OVERVIEW.md §4.3).
 */
public final class MetricQueryBuilder {

    /** Today's only data source: the demo STAR schema in the Prism database. */
    static final String DEMO_FROM_CLAUSE = """
            FROM "prism"."Sprint_Metrics_Fact" smf
            JOIN "prism"."Sprint_Dim" sd ON smf.sprint_pk = sd.sprint_pk
            JOIN "prism"."Team_Dim" td   ON smf.team_pk   = td.team_pk""";

    /** Focus-level columns must be plain identifiers (they are written into the SQL). */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

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

    public static MetricQuery build(MetricDefinition metric, FocusLevelDefinition groupBy, List<ResolvedFilter> filters) {
        var groupColumn = column(groupBy);
        var sql = new StringBuilder()
                .append("SELECT ").append(groupColumn).append(" AS label, ")
                .append(calculation(metric)).append(" AS value\n")
                .append(DEMO_FROM_CLAUSE).append('\n');

        var parameters = new ArrayList<String>();
        var conditions = new ArrayList<String>();
        for (var filter : filters) {
            var column = column(filter.level());
            if (filter.value() == null) {
                conditions.add(column + " IS NULL");
            } else {
                conditions.add(column + " = ?");
                parameters.add(filter.value());
            }
        }
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append('\n');
        }
        sql.append("GROUP BY ").append(groupColumn).append('\n')
           .append("ORDER BY ").append(groupColumn);
        return new MetricQuery(sql.toString(), List.copyOf(parameters));
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

    private static String column(FocusLevelDefinition level) {
        if (!isIdentifier(level.columnName())) {
            throw new IllegalStateException("Focus level " + level.focusLevelPk() + " has an invalid column name.");
        }
        return level.columnName();
    }
}
