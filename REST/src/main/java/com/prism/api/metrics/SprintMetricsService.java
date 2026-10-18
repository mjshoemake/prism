package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

@Service
public class SprintMetricsService {

    private static final Logger sqlLog = LogCategory.SQL.logger(SprintMetricsService.class);

    private static final String METRICS_SQL = """
            SELECT %1$s%2$s
            FROM "prism"."Sprint_Metrics_Fact" smf
            JOIN "prism"."Sprint_Dim" sd  ON smf.sprint_pk = sd.sprint_pk
            JOIN "prism"."Team_Dim" td    ON smf.team_pk   = td.team_pk
            %3$s
            GROUP BY %1$s
            ORDER BY %1$s
            """;

    private static final String FOCUS_AREAS_SQL = """
            SELECT focus_area_pk, name
            FROM "prism"."Focus_Areas" fa
            """;

    private static final String FOCUS_LEVELS_SQL = """
            SELECT focus_area_pk, name, column_name
            FROM "prism"."Focus_Levels" fl
            WHERE focus_area_pk = %s
            """;

    private final JdbcTemplate jdbcTemplate;

    public SprintMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Runs a dynamically built SQL query from the three template pieces.
     * <p>
     * WARNING: the inputs are client-supplied SQL fragments and are not sanitized (SQL injection risk).
     * TODO: redesign this endpoint so the client no longer sends raw SQL.
     */
    public List<Map<String, Object>> fetchMetrics(String focusLevelColumn,
                                                  String valueColumnsForSelectClause,
                                                  String whereClause) {
        var valueColumns = StringUtils.hasLength(valueColumnsForSelectClause)
                ? ", " + valueColumnsForSelectClause
                : "";
        var sql = METRICS_SQL.formatted(focusLevelColumn, valueColumns, whereClause);
        sqlLog.debug("SQL: {}", sql);
        return PerformanceLog.time("fetchMetrics", () -> jdbcTemplate.queryForList(sql));
    }

    public List<Map<String, Object>> fetchFocusAreas() {
        sqlLog.debug("SQL: {}", FOCUS_AREAS_SQL);
        return PerformanceLog.time("fetchFocusAreas", () -> jdbcTemplate.queryForList(FOCUS_AREAS_SQL));
    }

    /**
     * TODO: focusAreaPK is interpolated into the SQL text (SQL injection risk), as in the original Groovy.
     * Switch to a bound parameter once the column type of Focus_Levels.focus_area_pk is confirmed.
     */
    public List<Map<String, Object>> fetchFocusLevels(String focusAreaPK) {
        var sql = FOCUS_LEVELS_SQL.formatted(focusAreaPK);
        sqlLog.debug("SQL: {}", sql);
        return PerformanceLog.time("fetchFocusLevels", () -> jdbcTemplate.queryForList(sql));
    }
}
