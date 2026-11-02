package com.prism.api.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;

/** Plain JUnit: the metric query login's guards that need no database (SQL/022, roadmap #3). */
class MetricQueryConnectionTests {

    private static MetricQueryConnection connection(String username, String prismUsername) {
        return new MetricQueryConnection("jdbc:postgresql://localhost:5432/playpen", username, "", prismUsername,
                Duration.ofSeconds(30), 2);
    }

    @Test
    void prismsOwnLoginIsRefused() {
        var error = assertThrows(MetricDataUnavailableException.class, () -> connection("postgres", "postgres").jdbc());
        assertTrue(error.getMessage().contains("own read-only login"), error.getMessage());
        // Case doesn't matter: Postgres folds unquoted role names.
        assertThrows(MetricDataUnavailableException.class, () -> connection("Postgres", "postgres").jdbc());
    }

    @Test
    void aMissingLoginIsRefused() {
        var error = assertThrows(MetricDataUnavailableException.class, () -> connection("  ", "postgres").jdbc());
        assertTrue(error.getMessage().contains("SQL/022"), error.getMessage());
    }

    @Test
    void timeoutComesFromTheSettings() {
        assertEquals(Duration.ofSeconds(30), connection("prism_reader", "postgres").timeout());
    }

    @Test
    void driverMessagesAreCutToTheirFirstLine() {
        var e = new DataAccessResourceFailureException("wrapped",
                new SQLException("FATAL: password authentication failed for user \"prism_reader\"\n  Detail: …"));
        assertEquals("FATAL: password authentication failed for user \"prism_reader\"",
                MetricQueryConnection.firstLine(e));
    }

    @Test
    void timeoutsAreRecognisedBySqlState() {
        var cancelled = new UncategorizedSQLException("query", "SELECT 1",
                new SQLException("canceling statement due to statement timeout", "57014"));
        assertEquals(SqlMetricQueries.QUERY_CANCELED, SqlMetricQueries.sqlState(cancelled));
        assertNull(SqlMetricQueries.sqlState(new DataAccessResourceFailureException("no SQLException")));
    }
}
