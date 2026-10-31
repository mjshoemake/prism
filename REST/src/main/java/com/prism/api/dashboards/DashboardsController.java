package com.prism.api.dashboards;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * /api/dashboards: the signed-in user's saved dashboards (SQL/020). Errors are {success:false, message}: 400 for an
 * invalid request or user, 404 for a dashboard the user doesn't have, 409 for a name already in use.
 * TODO (roadmap #4): the user comes from the X-User-ID header (as /api/auth/change-password) until real sessions exist.
 */
@RestController
@RequestMapping("/api/dashboards")
public class DashboardsController {

    private static final Logger log = LogCategory.CONTROLLER.logger(DashboardsController.class);

    private final DashboardsService svc;

    public DashboardsController(DashboardsService svc) {
        this.svc = svc;
    }

    /** The user's dashboards, by name: {@code [{dashboard_pk, name, layout, charts:[{position, metric_pk}]}]}. */
    @GetMapping
    public ResponseEntity<Object> list(@RequestHeader("X-User-ID") String userId) {
        log.debug("Started");
        return PerformanceLog.time("DashboardsController.list", () -> handle(() -> {
            List<Dashboard> dashboards = svc.list(userId(userId));
            return ResponseEntity.ok(dashboards);
        }));
    }

    /** Creates a dashboard: body {name, layout, charts}; 200 with the saved dashboard. */
    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader("X-User-ID") String userId,
                                         @RequestBody DashboardRequest request) {
        log.debug("Started");
        return PerformanceLog.time("DashboardsController.create",
                () -> handle(() -> ResponseEntity.ok(svc.create(userId(userId), request))));
    }

    /** Replaces a dashboard's name, layout and chart areas; 200 with the saved dashboard, 404 if not the user's. */
    @PutMapping("/{dashboardPk}")
    public ResponseEntity<Object> update(@RequestHeader("X-User-ID") String userId,
                                         @PathVariable("dashboardPk") int dashboardPk,
                                         @RequestBody DashboardRequest request) {
        log.debug("Started");
        return PerformanceLog.time("DashboardsController.update", () -> handle(() -> svc.update(userId(userId), dashboardPk, request)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> error(HttpStatus.NOT_FOUND, "Dashboard not found."))));
    }

    /** Deletes a dashboard and its chart areas; 204, or 404 if not the user's. */
    @DeleteMapping("/{dashboardPk}")
    public ResponseEntity<Object> delete(@RequestHeader("X-User-ID") String userId,
                                         @PathVariable("dashboardPk") int dashboardPk) {
        log.debug("Started");
        return PerformanceLog.time("DashboardsController.delete", () -> handle(() -> svc.delete(userId(userId), dashboardPk)
                ? ResponseEntity.noContent().build()
                : error(HttpStatus.NOT_FOUND, "Dashboard not found.")));
    }

    private static ResponseEntity<Object> handle(Supplier<ResponseEntity<Object>> work) {
        log.debug("Started");
        try {
            return work.get();
        } catch (DashboardsService.DuplicateNameException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (DuplicateKeyException e) {
            // Two saves with the same name at once: the unique constraint (SQL/020) catches the second.
            return error(HttpStatus.CONFLICT, "You already have a dashboard with that name.");
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static UUID userId(String value) {
        log.debug("Started");
        try {
            return UUID.fromString(value == null ? "" : value.strip());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("X-User-ID must be a user ID.");
        }
    }

    private static ResponseEntity<Object> error(HttpStatus status, String message) {
        log.debug("Started");
        return ResponseEntity.status(status).body(Map.of("success", false, "message", message));
    }
}
