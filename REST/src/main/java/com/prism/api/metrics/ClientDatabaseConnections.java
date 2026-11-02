package com.prism.api.metrics;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Connections to databases outside Prism's own, one small pool per data source. So far only for the TEMPORARY,
 * development-only connection method {@code direct_dev} (owner, 2026-11-01; SQL/024): the address comes from the data
 * source ({@code db_host}, {@code db_port}, {@code db_name}) and the login from its properties
 * ("prism"."Data_Source_Properties": {@code username}, {@code password}), because the secrets store doesn't exist yet.
 * Refused unless {@code prism.data-sources.allow-dev-connections=true}. Remove with the method when the secrets store
 * exists (ROADMAP); the firewall-allow-list method will then build its pools here with the secret's login.
 * <p>
 * Fail closed, like the Prism database's metric login: before a pool is used its login must pass
 * {@link ReadOnlyLoginCheck} (read-only, not a superuser, no access to Prism's schema), and it may not be Prism's own
 * login. Only allow-listed properties ({@link #PROPERTIES}) reach the driver: others could load code or turn
 * read-only off. A pool is rebuilt when its data source or properties change (checked before each use) and closed
 * when the data source is updated or deleted ({@link #evict}). Nothing connects until the first query or test.
 */
@Component
public class ClientDatabaseConnections implements DisposableBean {

    private static final Logger log = LogCategory.SERVICE.logger(ClientDatabaseConnections.class);

    /** TEMPORARY development-only connection method (SQL/024). */
    public static final String DIRECT_DEV = "direct_dev";

    /** Settings a data source's properties may hold, by name, in the order the UI shows them. */
    public static final Map<String, PropertySpec> PROPERTIES = specs();

    /** Host name or IP address: nothing that could change the JDBC URL (no '/', '?', '@', spaces). */
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9.:\\-]+");
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    private static final String SETTINGS_SQL = """
            SELECT ds.name, ds.db_connection_method, ds.db_host, ds.db_port, ds.db_name,
                   ds.updated_at::text || '|' || coalesce((SELECT max(p.updated_at)::text || '|' || count(*)
                                                            FROM "prism"."Data_Source_Properties" p
                                                           WHERE p.data_source_pk = ds.data_source_pk), '')
                       AS fingerprint
              FROM "prism"."Data_Sources" ds
             WHERE ds.data_source_pk = ?
            """;

    private static final String PROPERTIES_SQL = """
            SELECT name, value FROM "prism"."Data_Source_Properties" WHERE data_source_pk = ?
            """;

    /**
     * One allowed property. {@code driver}: passed to the database driver under this name (else only Prism reads it).
     */
    public record PropertySpec(String name, String label, boolean secret, boolean required, boolean driver,
                               String hint) {
    }

    /** A checked connection: the login it uses and where it goes (for messages). */
    public record Connection(JdbcTemplate jdbc, String login, String address) {
    }

    private record Pool(String fingerprint, HikariDataSource dataSource, Connection connection) {
    }

    private final JdbcTemplate prism;
    private final boolean allowDevConnections;
    private final String prismUsername;
    private final Duration timeout;
    private final int poolSize;
    private final Map<Integer, Pool> pools = new ConcurrentHashMap<>();

    public ClientDatabaseConnections(JdbcTemplate prism,
                                     @Value("${prism.data-sources.allow-dev-connections:false}")
                                     boolean allowDevConnections,
                                     @Value("${spring.datasource.username}") String prismUsername,
                                     @Value("${prism.metric-query.timeout:30s}") Duration timeout,
                                     @Value("${prism.data-sources.pool-size:5}") int poolSize) {
        log.debug("Started");
        this.prism = prism;
        this.allowDevConnections = allowDevConnections;
        this.prismUsername = prismUsername == null ? "" : prismUsername.strip();
        this.timeout = timeout;
        this.poolSize = poolSize;
    }

    /** Whether the development-only method may be saved and connected ({@code prism.data-sources.allow-dev-connections}). */
    public boolean devConnectionsAllowed() {
        log.debug("Started");
        return allowDevConnections;
    }

    /** The per-query time limit (the metric queries' own, {@code prism.metric-query.timeout}). */
    public Duration timeout() {
        log.debug("Started");
        return timeout;
    }

    /**
     * A checked, read-only connection to the data source's database, with the query time limit set.
     *
     * @throws MetricDataUnavailableException it can't be used (turned off, settings missing, can't connect, or the
     *                                        login may do more than read); retried on the next call
     */
    public Connection connection(int dataSourcePk) {
        log.debug("Started");
        var settings = prism.queryForList(SETTINGS_SQL, dataSourcePk);
        if (settings.isEmpty()) {
            throw new MetricDataUnavailableException("Data source " + dataSourcePk + " doesn't exist.");
        }
        var row = settings.get(0);
        var fingerprint = (String) row.get("fingerprint");
        var current = pools.get(dataSourcePk);
        if (current != null && current.fingerprint().equals(fingerprint)) {
            return current.connection();
        }
        synchronized (this) {
            current = pools.get(dataSourcePk);
            if (current != null && current.fingerprint().equals(fingerprint)) {
                return current.connection();
            }
            evict(dataSourcePk);
            var pool = open(dataSourcePk, row, fingerprint);
            pools.put(dataSourcePk, pool);
            return pool.connection();
        }
    }

    /** Closes the data source's pool (its settings changed, or it was deleted); the next use opens a new one. */
    public void evict(int dataSourcePk) {
        log.debug("Started");
        var old = pools.remove(dataSourcePk);
        if (old != null) {
            old.dataSource().close();
            log.info("Closed the connection pool of data source {}", dataSourcePk);
        }
    }

    private Pool open(int dataSourcePk, Map<String, Object> row, String fingerprint) {
        log.debug("Started");
        var name = "'" + row.get("name") + "'";
        var method = (String) row.get("db_connection_method");
        if (!DIRECT_DEV.equals(method)) {
            throw new MetricDataUnavailableException("Prism can't connect to " + name + "'s kind of database yet "
                    + "(roadmap: client data sources).");
        }
        if (!allowDevConnections) {
            throw new MetricDataUnavailableException(name + " uses a development-only connection, which is turned off "
                    + "on this server (prism.data-sources.allow-dev-connections).");
        }
        var host = (String) row.get("db_host");
        var dbName = (String) row.get("db_name");
        if (host == null || !HOST.matcher(host).matches() || dbName == null) {
            throw new MetricDataUnavailableException(name + " has no valid host or database name.");
        }
        var port = row.get("db_port") == null ? 5432 : ((Number) row.get("db_port")).intValue();
        var properties = new LinkedHashMap<String, String>();
        for (var p : prism.queryForList(PROPERTIES_SQL, dataSourcePk)) {
            properties.put((String) p.get("name"), (String) p.get("value"));
        }
        var username = properties.getOrDefault("username", "").strip();
        var password = properties.getOrDefault("password", "");
        if (username.isEmpty() || password.isEmpty()) {
            throw new MetricDataUnavailableException(name + " needs a username and password.");
        }
        if (username.equalsIgnoreCase(prismUsername)) {
            throw new MetricDataUnavailableException("Metric data can't be read with Prism's own login ('" + username
                    + "'). Use a read-only login, e.g. prism_reader (SQL/022).");
        }
        var address = host + ":" + port + "/" + dbName;
        var config = new HikariConfig();
        config.setPoolName("client-db-" + dataSourcePk);
        config.setJdbcUrl("jdbc:postgresql://" + (host.contains(":") ? "[" + host + "]" : host) + ":" + port + "/"
                + URLEncoder.encode(dbName, StandardCharsets.UTF_8).replace("+", "%20"));
        config.setUsername(username);
        config.setPassword(password);
        config.setReadOnly(true);
        config.setAutoCommit(true);
        config.setMaximumPoolSize(Math.max(1, poolSize));
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10_000);
        config.setInitializationFailTimeout(-1);
        // Allow-listed driver settings only, with safe defaults.
        config.addDataSourceProperty("sslmode", LOCAL_HOSTS.contains(host.toLowerCase(Locale.ROOT)) ? "prefer"
                : "require");
        config.addDataSourceProperty("connectTimeout", "10");
        config.addDataSourceProperty("ApplicationName", "Prism");
        properties.forEach((key, value) -> {
            var spec = PROPERTIES.get(key);
            if (spec != null && spec.driver() && validate(key, value) == null) {
                config.addDataSourceProperty(key, value.strip());
            }
        });
        var dataSource = new HikariDataSource(config);
        try {
            var jdbc = new JdbcTemplate(dataSource);
            jdbc.setQueryTimeout((int) Math.max(1, timeout.toSeconds()));
            var login = ReadOnlyLoginCheck.verify(jdbc, name + " (" + address + ") as '" + username + "'",
                    "Check the data source's address and login; it must be a read-only login such as prism_reader.",
                    "CLIENT_DB_LOGIN");
            log.info("Data source {} connects to {} as read-only '{}' (time limit {}s)", dataSourcePk, address, login,
                    jdbc.getQueryTimeout());
            return new Pool(fingerprint, dataSource, new Connection(jdbc, login, address));
        } catch (RuntimeException e) {
            dataSource.close();
            throw e;
        }
    }

    /**
     * Checks one property value.
     *
     * @return null if it is allowed, else a user-facing message
     */
    public static String validate(String name, String value) {
        log.debug("Started");
        var spec = PROPERTIES.get(name);
        if (spec == null) {
            return "Unknown setting '" + name + "' (allowed: " + String.join(", ", PROPERTIES.keySet()) + ").";
        }
        var v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            return spec.label() + " is empty.";
        }
        return switch (name) {
            case "username" -> v.length() <= 63 && v.matches("[A-Za-z0-9_.@\\-]+") ? null
                    : "The username may use letters, digits and _ . @ - only (at most 63).";
            case "password" -> value.length() <= 200 && value.chars().noneMatch(c -> c == '\n' || c == '\r') ? null
                    : "The password must be at most 200 characters on one line.";
            case "sslmode" -> List.of("disable", "allow", "prefer", "require", "verify-ca", "verify-full").contains(v)
                    ? null : "SSL mode must be disable, allow, prefer, require, verify-ca or verify-full.";
            case "connectTimeout" -> seconds(v, 1, 300) ? null : "Connect timeout must be 1 to 300 seconds.";
            case "socketTimeout" -> seconds(v, 1, 3600) ? null : "Socket timeout must be 1 to 3600 seconds.";
            case "ApplicationName" -> v.length() <= 63 && v.matches("[ -~]+") ? null
                    : "The application name must be at most 63 plain characters.";
            default -> null;
        };
    }

    private static boolean seconds(String value, int min, int max) {
        log.debug("Started");
        try {
            var n = Integer.parseInt(value);
            return n >= min && n <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Map<String, PropertySpec> specs() {
        log.debug("Started");
        var specs = new LinkedHashMap<String, PropertySpec>();
        for (var spec : List.of(
                new PropertySpec("username", "Username", false, true, false, "Read-only database login"),
                new PropertySpec("password", "Password", true, true, false, "Never shown again once saved"),
                new PropertySpec("sslmode", "SSL mode", false, false, true,
                        "Default: prefer on this computer, require elsewhere"),
                new PropertySpec("connectTimeout", "Connect timeout (s)", false, false, true, "Default 10"),
                new PropertySpec("socketTimeout", "Socket timeout (s)", false, false, true, "Default: none"),
                new PropertySpec("ApplicationName", "Application name", false, false, true, "Default: Prism"))) {
            specs.put(spec.name(), spec);
        }
        return Collections.unmodifiableMap(specs);
    }

    @Override
    public void destroy() {
        log.debug("Started");
        for (var pk : List.copyOf(pools.keySet())) {
            evict(pk);
        }
    }
}
