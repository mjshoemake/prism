package com.prism.api.datasources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Plain JUnit: the rules a data source must follow before it is saved (mirroring SQL/013's CHECKs). */
class DataSourcesServiceTests {

    private static DataSourceRequest db(String method, String host, Integer port, String dbName, String secret) {
        return new DataSourceRequest(" Warehouse ", "  ", "database", method, host, port, dbName, secret, null, null);
    }

    private static DataSourceRequest api(String url, String secret) {
        return new DataSourceRequest("Acme API", null, "api", null, null, null, null, null, url, secret);
    }

    private static String refused(DataSourceRequest request) {
        return assertThrows(IllegalArgumentException.class, () -> DataSourcesService.validate(request)).getMessage();
    }

    @Test
    void prismDatabaseNeedsNoAddressAndDropsAnyGiven() {
        // A form may still hold host fields after the user switched to the Prism database.
        var valid = DataSourcesService.validate(db("prism_database", "db.acme.com", 5432, "dw", "prism/x"));
        assertEquals("Warehouse", valid.name());
        assertNull(valid.description());
        assertEquals("prism_database", valid.method());
        assertNull(valid.host());
        assertNull(valid.port());
        assertNull(valid.dbName());
        assertNull(valid.dbSecret());
        assertNull(valid.apiUrl());
    }

    @Test
    void externalDatabaseNeedsHostNameAndSecretName() {
        var valid = DataSourcesService.validate(db("private_link", " db.acme.internal ", null, "warehouse",
                "prism/acme/warehouse-reader"));
        assertEquals("db.acme.internal", valid.host());
        assertNull(valid.port());
        assertEquals("prism/acme/warehouse-reader", valid.dbSecret());
        assertTrue(refused(db("firewall_allow_list", null, null, "dw", "s")).contains("host is required"));
        assertTrue(refused(db("firewall_allow_list", "h", null, " ", "s")).contains("database name is required"));
        assertTrue(refused(db("firewall_allow_list", "h", null, "dw", null)).contains("secret name is required"));
    }

    @Test
    void hostPortAndSecretAreChecked() {
        assertTrue(refused(db("outbound_agent", "https://db.acme.com", null, "dw", "s")).contains("host name"));
        assertTrue(refused(db("outbound_agent", "db acme", null, "dw", "s")).contains("host name"));
        assertTrue(refused(db("outbound_agent", "h", 0, "dw", "s")).contains("1 and 65535"));
        assertTrue(refused(db("outbound_agent", "h", 70000, "dw", "s")).contains("1 and 65535"));
        // A password typed into the secret-name field is refused (spaces / symbols).
        assertTrue(refused(db("outbound_agent", "h", null, "dw", "pa ss!word")).contains("letters, digits"));
        DataSourcesService.validate(db("outbound_agent", "10.0.0.5", 5432, "dw", "s"));
    }

    @Test
    void methodMustBeKnown() {
        assertTrue(refused(db(null, null, null, null, null)).contains("how Prism connects"));
        assertTrue(refused(db("odbc", "h", null, "dw", "s")).contains("how Prism connects"));
    }

    @Test
    void apiNeedsHttpsUrlAndSecretNameAndDropsDatabaseFields() {
        var valid = DataSourcesService.validate(new DataSourceRequest("Acme API", null, "api", "private_link", "h",
                5432, "dw", "x", "https://metrics.acme.com/prism", "prism/acme/api"));
        assertNull(valid.method());
        assertNull(valid.host());
        assertEquals("https://metrics.acme.com/prism", valid.apiUrl());
        assertTrue(refused(api("http://metrics.acme.com", "s")).contains("https://"));
        assertTrue(refused(api("https://", "s")).contains("https://"));
        assertTrue(refused(api(null, "s")).contains("URL is required"));
        assertTrue(refused(api("https://metrics.acme.com", null)).contains("secret name is required"));
    }

    @Test
    void nameTypeAndRequestAreRequired() {
        assertTrue(refused(new DataSourceRequest("  ", null, "database", "prism_database", null, null, null, null,
                null, null)).contains("name is required"));
        assertTrue(refused(new DataSourceRequest("x".repeat(61), null, "database", "prism_database", null, null, null,
                null, null, null)).contains("60"));
        assertTrue(refused(new DataSourceRequest("n", null, null, "prism_database", null, null, null, null, null,
                null)).contains("database or api"));
        assertTrue(refused(null).contains("required"));
    }

    @Test
    void methodLabelsCoverTheDatabaseCheck() {
        assertEquals(java.util.List.of("prism_database", "firewall_allow_list", "private_link", "outbound_agent",
                "direct_dev"), java.util.List.copyOf(DataSourcesService.METHODS.keySet()));
    }

    private static DataSourceRequest dev(DataSourceRequest.Property... properties) {
        return new DataSourceRequest("Dev", null, "database", "direct_dev", "localhost", 5432, "playpen",
                "ignored/secret", null, null, java.util.List.of(properties));
    }

    private static DataSourceRequest.Property p(String name, String value) {
        return new DataSourceRequest.Property(name, value);
    }

    @Test
    void devConnectionNeedsUsernameAndKeepsLoginInProperties() {
        var valid = DataSourcesService.validate(dev(p("password", "s3cret pass "), p(" username ", " prism_reader "),
                p("sslmode", "disable"), p("connectTimeout", " ")));
        assertEquals("localhost", valid.host());
        assertNull(valid.dbSecret()); // no secret name: the login is in the properties
        // Ordered like the allow-list; secrets kept as typed; blank optional settings dropped.
        assertEquals(java.util.List.of("username", "password", "sslmode"),
                java.util.List.copyOf(valid.properties().keySet()));
        assertEquals("prism_reader", valid.properties().get("username"));
        assertEquals("s3cret pass ", valid.properties().get("password"));
        assertTrue(refused(dev(p("password", "x"))).contains("username is required"));
        assertTrue(refused(new DataSourceRequest("Dev", null, "database", "direct_dev", null, null, "db", null, null,
                null, null)).contains("host is required"));
    }

    @Test
    void devPasswordWithoutValueMeansKeepTheSavedOne() {
        var valid = DataSourcesService.validate(dev(p("username", "prism_reader"), p("password", null)));
        assertTrue(valid.properties().containsKey("password"));
        assertNull(valid.properties().get("password"));
        valid = DataSourcesService.validate(dev(p("username", "prism_reader"), p("password", "")));
        assertNull(valid.properties().get("password"));
    }

    @Test
    void onlyAllowListedSettingsWithValidValues() {
        // Driver settings that could load code or undo read-only are never accepted.
        assertTrue(refused(dev(p("username", "u"), p("socketFactory", "x"))).contains("Unknown setting"));
        assertTrue(refused(dev(p("username", "u"), p("readOnly", "false"))).contains("Unknown setting"));
        assertTrue(refused(dev(p("username", "u"), p("username", "v"))).contains("twice"));
        assertTrue(refused(dev(p("username", "u; drop"))).contains("letters, digits"));
        assertTrue(refused(dev(p("username", "u"), p("sslmode", "maybe"))).contains("SSL mode"));
        assertTrue(refused(dev(p("username", "u"), p("connectTimeout", "0"))).contains("1 to 300"));
        assertTrue(refused(dev(p("username", "u"), p("socketTimeout", "abc"))).contains("1 to 3600"));
        assertTrue(refused(dev(p("username", "u"), p("password", "a\nb"))).contains("one line"));
        assertTrue(refused(dev(p(" ", "x"))).contains("needs a name"));
    }

    @Test
    void otherMethodsIgnoreAndClearProperties() {
        var valid = DataSourcesService.validate(new DataSourceRequest("W", null, "database", "firewall_allow_list",
                "db.acme.com", null, "dw", "prism/acme/dw", null, null,
                java.util.List.of(p("username", "u"), p("password", "p"))));
        assertTrue(valid.properties().isEmpty());
        assertEquals("prism/acme/dw", valid.dbSecret());
        assertTrue(DataSourcesService.validate(db("prism_database", null, null, null, null)).properties().isEmpty());
    }
}
