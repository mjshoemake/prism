package com.prism.api.datasources;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Body of POST /api/data-sources and PUT /api/data-sources/{pk}: the whole data source. Fields of the other
 * connection type (or of a method that doesn't use them) are ignored, so a form may keep them while the user switches.
 *
 * @param properties the data source's settings ("prism"."Data_Source_Properties", SQL/024), replacing the saved ones;
 *                   only for the TEMPORARY development method {@code direct_dev} so far (ignored and cleared for the
 *                   others). A secret (the password) given without a value keeps the saved one.
 */
public record DataSourceRequest(
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
        List<Property> properties) {

    /** One setting: {@code value} null or blank for a secret keeps the saved value. */
    public record Property(String name, String value) {
    }

    /** A request without properties. */
    public DataSourceRequest(String name, String description, String connectionType, String dbConnectionMethod,
                             String dbHost, Integer dbPort, String dbName, String dbCredentialsRef, String apiUrl,
                             String apiCredentialsRef) {
        this(name, description, connectionType, dbConnectionMethod, dbHost, dbPort, dbName, dbCredentialsRef, apiUrl,
                apiCredentialsRef, null);
    }
}
