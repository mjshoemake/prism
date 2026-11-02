package com.prism.api.metrics;

import java.time.Duration;

import org.slf4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.prism.common.logging.LogCategory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * The connection metric queries run on (roadmap #3, SQL/022): a second connection pool, separate from Prism's own,
 * logged in as the read-only {@code prism_reader} ({@code prism.metric-query.*} in application.properties; password
 * from {@code PRISM_READER_PASSWORD}). Every query also gets a time limit on the Java side ({@code
 * prism.metric-query.timeout}), on top of the login's own {@code statement_timeout}.
 * <p>
 * Fail closed: before its first query the login is checked ({@link ReadOnlyLoginCheck}), and chart queries are refused
 * (503, with a message saying what to do) if it can't connect, if it is Prism's own login, if its transactions aren't
 * read-only, if it is a superuser, or if it can use Prism's schema or read any table in it (SQL/023: Prism's login
 * owns {@code prism}, this one sees only the STAR schema). Metric data is never fetched on Prism's own login instead.
 * <p>
 * The pool is private to this component (not a {@code DataSource} bean), so Spring Boot keeps configuring Prism's own
 * datasource as before. Nothing connects until the first chart request.
 */
@Component
public class MetricQueryConnection implements DisposableBean {

    private static final Logger log = LogCategory.SERVICE.logger(MetricQueryConnection.class);

    private static final String SETUP_HINT =
            "Run SQL/022 and SQL/023 and set PRISM_READER_PASSWORD (REST/.env.local); see README.";

    private final String url;
    private final String username;
    private final String password;
    private final String prismUsername;
    private final Duration timeout;
    private final int poolSize;

    private HikariDataSource pool;
    /** Set once the login has passed the check; queries use it from then on. */
    private volatile JdbcTemplate checked;

    /** The one Spring uses (the other constructor is for tests), hence {@code @Autowired}. */
    @Autowired
    public MetricQueryConnection(@Value("${prism.metric-query.url}") String url,
                                 @Value("${prism.metric-query.username}") String username,
                                 @Value("${prism.metric-query.password:}") String password,
                                 @Value("${spring.datasource.username}") String prismUsername,
                                 @Value("${prism.metric-query.timeout:30s}") Duration timeout,
                                 @Value("${prism.metric-query.pool-size:10}") int poolSize) {
        log.debug("Started");
        this.url = url;
        this.username = username == null ? "" : username.strip();
        this.password = password == null ? "" : password;
        this.prismUsername = prismUsername == null ? "" : prismUsername.strip();
        this.timeout = timeout;
        this.poolSize = poolSize;
    }

    /** For tests that bring their own connection (the parity harness): no pool, no login check. */
    private MetricQueryConnection(JdbcTemplate jdbcTemplate) {
        log.debug("Started");
        this.url = "";
        this.username = "";
        this.password = "";
        this.prismUsername = "";
        this.timeout = Duration.ofSeconds(jdbcTemplate.getQueryTimeout());
        this.poolSize = 0;
        this.checked = jdbcTemplate;
    }

    /** Tests only: queries run on {@code jdbcTemplate} as given, without the read-only login or its check. */
    static MetricQueryConnection unchecked(JdbcTemplate jdbcTemplate) {
        log.debug("Started");
        return new MetricQueryConnection(jdbcTemplate);
    }

    /** The per-query time limit (also given in the timeout message). */
    public Duration timeout() {
        log.debug("Started");
        return timeout;
    }

    /**
     * A JdbcTemplate on the read-only login, with the query time limit set.
     *
     * @throws MetricDataUnavailableException the login isn't usable (see the class comment); retried on the next call
     */
    public JdbcTemplate jdbc() {
        log.debug("Started");
        var ready = checked;
        if (ready != null) {
            return ready;
        }
        synchronized (this) {
            if (checked == null) {
                checked = connectAndCheck();
            }
            return checked;
        }
    }

    private JdbcTemplate connectAndCheck() {
        log.debug("Started");
        if (username.isEmpty()) {
            throw refuse("No metric query login is configured (prism.metric-query.username). " + SETUP_HINT);
        }
        if (username.equalsIgnoreCase(prismUsername)) {
            throw refuse("Metric queries must use their own read-only login, not Prism's ('" + username + "'). "
                    + SETUP_HINT);
        }
        if (pool == null) {
            pool = new HikariDataSource(poolConfig());
        }
        var jdbc = new JdbcTemplate(pool);
        jdbc.setQueryTimeout((int) Math.max(1, timeout.toSeconds()));

        var login = ReadOnlyLoginCheck.verify(jdbc, "the metric data as '" + username + "'",
                "Run SQL/022 and SQL/023 (they confine that login to the STAR schema) and set PRISM_READER_PASSWORD "
                        + "(REST/.env.local); see README.", "METRIC_QUERY_LOGIN");
        log.info("Metric queries use the read-only login '{}' (time limit {}s)", login,
                jdbc.getQueryTimeout());
        return jdbc;
    }

    private HikariConfig poolConfig() {
        log.debug("Started");
        var config = new HikariConfig();
        config.setPoolName("metric-query");
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setReadOnly(true);
        config.setAutoCommit(true);
        config.setMaximumPoolSize(Math.max(1, poolSize));
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5_000);
        // Don't connect while the pool is created: a wrong password shows up as a 503 on the chart, not a failed start.
        config.setInitializationFailTimeout(-1);
        return config;
    }

    private MetricDataUnavailableException refuse(String message) {
        log.debug("Started");
        log.error(message);
        return new MetricDataUnavailableException(message);
    }

    /** The first line of the deepest cause's message (driver messages can run to several lines). */
    static String firstLine(Throwable e) {
        log.debug("Started");
        return ReadOnlyLoginCheck.firstLine(e);
    }

    @Override
    public void destroy() {
        log.debug("Started");
        if (pool != null) {
            pool.close();
        }
    }
}
