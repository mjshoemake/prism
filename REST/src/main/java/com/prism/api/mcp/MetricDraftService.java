package com.prism.api.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import com.prism.api.metrics.FactDefinition;
import com.prism.api.metrics.FocusLevelDefinition;
import com.prism.api.metrics.MetricDefinition;
import com.prism.api.metrics.MetricQueryBuilder;
import com.prism.api.metrics.MetricType;
import com.prism.api.metrics.TableDefinition;
import com.prism.common.logging.LogCategory;

/**
 * Turns a metric idea into a Prism metric definition (docs/MCP.md §4.2, tool {@code draft_metric}). Applies the same
 * rules as the metadata cache and the {@code Metrics} table's constraints, builds the SQL with the real
 * {@link MetricQueryBuilder}, and flags likely mistakes. Pure: it never touches a database.
 */
@Service
public class MetricDraftService {

    private static final Logger log = LogCategory.SERVICE.logger(MetricDraftService.class);

    /** Column limits of "prism"."Metrics" (SQL/008, SQL/012). */
    static final int MAX_NAME = 40;
    static final int MAX_TITLE = 60;
    static final int MAX_UNIT = 20;
    /** Input limit per expression (docs/MCP.md §4.4). */
    static final int MAX_EXPRESSION = 4000;
    static final int DEFAULT_DECIMALS = 2;

    /** Identifier parts that suggest a stored ratio, percentage or average rather than a raw count. */
    private static final Set<String> DERIVED_WORDS = Set.of("pct", "percent", "percentage", "ratio", "rate", "avg",
            "average", "mean");
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** Example fact for the SQL preview: the demo schema (fact smf, dimensions sd and td), as seeded by SQL/013. */
    private static final FactDefinition PREVIEW_FACT = new FactDefinition(0,
            new TableDefinition("prism", "Sprint_Metrics_Fact", "smf"),
            List.of(new FactDefinition.Join(1, new TableDefinition("prism", "Sprint_Dim", "sd"), "sprint_pk", "sprint_pk"),
                    new FactDefinition.Join(2, new TableDefinition("prism", "Team_Dim", "td"), "team_pk", "team_pk")));

    /** Example level and filter for the SQL preview (demo schema columns). */
    private static final FocusLevelDefinition PREVIEW_GROUP = new FocusLevelDefinition(0, 0, 1, "Year & Month", "year_month");
    private static final FocusLevelDefinition PREVIEW_FILTER = new FocusLevelDefinition(0, 0, 2, "Team", "team_name");

    /** The first {@code alias.} in an expression: the fact the metric belongs to, for the INSERT. */
    private static final Pattern ALIAS = Pattern.compile("(?<![A-Za-z0-9_.\"])([a-z_][a-z0-9_]*)\\s*\\.");
    private static final String DEFAULT_FACT_ALIAS = "smf";

    public MetricDraft draft(String name, String type, String numerator, String denominator, String rowCondition,
                             String title, String unit, Integer decimals) {
        log.debug("Started");
        var errors = new ArrayList<String>();
        var warnings = new ArrayList<String>();

        name = trimToNull(name);
        title = trimToNull(title);
        unit = trimToNull(unit);
        numerator = trimToNull(numerator);
        denominator = trimToNull(denominator);
        rowCondition = trimToNull(rowCondition);
        int places = decimals == null ? DEFAULT_DECIMALS : decimals;

        if (name == null) {
            errors.add("A name is required.");
        } else if (name.length() > MAX_NAME) {
            errors.add("The name is longer than " + MAX_NAME + " characters.");
        }
        if (title == null) {
            title = name;
        }
        if (title != null && title.length() > MAX_TITLE) {
            errors.add("The title is longer than " + MAX_TITLE + " characters.");
        }
        if (unit != null && unit.length() > MAX_UNIT) {
            errors.add("The unit is longer than " + MAX_UNIT + " characters.");
        }
        if (places < 0 || places > 6) {
            errors.add("Decimal places must be between 0 and 6.");
        }
        for (var expression : new String[] {numerator, denominator, rowCondition}) {
            if (expression != null && expression.length() > MAX_EXPRESSION) {
                errors.add("An expression is longer than " + MAX_EXPRESSION + " characters.");
            }
        }

        MetricType metricType = null;
        try {
            metricType = MetricType.fromDbValue(type);
        } catch (IllegalArgumentException e) {
            errors.add(type != null && type.trim().equalsIgnoreCase("count")
                    ? "There is no Count type: use type 'sum' with value 1 and a row condition (e.g. to count sprints "
                            + "with no churn: value 1, condition 'smf.added + smf.removed = 0')."
                    : "Unknown type '" + type + "'. Use one of: " + typeNames() + ".");
        }

        if (numerator == null) {
            errors.add(metricType != null && !metricType.needsDenominator()
                    ? "A value expression is required (passed as the numerator)."
                    : "A numerator is required.");
        }
        if (metricType != null) {
            if (metricType.needsDenominator() && denominator == null) {
                errors.add("Type '" + metricType.dbValue() + "' needs a denominator.");
            }
            if (!metricType.needsDenominator() && denominator != null) {
                errors.add("Type '" + metricType.dbValue() + "' takes a single value, not a denominator. For a share "
                        + "or rate use 'percentage'; for 'A per B' use 'ratio'.");
            }
        }

        String previewSql = null;
        if (errors.isEmpty()) {
            var definition = new MetricDefinition(0, name, metricType, numerator, denominator, rowCondition, title,
                    unit, places, null, null, null, true, PREVIEW_FACT);
            try {
                var query = MetricQueryBuilder.build(definition, PREVIEW_GROUP,
                        List.of(new MetricQueryBuilder.ResolvedFilter(PREVIEW_FILTER, "Team A")));
                previewSql = query.sql() + "\n-- ? = 'Team A' (filter values are always bound as parameters)";
            } catch (IllegalStateException e) {
                errors.add(e.getMessage() + " (no ';', comments or empty expressions)");
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        if (metricType != null) {
            warnings.addAll(warningsFor(metricType, numerator, denominator));
        }
        if (!errors.isEmpty()) {
            return new MetricDraft(false, List.copyOf(errors), List.copyOf(warnings),
                    metricType == null ? null : calculation(metricType), null, null);
        }
        return new MetricDraft(true, List.of(), List.copyOf(warnings), calculation(metricType),
                insertSql(name, metricType, numerator, denominator, rowCondition, title, unit, places), previewSql);
    }

    /** What Prism calculates for a type, in words. */
    static String calculation(MetricType type) {
        log.debug("Started");
        return switch (type) {
            case PERCENTAGE -> "SUM(numerator) / SUM(denominator) × 100, recalculated from the fact rows at every "
                    + "drill-down level (ratio of sums); a zero denominator gives no value.";
            case RATIO -> "SUM(numerator) / SUM(denominator), recalculated from the fact rows at every drill-down "
                    + "level; a zero denominator gives no value.";
            case AVERAGE -> "AVG(value) over the fact rows: the average per fact row; rows without a value are "
                    + "ignored, not counted as zero.";
            case SUM -> "SUM(value) over the fact rows.";
        };
    }

    private static List<String> warningsFor(MetricType type, String numerator, String denominator) {
        log.debug("Started");
        var warnings = new ArrayList<String>();
        for (var expression : new String[] {numerator, denominator}) {
            var derived = derivedLookingColumns(expression);
            if (!derived.isEmpty()) {
                warnings.add("'" + String.join("', '", derived) + "' looks like a stored percentage, ratio or average. "
                        + (type.needsDenominator()
                                ? "Use the raw counts it was calculated from, so Prism can add them up before dividing."
                                : "That's fine only if it's a genuine per-row measure; averaging stored ratios or "
                                        + "averages gives an average of averages.")
                        + " See prism://guide/metric-types.");
            }
        }
        if (type == MetricType.AVERAGE && numerator != null && numerator.contains("/")) {
            warnings.add("This averages a per-row ratio (average of averages). For a ratio across rows use type "
                    + "'percentage' or 'ratio' with a numerator and a denominator.");
        }
        if (type.needsDenominator() && numerator != null && numerator.equals(denominator)) {
            warnings.add("The numerator and denominator are the same, so the value is always "
                    + (type == MetricType.PERCENTAGE ? "100" : "1") + " (or empty). Did you mean to add a row condition "
                    + "to only one of them? Use a numerator such as 'CASE WHEN … THEN 1 ELSE 0 END' instead.");
        }
        return warnings;
    }

    /** Column names in an expression with a part like pct, ratio, rate or avg (e.g. smf.pass_rate). */
    static List<String> derivedLookingColumns(String expression) {
        log.debug("Started");
        if (expression == null) {
            return List.of();
        }
        var found = new ArrayList<String>();
        var matcher = IDENTIFIER.matcher(expression);
        while (matcher.find()) {
            var identifier = matcher.group();
            var parts = Arrays.stream(identifier.toLowerCase(Locale.ROOT).split("_")).collect(Collectors.toSet());
            if (parts.stream().anyMatch(DERIVED_WORDS::contains) && !found.contains(identifier)) {
                found.add(identifier);
            }
        }
        return found;
    }

    /**
     * A re-runnable INSERT for "prism"."Metrics": appended at the end of the display order; skipped if the name exists.
     * The metric belongs to the fact whose alias its expressions use (SQL/016); nothing is inserted if there's none.
     */
    static String insertSql(String name, MetricType type, String numerator, String denominator, String rowCondition,
                            String title, String unit, int places) {
        log.debug("Started");
        return """
                -- Fact: the one whose alias is '%s' (if several facts share it, set fact_pk yourself).
                INSERT INTO "prism"."Metrics"
                    (fact_pk, name, metric_type, numerator_sql, denominator_sql, row_condition_sql, title, unit, decimal_places, sort_order)
                SELECT f.fact_pk, %s, %s, %s, %s, %s, %s, %s, %d,
                       (SELECT COALESCE(MAX(sort_order), 0) + 10 FROM "prism"."Metrics")
                FROM "prism"."Facts" f
                WHERE f.table_alias = %s
                ORDER BY f.fact_pk
                LIMIT 1
                ON CONFLICT (name) DO NOTHING;""".formatted(factAlias(numerator), literal(name), literal(type.dbValue()),
                literal(numerator), literal(denominator), literal(rowCondition), literal(title), literal(unit), places,
                literal(factAlias(numerator)));
    }

    /** The alias the numerator starts from (e.g. smf in smf.failed_deployments); the demo fact's if it names none. */
    static String factAlias(String numerator) {
        log.debug("Started");
        var matcher = numerator == null ? null : ALIAS.matcher(numerator);
        return matcher != null && matcher.find() ? matcher.group(1) : DEFAULT_FACT_ALIAS;
    }

    /** A SQL string literal ('' doubled), or NULL. */
    static String literal(String value) {
        return value == null ? "NULL" : "'" + value.replace("'", "''") + "'";
    }

    private static String typeNames() {
        return Arrays.stream(MetricType.values()).map(MetricType::dbValue).collect(Collectors.joining(", "));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
