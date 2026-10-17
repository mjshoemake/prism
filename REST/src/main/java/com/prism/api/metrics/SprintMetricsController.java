package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.common.exceptions.ParameterValueRequiredException;
import com.prism.common.exceptions.ResourceNotFoundException;

@RestController
@RequestMapping("/api/metrics")
public class SprintMetricsController {

    private static final Logger log = LoggerFactory.getLogger(SprintMetricsController.class);

    private final SprintMetricsService svc;

    public SprintMetricsController(SprintMetricsService svc) {
        this.svc = svc;
    }

    @PostMapping("/sprint-metrics")
    public ResponseEntity<List<Map<String, Object>>> getSprintMetrics(@RequestBody SprintMetricsRequest request) {
        log.debug("getSprintMetrics() called.");

        // StringUtils.hasLength mirrors Groovy truthiness for Strings: null and "" are rejected, "  " is not.
        // TODO: these should be 400 Bad Request rather than ResourceNotFoundException (currently surfaces as 500).
        if (!StringUtils.hasLength(request.focusLevelColumn())) {
            throw new ResourceNotFoundException("FocusLevelColumn not specified.");
        }
        if (!StringUtils.hasLength(request.valueColumnsForSelectClause())) {
            throw new ResourceNotFoundException("ValueColumnsForSelectClause not specified.");
        }
        if (request.whereClause() == null) {
            throw new ResourceNotFoundException("WhereClause not specified.");
        }

        var rows = svc.fetchMetrics(
                request.focusLevelColumn(),
                request.valueColumnsForSelectClause(),
                request.whereClause());
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/focus-areas-list")
    public ResponseEntity<List<Map<String, Object>>> getFocusAreasList() {
        log.debug("getFocusAreasList() called.");
        return ResponseEntity.ok(svc.fetchFocusAreas());
    }

    @GetMapping("/focus-levels-list/{focusAreaPK}")
    public ResponseEntity<List<Map<String, Object>>> getFocusLevelsList(@PathVariable String focusAreaPK) {
        log.debug("getFocusLevelsList() called.");
        if (!StringUtils.hasLength(focusAreaPK)) {
            throw new ParameterValueRequiredException("Required parameter Focus-Area-ID is empty.");
        }
        return ResponseEntity.ok(svc.fetchFocusLevels(focusAreaPK));
    }
}
