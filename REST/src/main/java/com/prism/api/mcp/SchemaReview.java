package com.prism.api.mcp;

import java.util.List;

/**
 * Result of {@code review_star_schema} (docs/MCP.md §4.2): how the tables were read, what to fix, and a suggested
 * start for focus areas and levels.
 *
 * @param summary    one paragraph: tables found, Prism-ready or not, counts by severity
 * @param tables     each table with the role the review gave it and why
 * @param findings   errors first, then warnings, then notes
 * @param focusAreas suggested focus areas (one per dimension) with levels in drill order
 * @param skipped    statements that were not table DDL or could not be read (first words only, never the full text)
 */
public record SchemaReview(String summary, List<TableSummary> tables, List<Finding> findings,
                           List<FocusAreaSuggestion> focusAreas, List<String> skipped) {

    /** {@code role}: fact, dimension or unknown; {@code grain} only for facts. */
    public record TableSummary(String name, String role, String reason, String grain, int columns) {
    }

    /**
     * One finding. {@code rule} is a stable ID (S1 grain … S8 per-item grain; S0 = could not read), so findings can be
     * explained and looked up in prism://guide/star-schema.
     */
    public record Finding(String severity, String rule, String table, String column, String message, String suggestion) {
    }

    /** Levels in drill order (highest first); standalone levels are offered for slicing but not drilled into. */
    public record FocusAreaSuggestion(String focusArea, String dimension, List<String> drillLevels,
                                      List<String> standaloneLevels, String note) {
    }
}
