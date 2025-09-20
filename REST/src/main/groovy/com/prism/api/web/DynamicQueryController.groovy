package com.prism.api.web

import com.prism.api.web.DynamicQueryService
import com.prism.common.exceptions.ResourceNotFoundException
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
     * GET /api/metrics/level/<>/values/<>/where/<>
     *   ?focusLevelColumn=td.level_1_name
     *   &valueColumnsForSelectClause=SUM(smf.story_points_completed) AS velocity
     *   &whereClause=WHERE sd.sprint_year=2025
     */
    @GetMapping("/level/{focusLevelColumn}/values/{valueColumnsForSelectClause}/where/{whereClause}")
    ResponseEntity<List<Map<String, Object>>> getSprintMetrics(
        @PathVariable String focusLevelColumn,
        @PathVariable String valueColumnsForSelectClause,
        @PathVariable String whereClause
    ) {
        if (! focusLevelColumn) {
            throw new ResourceNotFoundException("FocusLevelColumn not specified.")
        }
        if (! valueColumnsForSelectClause) {
            throw new ResourceNotFoundException("ValueColumnsForSelectClause not specified.")
        }
        if (! whereClause) {
            throw new ResourceNotFoundException("WhereClause not specified.")
        }
        def rows = svc.fetchMetrics(focusLevelColumn, valueColumnsForSelectClause, whereClause)
        ResponseEntity.ok(rows)
    }

    /**
     * API: http://localhost:8080/api/metrics/noparams
     * @return String
     */
    @GetMapping("/noparams")
    String getTestNoParams() {
        //def rows = svc.fetchMetrics(focusLevelColumn, valueColumnsForSelectClause, whereClause)
        "DONE!!!!"
    }

    /**
     * API: http://localhost:8080/api/metrics/sprint/level/level_1_name
     * @param focusLevelColumn
     * @param valueColumnsForSelectClause
     * @param whereClause
     * @return
     */
    @GetMapping("/sprint/level/{focusLevelColumn}")
    ResponseEntity<List<Map<String, Object>>> getSprintMetrics(@PathVariable String focusLevelColumn) {
        if (! focusLevelColumn) {
            throw new ResourceNotFoundException("FocusLevelColumn not specified.")
        }
        String valueColumnsForSelectClause = ""
        String whereClause = ""
        def rows = svc.fetchMetrics(focusLevelColumn, valueColumnsForSelectClause, whereClause)
        ResponseEntity.ok(rows)
    }

    @GetMapping("/sprint/test/{focusLevelColumn}")
    String testSingleParameter(@PathVariable String focusLevelColumn) {
        if (! focusLevelColumn) {
            throw new ResourceNotFoundException("FocusLevelColumn not specified.")
        }
        return "DONE!!!  focusLevelColumn=" + focusLevelColumn
    }

}