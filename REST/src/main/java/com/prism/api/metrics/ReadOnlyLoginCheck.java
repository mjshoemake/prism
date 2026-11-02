package com.prism.api.metrics;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.prism.common.logging.LogCategory;

/**
 * The check every metric-data login must pass before its first query (fail closed): it connects, its transactions
 * are read-only, it isn't a superuser, and it can neither use Prism's schema nor read any table in it. Shared by the
 * Prism database's own metric login ({@link MetricQueryConnection}) and client-database connections
 * ({@link ClientDatabaseConnections}). Works by catalog IDs, never by name, so it neither fails for a login that can't
 * see Prism's schema (the goal, SQL/023) nor when that schema isn't in the database at all (a client's database).
 */
public final class ReadOnlyLoginCheck {

    private static final Logger log = LogCategory.SERVICE.logger(ReadOnlyLoginCheck.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    /** What the login may and may not do, read from the database itself. */
    static final String CHECK_SQL = """
            SELECT current_user AS login,
                   current_setting('transaction_read_only') AS read_only,
                   current_setting('is_superuser') AS superuser,
                   EXISTS (SELECT 1 FROM pg_namespace n
                            WHERE n.nspname = 'prism' AND has_schema_privilege(n.oid, 'USAGE')) AS uses_prism_schema,
                   EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                            WHERE n.nspname = 'prism' AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
                              AND has_table_privilege(c.oid, 'SELECT')) AS reads_prism_tables
            """;

    private ReadOnlyLoginCheck() {
    }

    /**
     * Runs the check on {@code jdbc}'s login.
     *
     * @param what      how the connection is named in messages, e.g. "the metric data" or "'Acme warehouse'"
     * @param hint      what to do when the check fails (appended to the message)
     * @param auditName tag of the audit line written when the login is refused, e.g. {@code METRIC_QUERY_LOGIN}
     * @return the login's name
     * @throws MetricDataUnavailableException it can't connect, or the login may do more than read
     */
    public static String verify(JdbcTemplate jdbc, String what, String hint, String auditName) {
        log.debug("Started");
        Map<String, Object> login;
        try {
            login = jdbc.queryForMap(CHECK_SQL);
        } catch (DataAccessException e) {
            log.warn("Can't connect to {}: {}", what, firstLine(e));
            throw new MetricDataUnavailableException("Can't connect to " + what + " (" + firstLine(e) + "). " + hint,
                    e);
        }
        var name = String.valueOf(login.get("login"));
        var readOnly = "on".equals(String.valueOf(login.get("read_only")));
        var superuser = "on".equals(String.valueOf(login.get("superuser")));
        var usesPrismSchema = Boolean.TRUE.equals(login.get("uses_prism_schema"));
        var readsPrismTables = Boolean.TRUE.equals(login.get("reads_prism_tables"));
        if (!readOnly || superuser || usesPrismSchema || readsPrismTables) {
            audit.warn("{}_REFUSED login={} readOnly={} superuser={} usesPrismSchema={} readsPrismTables={}",
                    auditName, name, readOnly, superuser, usesPrismSchema, readsPrismTables);
            var problem = superuser ? "is a superuser"
                    : readsPrismTables ? "can read Prism's own tables"
                    : usesPrismSchema ? "can use Prism's schema" : "isn't read-only";
            var message = "The login '" + name + "' for " + what + " " + problem + ", so metric queries on it are "
                    + "refused. " + hint;
            log.error(message);
            throw new MetricDataUnavailableException(message);
        }
        return name;
    }

    /** The first line of the deepest cause's message (driver messages can run to several lines). */
    public static String firstLine(Throwable e) {
        log.debug("Started");
        var root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        var message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        return message.lines().findFirst().orElse(message).strip();
    }
}
