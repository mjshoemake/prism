package com.prism.api.settings;

import java.util.Map;

import org.slf4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prism.api.admin.UserActionResult;
import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

/**
 * Admin → System Settings. Settings are per enterprise.
 * TODO(backlog #5): take the enterprise from the authenticated session instead of the request.
 */
@RestController
@RequestMapping("/api/settings")
public class SystemSettingsController {

    private static final Logger log = LogCategory.CONTROLLER.logger(SystemSettingsController.class);

    public record SaveSettingsRequest(@JsonProperty("enterprise_pk") Long enterprisePk,
                                      @JsonProperty("temp_password_valid_hours") Integer tempPasswordValidHours) {
    }

    private final SystemSettingsService svc;

    public SystemSettingsController(SystemSettingsService svc) {
        this.svc = svc;
    }

    /** {enterprise_pk, temp_password_valid_hours, updated_at}; 404 if the enterprise has no settings row. */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getSettings(@RequestParam("enterprisePk") long enterprisePk) {
        log.debug("Started");
        return PerformanceLog.time("SystemSettingsController.getSettings",
                () -> ResponseEntity.of(svc.getSettings(enterprisePk)));
    }

    /** Saves the enterprise's settings. 200/400 with a UserActionResult body. */
    @PutMapping
    public ResponseEntity<UserActionResult> saveSettings(@RequestBody SaveSettingsRequest req) {
        log.debug("Started");
        return PerformanceLog.time("SystemSettingsController.saveSettings", () -> {
            if (req.enterprisePk() == null || req.enterprisePk() == 0) {
                return ResponseEntity.badRequest().body(UserActionResult.failed("Enterprise is required."));
            }
            try {
                var result = svc.saveSettings(req.enterprisePk(), req.tempPasswordValidHours());
                return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
            } catch (DataIntegrityViolationException e) {
                return ResponseEntity.badRequest().body(UserActionResult.failed("Unknown enterprise."));
            }
        });
    }
}
