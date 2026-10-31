package com.prism.api.metrics;

/**
 * One row of "prism"."Focus_Levels", as cached by {@link MetricsMetadataCache}.
 *
 * @param dimensionPk   the dimension of the level's focus area (SQL/015); both columns belong to it
 * @param columnName    the LABEL column: the text a bar shows (need not be unique); a plain identifier
 * @param keyColumnName the KEY column: what charts group, sort and filter by, unique per bar, one label per key
 *                      (SQL/015, used since step 1e); null = the label column is the key
 */
public record FocusLevelDefinition(int focusLevelPk, int focusAreaPk, int dimensionPk, String name, String columnName,
                                   String keyColumnName) {

    /** A level whose label is its key (no separate key column). */
    public FocusLevelDefinition(int focusLevelPk, int focusAreaPk, int dimensionPk, String name, String columnName) {
        this(focusLevelPk, focusAreaPk, dimensionPk, name, columnName, null);
    }

    /** The column charts group, sort and filter by. */
    public String keyColumn() {
        return keyColumnName != null ? keyColumnName : columnName;
    }
}
