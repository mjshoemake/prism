package com.prism.api.datasources;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A data source as the API returns it ("prism"."Data_Sources", SQL/013): where a STAR schema lives and how Prism
 * connects, plus how much metadata hangs off it. Exactly one connection is filled: a database ({@code db_*}) or a
 * client-run API ({@code api_*}). Credentials are never returned: {@code *_credentials_ref} only names the secret in
 * the deployment's secrets store. The TEMPORARY development method {@code direct_dev} (SQL/024) keeps its login in
 * {@code properties} instead; a secret property (the password) comes back without its value.
 */
public record DataSource(
        @JsonProperty("data_source_pk") int dataSourcePk,
        @JsonProperty("enterprise_pk") long enterprisePk,
        String name,
        String description,
        @JsonProperty("connection_type") String connectionType,
        @JsonProperty("db_connection_method") String dbConnectionMethod,
        @JsonProperty("db_host") String dbHost,
        @JsonProperty("db_port") Integer dbPort,
        @JsonProperty("db_name") String dbName,
        @JsonProperty("db_credentials_ref") String dbCredentialsRef,
        @JsonProperty("api_url") String apiUrl,
        @JsonProperty("api_credentials_ref") String apiCredentialsRef,
        @JsonProperty("fact_count") int factCount,
        @JsonProperty("dimension_count") int dimensionCount,
        @JsonProperty("metric_count") int metricCount,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("updated_at") OffsetDateTime updatedAt,
        List<Property> properties) {

    /**
     * One saved setting ("prism"."Data_Source_Properties"). {@code value} is null for a secret, whose value is never
     * returned; {@code hasValue} says whether one is saved.
     */
    public record Property(String name, String value, boolean secret, @JsonProperty("has_value") boolean hasValue) {
    }

    /** {@code connection_type}: a database Prism queries with SQL it builds. */
    public static final String DATABASE = "database";
    /** {@code connection_type}: a client-run service implementing Prism's metric-data contract (second phase). */
    public static final String API = "api";
}
