package com.prism.api.metrics;

/**
 * One row of "prism"."Focus_Levels", as cached by {@link MetricsMetadataCache}.
 *
 * @param dimensionPk the dimension of the level's focus area (SQL/015); the column belongs to it
 * @param columnName  the label column charts group and filter by; validated as a plain identifier when loaded
 */
public record FocusLevelDefinition(int focusLevelPk, int focusAreaPk, int dimensionPk, String name, String columnName) {
}
