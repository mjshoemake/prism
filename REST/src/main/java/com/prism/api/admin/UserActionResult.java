package com.prism.api.admin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Outcome of a user operation (login, password change, create, reset), serialized as
 * {@code {"success": ..., "message": ...}} or {@code {"success": true, "user": {...}}}.
 * Null components are omitted.
 * <p>
 * {@code temporaryPassword} is set only by create-user and reset-password: the generated password in clear
 * text, returned once so the administrator can pass it on. It is never stored or logged.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserActionResult(boolean success, String message, Map<String, Object> user, String temporaryPassword) {

    public static UserActionResult authenticated(Map<String, Object> user) {
        return new UserActionResult(true, null, user, null);
    }

    public static UserActionResult succeeded(String message) {
        return new UserActionResult(true, message, null, null);
    }

    public static UserActionResult failed(String message) {
        return new UserActionResult(false, message, null, null);
    }

    /** A user created or reset with a generated temporary password. */
    public static UserActionResult withTemporaryPassword(String message, Map<String, Object> user,
                                                         String temporaryPassword) {
        return new UserActionResult(true, message, user, temporaryPassword);
    }

    /** Masks the password so a logged or printed result never reveals it. */
    @Override
    public String toString() {
        return "UserActionResult[success=%s, message=%s, user=%s, temporaryPassword=%s]"
                .formatted(success, message, user, temporaryPassword == null ? null : "****");
    }
}
