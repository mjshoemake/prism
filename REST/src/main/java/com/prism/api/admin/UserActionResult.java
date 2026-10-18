package com.prism.api.admin;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Outcome of a user operation (login, password change), serialized as
 * {@code {"success": ..., "message": ...}} or {@code {"success": true, "user": {...}}}.
 * Null components are omitted, so the JSON matches the maps the Groovy code returned.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserActionResult(boolean success, String message, Map<String, Object> user) {

    public static UserActionResult authenticated(Map<String, Object> user) {
        return new UserActionResult(true, null, user);
    }

    public static UserActionResult succeeded(String message) {
        return new UserActionResult(true, message, null);
    }

    public static UserActionResult failed(String message) {
        return new UserActionResult(false, message, null);
    }
}
