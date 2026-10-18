package com.prism.api.auth;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.prism.api.admin.UserActionResult;
import com.prism.api.admin.UsersService;
import com.prism.common.logging.LogCategory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LogCategory.CONTROLLER.logger(AuthController.class);

    private static final int MIN_PASSWORD_LENGTH = 8;

    public record LoginRequest(String email, String password) {
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {
    }

    private final UsersService usersService;

    public AuthController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/login")
    public ResponseEntity<UserActionResult> login(@RequestBody LoginRequest request) {
        log.debug("login() called.");
        if (!StringUtils.hasLength(request.email()) || !StringUtils.hasLength(request.password())) {
            log.debug("login() rejected: email or password missing.");
            return ResponseEntity.badRequest().body(UserActionResult.failed("Email and password are required."));
        }

        var result = usersService.authenticateUser(request.email(), request.password());
        log.debug("login() for '{}': {}", request.email(), result.success() ? "SUCCESS" : "FAILURE");
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    @PostMapping("/change-password")
    public ResponseEntity<UserActionResult> changePassword(@RequestHeader("X-User-ID") String userId,
                                                           @RequestBody ChangePasswordRequest request) {
        if (!StringUtils.hasLength(request.currentPassword()) || !StringUtils.hasLength(request.newPassword())) {
            return ResponseEntity.badRequest()
                    .body(UserActionResult.failed("Current password and new password are required."));
        }
        if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
            return ResponseEntity.badRequest()
                    .body(UserActionResult.failed("New password must be at least 8 characters long."));
        }

        var result = usersService.changePassword(userId, request.currentPassword(), request.newPassword());
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }
}
