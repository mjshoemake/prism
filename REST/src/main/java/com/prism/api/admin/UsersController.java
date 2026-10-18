package com.prism.api.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
@RequestMapping("/api/users")
public class UsersController {

    public record CreateUserRequest(@JsonProperty("enterprise_pk") Long enterprisePk, String email, String username) {
    }

    public record UpdateUserRequest(String email, String username, @JsonProperty("isActive") Boolean isActive) {
    }

    public record UpdatePasswordRequest(String password) {
    }

    public record ResetPasswordRequest(String newPassword) {
    }

    private final UsersService svc;

    public UsersController(UsersService svc) {
        this.svc = svc;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        return ResponseEntity.ok(svc.listUsers());
    }

    @GetMapping("{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {
        return ResponseEntity.of(svc.getUser(userId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest req) {
        // Mirrors the Groovy truthiness check: a null or 0 enterprise_pk, or a null/empty email, is rejected.
        var enterprisePk = req.enterprisePk();
        if (enterprisePk == null || enterprisePk == 0 || !StringUtils.hasLength(req.email())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(svc.createUser(enterprisePk, req.email(), req.username()));
    }

    @PutMapping("{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest req) {
        svc.updateUser(userId, req.email(), req.username(), req.isActive());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{userId}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable String userId, @RequestBody UpdatePasswordRequest req) {
        if (!StringUtils.hasLength(req.password())) {
            return ResponseEntity.badRequest().build();
        }
        svc.updatePassword(userId, req.password());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable String userId, @RequestBody ResetPasswordRequest req) {
        if (!StringUtils.hasLength(req.newPassword())) {
            return ResponseEntity.badRequest().build();
        }
        svc.updatePassword(userId, req.newPassword());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        svc.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
