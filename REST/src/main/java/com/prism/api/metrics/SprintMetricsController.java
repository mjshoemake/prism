package com.prism.api.metrics;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

@RestController
@RequestMapping("/api/metrics")
public class SprintMetricsController {

    private static final Logger log = LogCategory.CONTROLLER.logger(SprintMetricsController.class);

    private final SprintMetricsService svc;

    public SprintMetricsController(SprintMetricsService svc) {
        this.svc = svc;
    }

    /** Active metrics in display order: display fields only (title, unit, decimals, scale), never SQL. */
    @GetMapping("/list")
    public ResponseEntity<List<MetricInfo>> getMetricList() {
        log.debug("Started");
        return PerformanceLog.time("SprintMetricsController.getMetricList",
                () -> ResponseEntity.ok(svc.fetchMetricList()));
    }

    /**
     * Chart rows {@code [{label, value}, ...]}. The body holds only IDs and values ({@link ChartDataRequest}); the
     * server builds the SQL from metadata. 400 {success:false, message} for an unknown metric or focus level, an
     * unsupported operator, or too many filters.
     */
    @PostMapping("/data")
    public ResponseEntity<Object> getChartData(@RequestBody ChartDataRequest request) {
        log.debug("Started");
        return PerformanceLog.time("SprintMetricsController.getChartData", () -> {
            try {
                return ResponseEntity.ok(svc.fetchChartData(request));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
            }
        });
    }

    @GetMapping("/focus-areas-list")
    public ResponseEntity<List<Map<String, Object>>> getFocusAreasList() {
        log.debug("Started");
        return PerformanceLog.time("SprintMetricsController.getFocusAreasList",
                () -> ResponseEntity.ok(svc.fetchFocusAreas()));
    }

    @GetMapping("/focus-levels-list/{focusAreaPK}")
    public ResponseEntity<List<Map<String, Object>>> getFocusLevelsList(@PathVariable int focusAreaPK) {
        log.debug("Started");
        // focusAreaPK is bound as an int: a non-numeric value is rejected by Spring with 400 Bad Request.
        return PerformanceLog.time("SprintMetricsController.getFocusLevelsList",
                () -> ResponseEntity.ok(svc.fetchFocusLevels(focusAreaPK)));
    }
}
