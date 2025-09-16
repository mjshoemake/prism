package com.prism.api.web

import com.prism.api.service.DynamicQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/metrics")
class DynamicQueryController {

    private final DynamicQueryService svc

    DynamicQueryController(DynamicQueryService svc) {
        this.svc = svc
    }

    /**
     * Example:
     * GET /api/metrics/sprint
     *   ?focusLevelColumn=td.level_1_name
     *   &valueColumnsForSelectClause=SUM(smf.story_points_completed) AS velocity
     *   &whereClause=WHERE sd.sprint_year=2025
     */
    @GetMapping("/sprint")
    ResponseEntity<List<Map<String, Object>>> getSprintMetrics(
        @RequestParam String focusLevelColumn,
        @RequestParam("valueColumnsForSelectClause") String valueColumnsForSelectClause,
        @RequestParam String whereClause
    ) {
        def rows = svc.fetchMetrics(focusLevelColumn, valueColumnsForSelectClause, whereClause)
        ResponseEntity.ok(rows)
    }
}