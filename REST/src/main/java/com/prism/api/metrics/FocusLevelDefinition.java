package com.prism.api.metrics;

/**
 * One row of "prism"."Focus_Levels", as cached by {@link MetricsMetadataCache}.
 *
 * @param columnName the column charts group and filter by; validated as a plain identifier when loaded
 */
public record FocusLevelDefinition(int focusLevelPk, int focusAreaPk, String name, String columnName) {
}
