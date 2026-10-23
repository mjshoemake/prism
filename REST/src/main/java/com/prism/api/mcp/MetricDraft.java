package com.prism.api.mcp;

import java.util.List;

/**
 * Result of {@code draft_metric}: whether the definition is valid for Prism and, if so, the {@code Metrics} row to
 * add and the SQL Prism would run for it.
 *
 * @param valid       true when there are no errors (warnings don't block)
 * @param errors      what must change before the metric can be used
 * @param warnings    likely mistakes (e.g. a stored percentage used as a numerator)
 * @param calculation in words: what Prism calculates for this type
 * @param insertSql   a re-runnable INSERT for "prism"."Metrics" (null when invalid)
 * @param previewSql  the query Prism would build, grouped by Year &amp; Month and filtered by one team, against the
 *                    demo schema (null when invalid); with a client's own fact table only the FROM/JOIN differs
 */
public record MetricDraft(boolean valid, List<String> errors, List<String> warnings, String calculation,
                          String insertSql, String previewSql) {
}
