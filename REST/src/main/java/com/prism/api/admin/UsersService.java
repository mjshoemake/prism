package com.prism.api.admin;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prism.api.common.utils.SecurityUtils;
import com.prism.common.logging.LogCategory;

@Service
public class UsersService {

    private static final Logger log = LogCategory.SERVICE.logger(UsersService.class);
    /** Security events. NOTE: endpoints are unauthenticated, so the acting user is not known yet. */
    private static final Logger audit = LogCategory.AUDIT.logger();

    private static final String PASSWORD_ALGO = "bcrypt";
    private static final String PASSWORD_PARAMS = "{\"cost\":%d}".formatted(SecurityUtils.BCRYPT_COST);
    private static final String INVALID_CREDENTIALS = "Invalid email or password.";

    // email and username are CITEXT: the Postgres driver returns those as PGobject (serialized to JSON as
    // {"type":"citext","value":...}), so every query casts them to text.
    private static final String USER_COLUMNS = """
            user_id, enterprise_pk, email::text AS email, username::text AS username, is_active, is_temp_password, \
            failed_login_count, locked_until, last_login_at, created_at, updated_at""";

    private static final String LIST_USERS_SQL =
            "SELECT %s FROM \"prism\".\"Users\" WHERE enterprise_pk = ? ORDER BY created_at DESC".formatted(USER_COLUMNS);

    private static final String GET_USER_SQL =
            "SELECT %s FROM \"prism\".\"Users\" WHERE user_id = ?".formatted(USER_COLUMNS);

    private static final String ENTERPRISE_TEMP_HASH_SQL = """
            SELECT temp_password_hash FROM "prism"."Enterprises" WHERE enterprise_pk = ?""";

    private static final String INSERT_USER_SQL = """
            INSERT INTO "prism"."Users" (enterprise_pk, email, username, password_hash, password_algo, password_params, is_temp_password)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            RETURNING user_id, enterprise_pk, email::text AS email, username::text AS username, is_active, is_temp_password, created_at, updated_at
            """;

    private static final String UPDATE_USER_SQL = """
            UPDATE "prism"."Users" SET email = ?, username = ?, is_active = COALESCE(?, is_active) WHERE user_id = ?""";

    private static final String UPDATE_PASSWORD_SQL = """
            UPDATE "prism"."Users"
            SET password_hash = ?, password_algo = ?, password_params = ?, password_updated_at = now(), is_temp_password = FALSE
            WHERE user_id = ?
            """;

    /** Admin reset to a typed temporary password; also clears any lockout. */
    private static final String RESET_TO_TEMP_PASSWORD_SQL = """
            UPDATE "prism"."Users"
            SET password_hash = ?, password_algo = ?, password_params = ?, password_updated_at = now(),
                is_temp_password = TRUE, failed_login_count = 0, locked_until = NULL
            WHERE user_id = ?
            """;

    /** Admin reset to the enterprise's default temporary password (Enterprises.temp_password_hash). */
    private static final String RESET_TO_ENTERPRISE_DEFAULT_SQL = """
            UPDATE "prism"."Users" u
            SET password_hash = e.temp_password_hash, password_algo = ?, password_params = ?,
                password_updated_at = now(), is_temp_password = TRUE, failed_login_count = 0, locked_until = NULL
            FROM "prism"."Enterprises" e
            WHERE e.enterprise_pk = u.enterprise_pk
              AND u.user_id = ?
              AND COALESCE(e.temp_password_hash, '') <> ''
            """;

    private static final String DELETE_USER_SQL = """
            DELETE FROM "prism"."Users" WHERE user_id = ?""";

    private static final String AUTHENTICATE_SQL = """
            SELECT user_id, enterprise_pk, email::text AS email, username::text AS username, password_hash, password_algo,
                   is_active, is_temp_password, failed_login_count, locked_until, last_login_at
            FROM "prism"."Users"
            WHERE email = ? AND is_active = TRUE
            """;

    private static final String RECORD_SUCCESSFUL_LOGIN_SQL = """
            UPDATE "prism"."Users"
            SET failed_login_count = 0, last_login_at = now()
            WHERE user_id = ?
            """;

    private static final String INCREMENT_FAILED_LOGIN_SQL = """
            UPDATE "prism"."Users"
            SET failed_login_count = failed_login_count + 1,
                locked_until = CASE
                    WHEN failed_login_count + 1 >= 5 THEN now() + INTERVAL '15 minutes'
                    ELSE locked_until
                END
            WHERE user_id = ?
            """;

    private static final String CURRENT_HASH_SQL = """
            SELECT password_hash FROM "prism"."Users" WHERE user_id = ? AND is_active = TRUE""";

    private final JdbcTemplate jdbcTemplate;

    public UsersService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Users of one enterprise (active and inactive; the UI hides inactive ones by default).
     * TODO(backlog #5): take the enterprise from the authenticated session instead of the request.
     */
    public List<Map<String, Object>> listUsers(long enterprisePk) {
        log.debug("Started");
        return jdbcTemplate.queryForList(LIST_USERS_SQL, enterprisePk);
    }

    /** User by ID; empty if the ID is not a valid UUID or no such user exists. */
    public Optional<Map<String, Object>> getUser(String userId) {
        log.debug("Started");
        if (!isValidUserId(userId)) {
            return Optional.empty();
        }
        try {
            return Optional.of(jdbcTemplate.queryForMap(GET_USER_SQL, parseUserId(userId)));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a user who must choose a new password at first login.
     *
     * @param temporaryPassword the initial temporary password, or {@code null} to use the enterprise's
     *                          default temporary password
     * @throws IllegalStateException with a user-facing message if the enterprise doesn't exist or has no
     *                               default temporary password (and none was given)
     * @throws org.springframework.dao.DuplicateKeyException if the email is already in use
     */
    public Map<String, Object> createUser(Long enterprisePk, String email, String username, String temporaryPassword) {
        log.debug("Started");
        String passwordHash;
        if (temporaryPassword != null) {
            passwordHash = SecurityUtils.encryptPassword(temporaryPassword);
        } else {
            try {
                passwordHash = jdbcTemplate.queryForObject(ENTERPRISE_TEMP_HASH_SQL, String.class, enterprisePk);
            } catch (EmptyResultDataAccessException e) {
                throw new IllegalStateException("Unknown enterprise.", e);
            }
            if (!StringUtils.hasLength(passwordHash)) {
                throw new IllegalStateException(
                        "No default temporary password is configured for this enterprise. Enter a temporary password instead.");
            }
        }
        var created = jdbcTemplate.queryForMap(INSERT_USER_SQL,
                enterprisePk, email, username, passwordHash, PASSWORD_ALGO, passwordParams());
        audit.info("USER_CREATED userId={} email={} enterprisePk={} password={}", created.get("user_id"), email,
                enterprisePk, temporaryPassword == null ? "enterprise-default" : "admin-entered");
        return created;
    }

    public int updateUser(String userId, String email, String username, Boolean isActive) {
        log.debug("Started");
        int rows = jdbcTemplate.update(UPDATE_USER_SQL, email, username, isActive, parseUserId(userId));
        audit.info("USER_UPDATED userId={} email={} isActive={} rows={}", userId, email, isActive, rows);
        return rows;
    }

    /** Administrator sets a user's password (PUT /api/users/{id}/password and /reset-password). */
    public int updatePassword(String userId, String rawPassword) {
        log.debug("Started");
        int rows = storePassword(userId, rawPassword);
        audit.info("PASSWORD_SET userId={} rows={}", userId, rows);
        return rows;
    }

    /**
     * Resets a user's password to a temporary one that must be changed at the next login.
     *
     * @param temporaryPassword the new temporary password, or {@code null} to use the enterprise's
     *                          default temporary password
     */
    public UserActionResult resetToTemporaryPassword(String userId, String temporaryPassword) {
        log.debug("Started");
        var uuid = parseUserId(userId);
        boolean useDefault = temporaryPassword == null;
        int rows = useDefault
                ? jdbcTemplate.update(RESET_TO_ENTERPRISE_DEFAULT_SQL, PASSWORD_ALGO, passwordParams(), uuid)
                : jdbcTemplate.update(RESET_TO_TEMP_PASSWORD_SQL,
                        SecurityUtils.encryptPassword(temporaryPassword), PASSWORD_ALGO, passwordParams(), uuid);
        if (rows == 0) {
            if (getUser(userId).isEmpty()) {
                return UserActionResult.failed("User not found.");
            }
            return UserActionResult.failed(
                    "No default temporary password is configured for this enterprise. Enter a temporary password instead.");
        }
        audit.info("PASSWORD_RESET userId={} source={}", userId, useDefault ? "enterprise-default" : "admin-entered");
        return UserActionResult.succeeded("Password reset. The user must choose a new password at next login.");
    }

    public int deleteUser(String userId) {
        log.debug("Started");
        int rows = jdbcTemplate.update(DELETE_USER_SQL, parseUserId(userId));
        audit.info("USER_DELETED userId={} rows={}", userId, rows);
        return rows;
    }

    public UserActionResult authenticateUser(String email, String password) {
        log.debug("Started");

        Map<String, Object> user;
        try {
            user = jdbcTemplate.queryForMap(AUTHENTICATE_SQL, email);
        } catch (EmptyResultDataAccessException e) {
            audit.info("LOGIN_FAILED email={} reason=unknown-or-inactive-user", email);
            return UserActionResult.failed(INVALID_CREDENTIALS);
        }

        var userId = user.get("user_id");
        if (isInFuture(user.get("locked_until"))) {
            audit.info("LOGIN_REJECTED userId={} email={} reason=account-locked", userId, email);
            return UserActionResult.failed("Account is temporarily locked. Please try again later.");
        }

        if (!(user.get("password_hash") instanceof String hash) || hash.isEmpty()) {
            incrementFailedLoginCount(userId.toString());
            audit.info("LOGIN_FAILED userId={} email={} reason=no-password-set", userId, email);
            return UserActionResult.failed(INVALID_CREDENTIALS);
        }

        if (!SecurityUtils.verifyPassword(password, hash)) {
            incrementFailedLoginCount(userId.toString());
            audit.info("LOGIN_FAILED userId={} email={} reason=bad-password", userId, email);
            return UserActionResult.failed(INVALID_CREDENTIALS);
        }

        jdbcTemplate.update(RECORD_SUCCESSFUL_LOGIN_SQL, userId);
        audit.info("LOGIN_SUCCEEDED userId={} email={}", userId, email);

        // Never return credential details to the client.
        user.remove("password_hash");
        user.remove("password_algo");
        return UserActionResult.authenticated(user);
    }

    public UserActionResult changePassword(String userId, String currentPassword, String newPassword) {
        log.debug("Started");
        try {
            var currentHash = jdbcTemplate.queryForObject(CURRENT_HASH_SQL, String.class, parseUserId(userId));
            if (!SecurityUtils.verifyPassword(currentPassword, currentHash)) {
                audit.info("PASSWORD_CHANGE_FAILED userId={} reason=bad-current-password", userId);
                return UserActionResult.failed("Current password is incorrect.");
            }
            storePassword(userId, newPassword);
            audit.info("PASSWORD_CHANGED userId={}", userId);
            return UserActionResult.succeeded("Password changed successfully.");
        } catch (IllegalArgumentException e) {
            return UserActionResult.failed("Invalid user identifier.");
        } catch (EmptyResultDataAccessException e) {
            return UserActionResult.failed("User not found.");
        }
    }

    private int storePassword(String userId, String rawPassword) {
        log.debug("Started");
        var hash = SecurityUtils.encryptPassword(rawPassword);
        return jdbcTemplate.update(UPDATE_PASSWORD_SQL, hash, PASSWORD_ALGO, passwordParams(), parseUserId(userId));
    }

    private void incrementFailedLoginCount(String userId) {
        log.debug("Started");
        jdbcTemplate.update(INCREMENT_FAILED_LOGIN_SQL, UUID.fromString(userId));
    }

    /** True if the ID is a well-formed UUID (user_id is a UUID column). */
    public static boolean isValidUserId(String userId) {
        log.debug("Started");
        try {
            UUID.fromString(userId);
            return true;
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }

    private static UUID parseUserId(String userId) {
        log.debug("Started");
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid user ID format", e);
        }
    }

    private static PGobject passwordParams() {
        log.debug("Started");
        var jsonb = new PGobject();
        jsonb.setType("jsonb");
        try {
            jsonb.setValue(PASSWORD_PARAMS);
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to build password_params JSON", e);
        }
        return jsonb;
    }

    /** The Postgres driver returns timestamps as java.sql.Timestamp; OffsetDateTime is handled for safety. */
    private static boolean isInFuture(Object timestamp) {
        log.debug("Started");
        return switch (timestamp) {
            case Date date -> date.after(new Date());
            case OffsetDateTime dateTime -> dateTime.isAfter(OffsetDateTime.now());
            case null, default -> false;
        };
    }
}
