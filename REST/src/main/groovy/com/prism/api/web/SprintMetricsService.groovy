package com.prism.api.web

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class SprintMetricsService {

    private final JdbcTemplate jdbcTemplate

    SprintMetricsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    /**
     * Runs a dynamically‐built SQL using the three template pieces.
     * WARNING: unsanitized input = SQL injection risk. You should validate
     * focusLevelColumn and valueColumnsForSelectClause against a whitelist.
     */
    List<Map<String, Object>> fetchMetrics(
        String focusLevelColumn,
        String valueColumnsForSelectClause,
        String whereClause
    ) {
        if (valueColumnsForSelectClause) {
            valueColumnsForSelectClause = ", " + valueColumnsForSelectClause
        } else {
            valueColumnsForSelectClause = ""
        }
        def sql = """
            SELECT ${focusLevelColumn}${valueColumnsForSelectClause}
            FROM "prism"."Sprint_Metrics_Fact" smf
            JOIN "prism"."Sprint_Dim" sd  ON smf.sprint_pk = sd.sprint_pk
            JOIN "prism"."Team_Dim" td    ON smf.team_pk   = td.team_pk
            ${whereClause}
            GROUP BY ${focusLevelColumn}
            ORDER BY ${focusLevelColumn}
        """
        println("SQL: $sql")
        jdbcTemplate.queryForList(sql)
    }

    /**
     * Runs a dynamically‐built SQL using the three template pieces.
     * WARNING: unsanitized input = SQL injection risk. You should validate
     * focusLevelColumn and valueColumnsForSelectClause against a whitelist.
     */
    List<Map<String, Object>> fetchFocusAreas() {
        def sql = """
            SELECT focus_area_pk, name
            FROM "prism"."Focus_Areas" fa
        """
        println("SQL: $sql")
        jdbcTemplate.queryForList(sql)
    }

    List<Map<String, Object>> fetchFocusLevels(String focusAreaPK) {
        def sql = """
            SELECT focus_area_pk, name, column_name
            FROM "prism"."Focus_Levels" fl
            WHERE focus_area_pk = $focusAreaPK 
        """
        println("SQL: $sql")
        jdbcTemplate.queryForList(sql)
    }
}