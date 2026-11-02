package com.prism.api.datasources;

import java.util.List;

/**
 * Result of POST /api/data-sources/{pk}/test: whether Prism can connect and read the data source's registered STAR
 * tables. {@code readable}: what the connection's login can read, per schema (system schemas left out); {@code tables}:
 * one line per registered fact or dimension table.
 */
public record ConnectionTest(boolean success, String message, List<SchemaTables> readable, List<TableCheck> tables) {

    /** Number of tables (and views) the login may SELECT in one schema. */
    public record SchemaTables(String schema, int tables) {
    }

    /** One registered fact or dimension table: {@code kind} is "fact" or "dimension"; {@code error} null when readable. */
    public record TableCheck(String kind, String name, String table, boolean readable, String error) {
    }
}
