package com.prism.api.datasources;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prism.api.metrics.ClientDatabaseConnections;
import com.prism.api.metrics.MetricDataUnavailableException;
import com.prism.api.metrics.MetricQueryConnection;
import com.prism.api.metrics.MetricsMetadataCache;
import com.prism.common.logging.LogCategory;

/**
 * Metrics → Data Sources ("prism"."Data_Sources", SQL/013): where an enterprise's STAR schemas live and how Prism
 * connects. A data source is the parent of its facts and dimensions, and through them of metrics and focus areas.
 * Every operation is limited to one enterprise.
 * TODO (roadmap #4): the enterprise comes from the request until sessions exist; metrics admins only.
 */
@Service
public class DataSourcesService {

    private static final Logger log = LogCategory.SERVICE.logger(DataSourcesService.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    public static final int MAX_NAME = 60;
    public static final int MAX_DESCRIPTION = 2000;
    public static final int MAX_HOST = 255;
    public static final int MAX_DB_NAME = 63;
    public static final int MAX_SECRET_NAME = 200;
    public static final int MAX_URL = 500;

    /** Database connection methods (SQL/013 CHECK), with the label the UI shows. */
    public static final Map<String, String> METHODS = orderedMethods();
    public static final String PRISM_DATABASE = "prism_database";
    /** TEMPORARY development-only method (SQL/024): login in the properties. Remove with the secrets store. */
    public static final String DIRECT_DEV = ClientDatabaseConnections.DIRECT_DEV;

    /** Host name or IP address (no scheme, no port, no spaces). */
    private static final Pattern HOST = Pattern.compile("[A-Za-z0-9.:\\-]+");
    /** Name of a secret in the secrets store, e.g. {@code prism/acme/warehouse-reader}. Never the secret itself. */
    private static final Pattern SECRET_NAME = Pattern.compile("[A-Za-z0-9._/\\-]+");

    private static final String SELECT_SQL = """
            SELECT ds.data_source_pk, ds.enterprise_pk, ds.name, ds.description, ds.db_connection_method, ds.db_host,
                   ds.db_port, ds.db_name, ds.db_credentials_ref, ds.api_url, ds.api_credentials_ref, ds.created_at,
                   ds.updated_at,
                   (SELECT count(*) FROM "prism"."Facts" f WHERE f.data_source_pk = ds.data_source_pk) AS fact_count,
                   (SELECT count(*) FROM "prism"."Dimensions" d WHERE d.data_source_pk = ds.data_source_pk)
                       AS dimension_count,
                   (SELECT count(*) FROM "prism"."Metrics" m JOIN "prism"."Facts" f ON f.fact_pk = m.fact_pk
                     WHERE f.data_source_pk = ds.data_source_pk) AS metric_count
            FROM "prism"."Data_Sources" ds
            WHERE ds.enterprise_pk = ?
            """;

    private static final String LIST_SQL = SELECT_SQL + " ORDER BY lower(ds.name), ds.data_source_pk";

    private static final String GET_SQL = SELECT_SQL + " AND ds.data_source_pk = ?";

    private static final String NAME_TAKEN_SQL = """
            SELECT 1 FROM "prism"."Data_Sources"
            WHERE enterprise_pk = ? AND lower(name) = lower(?) AND data_source_pk <> ?
            """;

    private static final String ENTERPRISE_SQL = """
            SELECT 1 FROM "prism"."Enterprises" WHERE enterprise_pk = ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO "prism"."Data_Sources"
                (enterprise_pk, name, description, db_connection_method, db_host, db_port, db_name, db_credentials_ref,
                 api_url, api_credentials_ref)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING data_source_pk
            """;

    private static final String UPDATE_SQL = """
            UPDATE "prism"."Data_Sources"
               SET name = ?, description = ?, db_connection_method = ?, db_host = ?, db_port = ?, db_name = ?,
                   db_credentials_ref = ?, api_url = ?, api_credentials_ref = ?, updated_at = now()
             WHERE data_source_pk = ? AND enterprise_pk = ?
            """;

    private static final String DELETE_SQL = """
            DELETE FROM "prism"."Data_Sources" WHERE data_source_pk = ? AND enterprise_pk = ?
            """;

    /** Saved settings of the enterprise's data sources; a secret's value is never even read. */
    private static final String PROPERTIES_SQL = """
            SELECT p.data_source_pk, p.name, CASE WHEN p.is_secret THEN NULL ELSE p.value END AS value, p.is_secret
              FROM "prism"."Data_Source_Properties" p
              JOIN "prism"."Data_Sources" ds ON ds.data_source_pk = p.data_source_pk
             WHERE ds.enterprise_pk = ?
             ORDER BY p.data_source_pk, p.name
            """;

    private static final String PROPERTY_NAMES_SQL = """
            SELECT name FROM "prism"."Data_Source_Properties" WHERE data_source_pk = ?
            """;

    private static final String UPSERT_PROPERTY_SQL = """
            INSERT INTO "prism"."Data_Source_Properties" (data_source_pk, name, value, is_secret)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (data_source_pk, name) DO UPDATE SET value = EXCLUDED.value, is_secret = EXCLUDED.is_secret
             WHERE ("prism"."Data_Source_Properties".value, "prism"."Data_Source_Properties".is_secret)
                   IS DISTINCT FROM (EXCLUDED.value, EXCLUDED.is_secret)
            """;

    private static final String DELETE_PROPERTY_SQL = """
            DELETE FROM "prism"."Data_Source_Properties" WHERE data_source_pk = ? AND name = ?
            """;

    /**
     * Tables and views the connection's login may SELECT, counted per schema (system schemas left out). Run on the
     * data source's own connection, by catalog IDs, so it shows what that login can actually see.
     */
    static final String READABLE_SQL = """
            SELECT n.nspname AS schema_name, count(*) AS tables
              FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
             WHERE c.relkind IN ('r', 'p', 'v', 'm', 'f')
               AND n.nspname NOT IN ('pg_catalog', 'information_schema') AND n.nspname NOT LIKE 'pg\\_%'
               AND has_table_privilege(c.oid, 'SELECT')
             GROUP BY n.nspname
             ORDER BY n.nspname
            """;

    private static final String TABLES_SQL = """
            SELECT 'fact' AS kind, f.name, f.schema_name, f.table_name
              FROM "prism"."Facts" f WHERE f.data_source_pk = ?
            UNION ALL
            SELECT 'dimension', d.name, d.schema_name, d.table_name
              FROM "prism"."Dimensions" d WHERE d.data_source_pk = ?
            ORDER BY 1 DESC, 2
            """;

    /** A name already used by another of the enterprise's data sources (answered with 409). */
    public static class DuplicateNameException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DuplicateNameException(String name) {
            super("A data source named '" + name + "' already exists.");
        }
    }

    /** A data source that facts or dimensions still belong to (answered with 409). */
    public static class InUseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public InUseException(String message) {
            super(message);
        }
    }

    /**
     * The request after validation: trimmed, and with the fields of the other connection type cleared.
     * {@code properties} by name, in {@link ClientDatabaseConnections#PROPERTIES} order; a null value is a secret to
     * keep as saved. Empty for every method but {@code direct_dev}.
     */
    record Valid(String name, String description, String method, String host, Integer port, String dbName,
                 String dbSecret, String apiUrl, String apiSecret, Map<String, String> properties) {
    }

    /** What the data source pages need to know about this server: GET /api/data-sources/options. */
    public record Options(@JsonProperty("dev_connections_allowed")
                          boolean devConnectionsAllowed,
                          List<ClientDatabaseConnections.PropertySpec> properties) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final MetricQueryConnection metricQueries;
    private final ClientDatabaseConnections clientDatabases;
    private final MetricsMetadataCache metadata;

    public DataSourcesService(JdbcTemplate jdbcTemplate, MetricQueryConnection metricQueries,
                              ClientDatabaseConnections clientDatabases, MetricsMetadataCache metadata) {
        this.jdbcTemplate = jdbcTemplate;
        this.metricQueries = metricQueries;
        this.clientDatabases = clientDatabases;
        this.metadata = metadata;
    }

    /** Whether the development-only method is turned on, and the settings a data source may hold. */
    public Options options() {
        log.debug("Started");
        return new Options(clientDatabases.devConnectionsAllowed(),
                List.copyOf(ClientDatabaseConnections.PROPERTIES.values()));
    }

    /** The enterprise's data sources by name, with how many facts, dimensions and metrics each has. */
    public List<DataSource> list(long enterprisePk) {
        log.debug("Started");
        var properties = properties(enterprisePk);
        return jdbcTemplate.queryForList(LIST_SQL, enterprisePk).stream()
                .map(row -> toDataSource(row, properties)).toList();
    }

    /** One of the enterprise's data sources; empty if it doesn't exist or belongs to another enterprise. */
    public Optional<DataSource> get(long enterprisePk, int dataSourcePk) {
        log.debug("Started");
        var rows = jdbcTemplate.queryForList(GET_SQL, enterprisePk, dataSourcePk);
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDataSource(rows.get(0), properties(enterprisePk)));
    }

    /**
     * @throws IllegalArgumentException invalid request or unknown enterprise (400)
     * @throws DuplicateNameException   the enterprise already has a data source with that name (409)
     */
    @Transactional
    public DataSource create(long enterprisePk, DataSourceRequest request) {
        log.debug("Started");
        var valid = validate(request);
        if (jdbcTemplate.queryForList(ENTERPRISE_SQL, enterprisePk).isEmpty()) {
            throw new IllegalArgumentException("Unknown enterprise.");
        }
        requireDevAllowed(valid);
        requireSecrets(valid, Set.of());
        requireFreeName(enterprisePk, valid.name(), -1);
        var pk = jdbcTemplate.queryForObject(INSERT_SQL, Integer.class, enterprisePk, valid.name(), valid.description(),
                valid.method(), valid.host(), valid.port(), valid.dbName(), valid.dbSecret(), valid.apiUrl(),
                valid.apiSecret());
        if (pk == null) {
            throw new IllegalStateException("No data_source_pk returned.");
        }
        saveProperties(pk, valid);
        metadata.invalidate();
        audit.info("DATA_SOURCE_CREATED enterprise={} dataSource={} name=\"{}\" connection={} properties={}",
                enterprisePk, pk, valid.name(), connectionLabel(valid), valid.properties().keySet());
        return get(enterprisePk, pk).orElseThrow();
    }

    /**
     * Replaces the data source's settings.
     *
     * @return empty if the enterprise has no such data source (404)
     * @throws IllegalArgumentException invalid request (400)
     * @throws DuplicateNameException   another of the enterprise's data sources has that name (409)
     */
    @Transactional
    public Optional<DataSource> update(long enterprisePk, int dataSourcePk, DataSourceRequest request) {
        log.debug("Started");
        var valid = validate(request);
        if (get(enterprisePk, dataSourcePk).isEmpty()) {
            return Optional.empty();
        }
        requireDevAllowed(valid);
        requireSecrets(valid, Set.copyOf(jdbcTemplate.queryForList(PROPERTY_NAMES_SQL, String.class, dataSourcePk)));
        requireFreeName(enterprisePk, valid.name(), dataSourcePk);
        jdbcTemplate.update(UPDATE_SQL, valid.name(), valid.description(), valid.method(), valid.host(), valid.port(),
                valid.dbName(), valid.dbSecret(), valid.apiUrl(), valid.apiSecret(), dataSourcePk, enterprisePk);
        saveProperties(dataSourcePk, valid);
        clientDatabases.evict(dataSourcePk);
        metadata.invalidate();
        audit.info("DATA_SOURCE_UPDATED enterprise={} dataSource={} name=\"{}\" connection={} properties={}",
                enterprisePk, dataSourcePk, valid.name(), connectionLabel(valid), valid.properties().keySet());
        return get(enterprisePk, dataSourcePk);
    }

    /**
     * Deletes a data source that nothing belongs to yet.
     *
     * @return false if the enterprise has no such data source (404)
     * @throws InUseException facts or dimensions still belong to it (409)
     */
    @Transactional
    public boolean delete(long enterprisePk, int dataSourcePk) {
        log.debug("Started");
        var existing = get(enterprisePk, dataSourcePk);
        if (existing.isEmpty()) {
            return false;
        }
        var ds = existing.get();
        if (ds.factCount() > 0 || ds.dimensionCount() > 0) {
            throw new InUseException("'" + ds.name() + "' still has " + plural(ds.factCount(), "fact") + " and "
                    + plural(ds.dimensionCount(), "dimension") + " (used by " + plural(ds.metricCount(), "metric")
                    + "). Remove those first.");
        }
        jdbcTemplate.update(DELETE_SQL, dataSourcePk, enterprisePk);
        clientDatabases.evict(dataSourcePk);
        metadata.invalidate();
        audit.info("DATA_SOURCE_DELETED enterprise={} dataSource={} name=\"{}\"", enterprisePk, dataSourcePk,
                ds.name());
        return true;
    }

    /**
     * Checks that Prism can read the data source's registered fact and dimension tables. Only the Prism database
     * (the demo STAR schema) and the temporary development method can be reached so far; other connections are saved
     * but not yet connected to.
     *
     * @return empty if the enterprise has no such data source (404)
     */
    public Optional<ConnectionTest> test(long enterprisePk, int dataSourcePk) {
        log.debug("Started");
        var found = get(enterprisePk, dataSourcePk);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var ds = found.get();
        var result = runTest(ds);
        audit.info("DATA_SOURCE_TESTED enterprise={} dataSource={} success={}", enterprisePk, dataSourcePk,
                result.success());
        return Optional.of(result);
    }

    private ConnectionTest runTest(DataSource ds) {
        log.debug("Started");
        if (DataSource.API.equals(ds.connectionType())) {
            return new ConnectionTest(false, "Prism can't call client APIs yet (a later roadmap step). The settings "
                    + "are saved.", List.of(), List.of());
        }
        var devConnection = DIRECT_DEV.equals(ds.dbConnectionMethod());
        if (!PRISM_DATABASE.equals(ds.dbConnectionMethod()) && !devConnection) {
            return new ConnectionTest(false, "Prism can't connect to '" + METHODS.get(ds.dbConnectionMethod())
                    + "' databases yet (roadmap: client data sources). The settings are saved.", List.of(), List.of());
        }
        JdbcTemplate reader;
        String connected;
        String grantHint;
        try {
            if (devConnection) {
                var client = clientDatabases.connection(ds.dataSourcePk());
                reader = client.jdbc();
                connected = "Connected to " + client.address() + " as the read-only login '" + client.login() + "'";
                grantHint = "Check that they exist and that '" + client.login() + "' may SELECT them.";
            } else {
                reader = metricQueries.jdbc();
                connected = "Connected with the read-only metric login";
                grantHint = "Check that they exist, then run SELECT prism.grant_star_tables_to_reader();";
            }
        } catch (MetricDataUnavailableException e) {
            return new ConnectionTest(false, e.getMessage(), List.of(), List.of());
        }
        var readable = new ArrayList<ConnectionTest.SchemaTables>();
        try {
            for (var row : reader.queryForList(READABLE_SQL)) {
                readable.add(new ConnectionTest.SchemaTables((String) row.get("schema_name"),
                        ((Number) row.get("tables")).intValue()));
            }
        } catch (DataAccessException e) {
            return new ConnectionTest(false, connected + ", but couldn't list the tables it can read ("
                    + firstLine(e) + ").", List.of(), List.of());
        }
        var found = readable.stream().mapToInt(ConnectionTest.SchemaTables::tables).sum();
        connected += found == 0 ? "; this login can't read any tables"
                : "; this login can read " + plural(found, "table") + " in " + plural(readable.size(), "schema");
        var checks = new ArrayList<ConnectionTest.TableCheck>();
        for (var row : jdbcTemplate.queryForList(TABLES_SQL, ds.dataSourcePk(), ds.dataSourcePk())) {
            var schema = (String) row.get("schema_name");
            var table = (String) row.get("table_name");
            var qualified = quote(schema) + "." + quote(table);
            String error = null;
            try {
                reader.queryForList("SELECT 1 FROM " + qualified + " LIMIT 0");
            } catch (DataAccessException e) {
                error = firstLine(e);
            }
            checks.add(new ConnectionTest.TableCheck((String) row.get("kind"), (String) row.get("name"),
                    schema + "." + table, error == null, error));
        }
        if (checks.isEmpty()) {
            return new ConnectionTest(found > 0, connected + (found > 0 ? ". None are registered as facts or "
                    + "dimensions yet." : ". Check that it has SELECT on the STAR tables."), List.copyOf(readable),
                    List.of());
        }
        var unreadable = checks.stream().filter(c -> !c.readable()).count();
        var message = unreadable == 0
                ? connected + "; all " + plural(checks.size(), "registered table") + " can be read."
                : connected + ", but " + unreadable + " of " + plural(checks.size(), "registered table")
                        + " can't be read. " + grantHint;
        return new ConnectionTest(unreadable == 0, message, List.copyOf(readable), List.copyOf(checks));
    }

    /**
     * The request trimmed and checked, with the fields that don't apply cleared.
     *
     * @throws IllegalArgumentException with a user-facing message
     */
    static Valid validate(DataSourceRequest request) {
        log.debug("Started");
        if (request == null) {
            throw new IllegalArgumentException("A data source is required.");
        }
        var name = trim(request.name());
        if (name == null) {
            throw new IllegalArgumentException("A name is required.");
        }
        if (name.length() > MAX_NAME) {
            throw new IllegalArgumentException("The name is longer than " + MAX_NAME + " characters.");
        }
        var description = trim(request.description());
        if (description != null && description.length() > MAX_DESCRIPTION) {
            throw new IllegalArgumentException("The description is longer than " + MAX_DESCRIPTION + " characters.");
        }
        var type = trim(request.connectionType());
        if (DataSource.API.equals(type)) {
            var url = trim(request.apiUrl());
            if (url == null) {
                throw new IllegalArgumentException("The API's URL is required.");
            }
            if (url.length() > MAX_URL || !url.startsWith("https://") || !hasHost(url)) {
                throw new IllegalArgumentException("The API's URL must be a full https:// address.");
            }
            return new Valid(name, description, null, null, null, null, null, url,
                    secretName(request.apiCredentialsRef()), Map.of());
        }
        if (!DataSource.DATABASE.equals(type)) {
            throw new IllegalArgumentException("The connection type must be database or api.");
        }
        var method = trim(request.dbConnectionMethod());
        if (method == null || !METHODS.containsKey(method)) {
            throw new IllegalArgumentException("Choose how Prism connects to the database.");
        }
        if (PRISM_DATABASE.equals(method)) {
            // The STAR schema lives in the Prism database: no address, no credentials (SQL/013 CHECK).
            return new Valid(name, description, method, null, null, null, null, null, null, Map.of());
        }
        var host = trim(request.dbHost());
        if (host == null) {
            throw new IllegalArgumentException("The database host is required.");
        }
        if (host.length() > MAX_HOST || !HOST.matcher(host).matches()) {
            throw new IllegalArgumentException("The host must be a host name or IP address (no https://, port or "
                    + "spaces).");
        }
        var port = request.dbPort();
        if (port != null && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("The port must be between 1 and 65535.");
        }
        var dbName = trim(request.dbName());
        if (dbName == null) {
            throw new IllegalArgumentException("The database name is required.");
        }
        if (dbName.length() > MAX_DB_NAME || dbName.contains("\"")) {
            throw new IllegalArgumentException("The database name must be at most " + MAX_DB_NAME
                    + " characters, without double quotes.");
        }
        if (DIRECT_DEV.equals(method)) {
            // TEMPORARY (SQL/024): the login is in the properties, not a secrets store.
            return new Valid(name, description, method, host, port, dbName, null, null, null,
                    validProperties(request.properties()));
        }
        return new Valid(name, description, method, host, port, dbName, secretName(request.dbCredentialsRef()),
                null, null, Map.of());
    }

    /**
     * The properties checked against {@link ClientDatabaseConnections#PROPERTIES}, in its order. A secret without a
     * value is kept as saved (null here); the username is required.
     */
    private static Map<String, String> validProperties(List<DataSourceRequest.Property> properties) {
        log.debug("Started");
        var given = new HashMap<String, String>();
        for (var property : properties == null ? List.<DataSourceRequest.Property>of() : properties) {
            if (property == null || trim(property.name()) == null) {
                throw new IllegalArgumentException("Every setting needs a name.");
            }
            var name = property.name().strip();
            var spec = ClientDatabaseConnections.PROPERTIES.get(name);
            if (given.containsKey(name)) {
                throw new IllegalArgumentException("The setting '" + name + "' is given twice.");
            }
            var keepSecret = spec != null && spec.secret()
                    && (property.value() == null || property.value().isEmpty());
            if (keepSecret) {
                given.put(name, null);
                continue;
            }
            if (spec != null && !spec.secret() && trim(property.value()) == null) {
                continue; // an optional setting left blank: not saved
            }
            var problem = ClientDatabaseConnections.validate(name, property.value());
            if (problem != null) {
                throw new IllegalArgumentException(problem);
            }
            given.put(name, spec.secret() ? property.value() : property.value().strip());
        }
        var ordered = new LinkedHashMap<String, String>();
        ClientDatabaseConnections.PROPERTIES.keySet().stream().filter(given::containsKey)
                .forEach(name -> ordered.put(name, given.get(name)));
        if (ordered.get("username") == null) {
            throw new IllegalArgumentException("A username is required (a read-only database login, e.g. "
                    + "prism_reader).");
        }
        return Collections.unmodifiableMap(ordered);
    }

    /** The development-only method is refused unless the server allows it. */
    private void requireDevAllowed(Valid valid) {
        log.debug("Started");
        if (DIRECT_DEV.equals(valid.method()) && !clientDatabases.devConnectionsAllowed()) {
            throw new IllegalArgumentException("Development-only connections are turned off on this server "
                    + "(prism.data-sources.allow-dev-connections).");
        }
    }

    /** Every required secret is either given or already saved ({@code saved}: the names saved now). */
    private static void requireSecrets(Valid valid, Set<String> saved) {
        log.debug("Started");
        if (!DIRECT_DEV.equals(valid.method())) {
            return;
        }
        for (var spec : ClientDatabaseConnections.PROPERTIES.values()) {
            if (spec.required() && spec.secret()) {
                var value = valid.properties().get(spec.name());
                if (value == null && !(valid.properties().containsKey(spec.name()) && saved.contains(spec.name()))) {
                    throw new IllegalArgumentException("A " + spec.label().toLowerCase(Locale.ROOT)
                            + " is required.");
                }
            }
        }
    }

    /** Replaces the saved properties with the valid ones (a secret without a value keeps the saved one). */
    private void saveProperties(int dataSourcePk, Valid valid) {
        log.debug("Started");
        for (var name : jdbcTemplate.queryForList(PROPERTY_NAMES_SQL, String.class, dataSourcePk)) {
            if (!valid.properties().containsKey(name)) {
                jdbcTemplate.update(DELETE_PROPERTY_SQL, dataSourcePk, name);
            }
        }
        valid.properties().forEach((name, value) -> {
            if (value != null) {
                jdbcTemplate.update(UPSERT_PROPERTY_SQL, dataSourcePk, name, value,
                        ClientDatabaseConnections.PROPERTIES.get(name).secret());
            }
        });
    }

    /** The enterprise's saved properties by data source (secret values left out). */
    private Map<Integer, List<DataSource.Property>> properties(long enterprisePk) {
        log.debug("Started");
        var order = new ArrayList<>(ClientDatabaseConnections.PROPERTIES.keySet());
        var byPk = new HashMap<Integer, List<DataSource.Property>>();
        for (var row : jdbcTemplate.queryForList(PROPERTIES_SQL, enterprisePk)) {
            var secret = Boolean.TRUE.equals(row.get("is_secret"));
            byPk.computeIfAbsent(((Number) row.get("data_source_pk")).intValue(), k -> new ArrayList<>())
                    .add(new DataSource.Property((String) row.get("name"), secret ? null : (String) row.get("value"),
                            secret, true));
        }
        byPk.values().forEach(list -> list.sort(Comparator.comparingInt((DataSource.Property p) -> {
            var i = order.indexOf(p.name());
            return i < 0 ? order.size() : i;
        }).thenComparing(DataSource.Property::name)));
        return byPk;
    }

    private static String secretName(String value) {
        log.debug("Started");
        var secret = trim(value);
        if (secret == null) {
            throw new IllegalArgumentException("The secret name is required (the name of the credentials in the "
                    + "secrets store, never the password itself).");
        }
        if (secret.length() > MAX_SECRET_NAME || !SECRET_NAME.matcher(secret).matches()) {
            throw new IllegalArgumentException("The secret name may use letters, digits and . _ / - only (e.g. "
                    + "prism/acme/warehouse-reader).");
        }
        return secret;
    }

    private static boolean hasHost(String url) {
        log.debug("Started");
        try {
            var host = new URI(url).getHost();
            return host != null && !host.isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void requireFreeName(long enterprisePk, String name, int ownPk) {
        log.debug("Started");
        if (!jdbcTemplate.queryForList(NAME_TAKEN_SQL, enterprisePk, name, ownPk).isEmpty()) {
            throw new DuplicateNameException(name);
        }
    }

    private static DataSource toDataSource(Map<String, Object> row, Map<Integer, List<DataSource.Property>> properties) {
        log.debug("Started");
        var method = (String) row.get("db_connection_method");
        var pk = ((Number) row.get("data_source_pk")).intValue();
        return new DataSource(
                pk,
                ((Number) row.get("enterprise_pk")).longValue(),
                (String) row.get("name"),
                (String) row.get("description"),
                method != null ? DataSource.DATABASE : DataSource.API,
                method,
                (String) row.get("db_host"),
                row.get("db_port") == null ? null : ((Number) row.get("db_port")).intValue(),
                (String) row.get("db_name"),
                (String) row.get("db_credentials_ref"),
                (String) row.get("api_url"),
                (String) row.get("api_credentials_ref"),
                ((Number) row.get("fact_count")).intValue(),
                ((Number) row.get("dimension_count")).intValue(),
                ((Number) row.get("metric_count")).intValue(),
                timestamp(row.get("created_at")),
                timestamp(row.get("updated_at")),
                List.copyOf(properties.getOrDefault(pk, List.of())));
    }

    private static OffsetDateTime timestamp(Object value) {
        log.debug("Started");
        if (value instanceof Timestamp ts) {
            return ts.toInstant().atOffset(ZoneOffset.UTC);
        }
        if (value instanceof OffsetDateTime odt) {
            return odt;
        }
        return null;
    }

    private static String connectionLabel(Valid valid) {
        log.debug("Started");
        return valid.method() != null ? valid.method() : "api";
    }

    private static Map<String, String> orderedMethods() {
        log.debug("Started");
        var methods = new LinkedHashMap<String, String>();
        methods.put(PRISM_DATABASE, "Prism database (demo data)");
        methods.put("firewall_allow_list", "Firewall allow-list");
        methods.put("private_link", "Private link");
        methods.put("outbound_agent", "Outbound agent");
        methods.put(DIRECT_DEV, "Direct (development only)");
        return java.util.Collections.unmodifiableMap(methods);
    }

    static String trim(String value) {
        log.debug("Started");
        if (value == null) {
            return null;
        }
        var stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    /** Schema or table name written as a quoted identifier (names can't contain '"' per SQL/013; doubled anyway). */
    private static String quote(String identifier) {
        log.debug("Started");
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String plural(long count, String noun) {
        log.debug("Started");
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String firstLine(Throwable e) {
        log.debug("Started");
        var root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        var message = root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        return message.lines().findFirst().orElse(message).strip();
    }
}
