package com.prism.api.dashboards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prism.api.metrics.MetricsMetadataCache;
import com.prism.common.logging.LogCategory;

/**
 * Users' saved dashboards ("prism"."Dashboards" + "Dashboard_Charts", SQL/020). Every operation is limited to the
 * dashboards of the given user.
 * TODO (roadmap #4): the user comes from the X-User-ID header until real sessions exist.
 */
@Service
public class DashboardsService {

    private static final Logger log = LogCategory.SERVICE.logger(DashboardsService.class);

    /** Layouts: one chart, two stacked, four quadrants. */
    public static final Set<Integer> LAYOUTS = Set.of(1, 2, 4);
    public static final int MAX_POSITION = 4;
    public static final int MAX_NAME = 60;

    private static final String ACTIVE_USER_SQL = """
            SELECT 1 FROM "prism"."Users" WHERE user_id = ? AND is_active
            """;

    private static final String LIST_SQL = """
            SELECT d.dashboard_pk, d.name, d.layout, c.position, c.metric_pk
            FROM "prism"."Dashboards" d
            LEFT JOIN "prism"."Dashboard_Charts" c ON c.dashboard_pk = d.dashboard_pk
            WHERE d.user_id = ?
            ORDER BY d.name, d.dashboard_pk, c.position
            """;

    private static final String OWNED_SQL = """
            SELECT 1 FROM "prism"."Dashboards" WHERE dashboard_pk = ? AND user_id = ?
            """;

    private static final String NAME_TAKEN_SQL = """
            SELECT 1 FROM "prism"."Dashboards" WHERE user_id = ? AND name = ? AND dashboard_pk <> ?
            """;

    private static final String INSERT_SQL = """
            INSERT INTO "prism"."Dashboards" (user_id, name, layout) VALUES (?, ?, ?)
            RETURNING dashboard_pk
            """;

    private static final String UPDATE_SQL = """
            UPDATE "prism"."Dashboards" SET name = ?, layout = ? WHERE dashboard_pk = ? AND user_id = ?
            """;

    private static final String DELETE_CHARTS_SQL = """
            DELETE FROM "prism"."Dashboard_Charts" WHERE dashboard_pk = ?
            """;

    private static final String INSERT_CHART_SQL = """
            INSERT INTO "prism"."Dashboard_Charts" (dashboard_pk, position, metric_pk) VALUES (?, ?, ?)
            """;

    private static final String DELETE_SQL = """
            DELETE FROM "prism"."Dashboards" WHERE dashboard_pk = ? AND user_id = ?
            """;

    /** A name already used by another of the user's dashboards (answered with 409). */
    public static class DuplicateNameException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DuplicateNameException(String name) {
            super("You already have a dashboard named '" + name + "'.");
        }
    }

    private final JdbcTemplate jdbcTemplate;
    private final MetricsMetadataCache metadata;

    public DashboardsService(JdbcTemplate jdbcTemplate, MetricsMetadataCache metadata) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadata = metadata;
    }

    /** The user's dashboards, by name, each with its chart areas in position order. */
    public List<Dashboard> list(UUID userId) {
        log.debug("Started");
        var byPk = new LinkedHashMap<Integer, Dashboard>();
        for (var row : jdbcTemplate.queryForList(LIST_SQL, userId)) {
            var pk = ((Number) row.get("dashboard_pk")).intValue();
            var dashboard = byPk.computeIfAbsent(pk, k -> new Dashboard(k, (String) row.get("name"),
                    ((Number) row.get("layout")).intValue(), new ArrayList<>()));
            if (row.get("position") != null) {
                dashboard.charts().add(new Dashboard.ChartArea(((Number) row.get("position")).intValue(),
                        ((Number) row.get("metric_pk")).intValue()));
            }
        }
        return byPk.values().stream()
                .map(d -> new Dashboard(d.dashboardPk(), d.name(), d.layout(), List.copyOf(d.charts()))).toList();
    }

    /** One of the user's dashboards; empty if it doesn't exist or belongs to someone else. */
    public Optional<Dashboard> get(UUID userId, int dashboardPk) {
        log.debug("Started");
        return list(userId).stream().filter(d -> d.dashboardPk() == dashboardPk).findFirst();
    }

    /**
     * @throws IllegalArgumentException invalid request or unknown/inactive user (400)
     * @throws DuplicateNameException   the user already has a dashboard with that name (409)
     */
    @Transactional
    public Dashboard create(UUID userId, DashboardRequest request) {
        log.debug("Started");
        var valid = validate(request);
        requireActiveUser(userId);
        requireActiveMetrics(valid.charts());
        requireFreeName(userId, valid.name(), -1);
        var pk = jdbcTemplate.queryForObject(INSERT_SQL, Integer.class, userId, valid.name(), valid.layout());
        if (pk == null) {
            throw new IllegalStateException("No dashboard_pk returned.");
        }
        insertCharts(pk, valid.charts());
        return new Dashboard(pk, valid.name(), valid.layout(), valid.charts());
    }

    /**
     * Replaces the dashboard's name, layout and chart areas.
     *
     * @return empty if the user has no such dashboard (404)
     * @throws IllegalArgumentException invalid request (400)
     * @throws DuplicateNameException   another of the user's dashboards has that name (409)
     */
    @Transactional
    public Optional<Dashboard> update(UUID userId, int dashboardPk, DashboardRequest request) {
        log.debug("Started");
        var valid = validate(request);
        // Ownership first: someone else's dashboard is "not found", whatever else is wrong with the request.
        if (jdbcTemplate.queryForList(OWNED_SQL, dashboardPk, userId).isEmpty()) {
            return Optional.empty();
        }
        requireActiveMetrics(valid.charts());
        requireFreeName(userId, valid.name(), dashboardPk);
        if (jdbcTemplate.update(UPDATE_SQL, valid.name(), valid.layout(), dashboardPk, userId) == 0) {
            return Optional.empty();
        }
        jdbcTemplate.update(DELETE_CHARTS_SQL, dashboardPk);
        insertCharts(dashboardPk, valid.charts());
        return Optional.of(new Dashboard(dashboardPk, valid.name(), valid.layout(), valid.charts()));
    }

    /** @return false if the user has no such dashboard (404). Its chart areas are deleted with it. */
    public boolean delete(UUID userId, int dashboardPk) {
        log.debug("Started");
        return jdbcTemplate.update(DELETE_SQL, dashboardPk, userId) > 0;
    }

    /**
     * The request with its name trimmed and chart areas in position order.
     *
     * @throws IllegalArgumentException with a user-facing message
     */
    static Dashboard validate(DashboardRequest request) {
        log.debug("Started");
        if (request == null) {
            throw new IllegalArgumentException("A dashboard is required.");
        }
        var name = request.name() == null ? "" : request.name().strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("A dashboard name is required.");
        }
        if (name.length() > MAX_NAME) {
            throw new IllegalArgumentException("The dashboard name is longer than " + MAX_NAME + " characters.");
        }
        if (request.layout() == null || !LAYOUTS.contains(request.layout())) {
            throw new IllegalArgumentException("The layout must be 1, 2 or 4 charts.");
        }
        var charts = new ArrayList<Dashboard.ChartArea>();
        var positions = new HashSet<Integer>();
        for (var chart : request.charts() == null ? List.<Dashboard.ChartArea>of() : request.charts()) {
            if (chart == null || chart.position() == null || chart.position() < 1 || chart.position() > MAX_POSITION) {
                throw new IllegalArgumentException("Chart positions are 1 to " + MAX_POSITION + ".");
            }
            if (chart.metricPk() == null) {
                throw new IllegalArgumentException("Chart " + chart.position() + " needs a metric_pk.");
            }
            if (!positions.add(chart.position())) {
                throw new IllegalArgumentException("Chart position " + chart.position() + " is given twice.");
            }
            charts.add(chart);
        }
        charts.sort((a, b) -> Integer.compare(a.position(), b.position()));
        return new Dashboard(0, name, request.layout(), List.copyOf(charts));
    }

    private void requireActiveUser(UUID userId) {
        log.debug("Started");
        if (jdbcTemplate.queryForList(ACTIVE_USER_SQL, userId).isEmpty()) {
            throw new IllegalArgumentException("Unknown or inactive user.");
        }
    }

    private void requireActiveMetrics(List<Dashboard.ChartArea> charts) {
        log.debug("Started");
        for (var chart : charts) {
            if (metadata.metric(chart.metricPk()).isEmpty()) {
                throw new IllegalArgumentException("Unknown metric: " + chart.metricPk());
            }
        }
    }

    private void requireFreeName(UUID userId, String name, int ownPk) {
        log.debug("Started");
        if (!jdbcTemplate.queryForList(NAME_TAKEN_SQL, userId, name, ownPk).isEmpty()) {
            throw new DuplicateNameException(name);
        }
    }

    private void insertCharts(int dashboardPk, List<Dashboard.ChartArea> charts) {
        log.debug("Started");
        for (var chart : charts) {
            jdbcTemplate.update(INSERT_CHART_SQL, dashboardPk, chart.position(), chart.metricPk());
        }
    }
}
