package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Plain JUnit: the allow-list of development-connection settings (SQL/024). */
class ClientDatabaseConnectionsTests {

    @Test
    void allowListIsFixedAndOnlyThePasswordIsSecret() {
        assertEquals(List.of("username", "password", "sslmode", "connectTimeout", "socketTimeout", "ApplicationName"),
                List.copyOf(ClientDatabaseConnections.PROPERTIES.keySet()));
        ClientDatabaseConnections.PROPERTIES.values().forEach(spec ->
                assertEquals("password".equals(spec.name()), spec.secret(), spec.name()));
        // The login is Prism's to use, never passed to the driver as a raw property.
        assertTrue(!ClientDatabaseConnections.PROPERTIES.get("username").driver()
                && !ClientDatabaseConnections.PROPERTIES.get("password").driver());
    }

    @Test
    void valuesAreChecked() {
        assertNull(ClientDatabaseConnections.validate("username", "prism_reader"));
        assertNull(ClientDatabaseConnections.validate("password", " any thing! "));
        assertNull(ClientDatabaseConnections.validate("sslmode", "verify-full"));
        assertNull(ClientDatabaseConnections.validate("connectTimeout", "10"));
        assertNull(ClientDatabaseConnections.validate("ApplicationName", "Prism dev"));
        assertTrue(ClientDatabaseConnections.validate("sslfactory", "x").contains("Unknown"));
        assertTrue(ClientDatabaseConnections.validate("username", " ").contains("empty"));
        assertTrue(ClientDatabaseConnections.validate("connectTimeout", "301").contains("1 to 300"));
        assertTrue(ClientDatabaseConnections.validate("ApplicationName", "é").contains("plain"));
    }

    @Test
    void readOnlyCheckRefusesSuperusers() {
        assertTrue(ReadOnlyLoginCheck.CHECK_SQL.contains("is_superuser"));
    }
}
