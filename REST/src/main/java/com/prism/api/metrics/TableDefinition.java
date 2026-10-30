package com.prism.api.metrics;

import java.util.regex.Pattern;

/**
 * A table of a data source as metric SQL refers to it: schema, table name and alias (SQL/013). Schema and table names
 * are written quoted, so CamelCase names work; the alias is written unquoted, so it must be a lower-case identifier.
 */
public record TableDefinition(String schemaName, String tableName, String alias) {

    private static final Pattern ALIAS = Pattern.compile("[a-z_][a-z0-9_]*");

    /** Validates the names (the database's CHECK constraints say the same; checked again before any SQL is built). */
    public TableDefinition {
        if (!quotable(schemaName) || !quotable(tableName)) {
            throw new IllegalArgumentException("Invalid schema or table name: " + schemaName + "." + tableName);
        }
        if (alias == null || !ALIAS.matcher(alias).matches()) {
            throw new IllegalArgumentException("Invalid table alias: " + alias);
        }
    }

    /** {@code "schema"."table" alias}, as written in a FROM or JOIN. */
    public String sqlReference() {
        return quoted(schemaName) + "." + quoted(tableName) + " " + alias;
    }

    private static boolean quotable(String name) {
        return name != null && !name.isEmpty() && name.length() <= 63
                && name.chars().noneMatch(c -> c == '"' || Character.isISOControl(c));
    }

    private static String quoted(String name) {
        return "\"" + name + "\"";
    }
}
