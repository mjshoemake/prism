package com.prism.api.admin;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prism.common.logging.LogCategory;
import com.prism.common.logging.PerformanceLog;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    private static final Logger log = LogCategory.CONTROLLER.logger(UsersController.class);

    /** {@code temporaryPassword} null or absent = use the enterprise's default temporary password. */
    public record CreateUserRequest(@JsonProperty("enterprise_pk") Long enterprisePk, String email, String username,
                                    String temporaryPassword) {
    }

    public record UpdateUserRequest(String email, String username, @JsonProperty("isActive") Boolean isActive) {
    }

    public record UpdatePasswordRequest(String password) {
    }

    /** {@code temporaryPassword} null or absent = use the enterprise's default temporary password. */
    public record ResetPasswordRequest(String temporaryPassword) {
    }

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UsersService svc;

    public UsersController(UsersService svc) {
        this.svc = svc;
    }

    /**
     * Users of one enterprise. TODO(backlog #5): the enterprise comes from the request until the API has
     * authenticated sessions; then it must come from the signed-in user (and the parameter go away).
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(@RequestParam("enterprisePk") long enterprisePk) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.listUsers", () -> ResponseEntity.ok(svc.listUsers(enterprisePk)));
    }

    @GetMapping("{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.getUser", () -> ResponseEntity.of(svc.getUser(userId)));
    }

    /**
     * 200 with the created user; 400 {success:false,message} for invalid input or a missing enterprise default
     * temporary password; 409 if the email is already used.
     * TODO(backlog #5): take the enterprise from the authenticated session instead of the request body.
     */
    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody CreateUserRequest req) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.createUser", () -> {
            // Mirrors the Groovy truthiness check: a null or 0 enterprise_pk, or a null/empty email, is rejected.
            var enterprisePk = req.enterprisePk();
            if (enterprisePk == null || enterprisePk == 0 || !StringUtils.hasLength(req.email())) {
                return ResponseEntity.badRequest().body(UserActionResult.failed("Email is required."));
            }
            var temporaryPassword = req.temporaryPassword();
            if (temporaryPassword != null && temporaryPassword.length() < MIN_PASSWORD_LENGTH) {
                return ResponseEntity.badRequest().body(UserActionResult.failed(
                        "Temporary password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
            }
            try {
                return ResponseEntity.ok(svc.createUser(enterprisePk, req.email(), req.username(), temporaryPassword));
            } catch (IllegalStateException e) {
                return ResponseEntity.badRequest().body(UserActionResult.failed(e.getMessage()));
            } catch (DuplicateKeyException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(UserActionResult.failed("A user with this email already exists."));
            }
        });
    }

    @PutMapping("{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest req) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.updateUser", () -> {
            if (!UsersService.isValidUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            svc.updateUser(userId, req.email(), req.username(), req.isActive());
            return ResponseEntity.noContent().build();
        });
    }

    @PutMapping("{userId}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable String userId, @RequestBody UpdatePasswordRequest req) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.updatePassword", () -> {
            if (!UsersService.isValidUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            if (!StringUtils.hasLength(req.password())) {
                return ResponseEntity.badRequest().build();
            }
            svc.updatePassword(userId, req.password());
            return ResponseEntity.noContent().build();
        });
    }

    @PutMapping("{userId}/reset-password")
    public ResponseEntity<UserActionResult> resetPassword(@PathVariable String userId,
                                                          @RequestBody(required = false) ResetPasswordRequest req) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.resetPassword", () -> {
            if (!UsersService.isValidUserId(userId)) {
                return ResponseEntity.badRequest().body(UserActionResult.failed("Invalid user identifier."));
            }
            String temporaryPassword = req == null ? null : req.temporaryPassword();
            if (temporaryPassword != null && temporaryPassword.length() < MIN_PASSWORD_LENGTH) {
                return ResponseEntity.badRequest().body(UserActionResult.failed(
                        "Temporary password must be at least " + MIN_PASSWORD_LENGTH + " characters long."));
            }
            var result = svc.resetToTemporaryPassword(userId, temporaryPassword);
            return result.success() ? ResponseEntity.ok(result) : ResponseEntity.badRequest().body(result);
        });
    }

    @DeleteMapping("{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        log.debug("Started");
        return PerformanceLog.time("UsersController.deleteUser", () -> {
            if (!UsersService.isValidUserId(userId)) {
                return ResponseEntity.badRequest().build();
            }
            svc.deleteUser(userId);
            return ResponseEntity.noContent().build();
        });
    }
}
