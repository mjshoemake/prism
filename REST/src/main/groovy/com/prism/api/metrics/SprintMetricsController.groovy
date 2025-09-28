package com.prism.api.metrics


import com.prism.common.exceptions.ParameterValueRequiredException
import com.prism.common.exceptions.ResourceNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

//@CrossOrigin(origins = "*") // Allow all origins
@RestController
@RequestMapping("/api/metrics")
class SprintMetricsController {

    private final SprintMetricsService svc

    SprintMetricsController(SprintMetricsService svc) {
        this.svc = svc
    }

    @PostMapping("/sprint-metrics")
    ResponseEntity<List<Map<String, Object>>> getSprintMetrics(
        @RequestBody SprintMetricsRequest request
    ) {
        if (! request.focusLevelColumn) {
            throw new ResourceNotFoundException("FocusLevelColumn not specified.")
        }
        if (! request.valueColumnsForSelectClause) {
            throw new ResourceNotFoundException("ValueColumnsForSelectClause not specified.")
        }
        if (request.whereClause == null) {
            throw new ResourceNotFoundException("WhereClause not specified.")
        }

        List<Map<String, Object>> rows = svc.fetchMetrics(
                request.getFocusLevelColumn(),
                request.getValueColumnsForSelectClause(),
                request.getWhereClause()
        );

        return ResponseEntity.ok(rows);
    }

    //@CrossOrigin(origins = "*")
    @GetMapping("/focus-areas-list")
    ResponseEntity<List<Map<String, Object>>> getFocusAreasList() {
        List<Map<String, Object>> rows = svc.fetchFocusAreas()
        return ResponseEntity.ok(rows)
    }

    //@CrossOrigin(origins = "*")
    @GetMapping("/focus-levels-list/{focusAreaPK}")
    ResponseEntity<List<Map<String, Object>>> getFocusLevelsList(
            @PathVariable String focusAreaPK
    ) {
        if (! focusAreaPK) {
            throw new ParameterValueRequiredException("Required parameter Focus-Area-ID is empty.")
        }
        List<Map<String, Object>> rows = svc.fetchFocusLevels(focusAreaPK)
        return ResponseEntity.ok(rows)
    }

}