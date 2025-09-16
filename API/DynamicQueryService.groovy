package com.prism.api.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class DynamicQueryService {

    private final JdbcTemplate jdbcTemplate

    DynamicQueryService(JdbcTemplate jdbcTemplate) {
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
        def sql = """
            SELECT ${focusLevelColumn}, ${valueColumnsForSelectClause}
              FROM "prism"."Sprint_Metrics_Fact" smf
              JOIN "prism"."Sprint_Dim" sd  ON smf.sprint_pk = sd.sprint_pk
              JOIN "prism"."Team_Dim" td    ON smf.team_pk   = td.team_pk
            ${whereClause}
            GROUP BY ${focusLevelColumn}
            ORDER BY ${focusLevelColumn}
        """
        jdbcTemplate.queryForList(sql)
    }
}