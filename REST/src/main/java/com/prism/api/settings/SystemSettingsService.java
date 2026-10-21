package com.prism.api.settings;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.prism.api.admin.UserActionResult;
import com.prism.common.logging.LogCategory;

/**
 * Enterprise-specific system settings ("prism"."System_Settings", one row per enterprise; SQL/010).
 * Today the only setting is how long a generated temporary password stays valid.
 */
@Service
public class SystemSettingsService {

    private static final Logger log = LogCategory.SERVICE.logger(SystemSettingsService.class);
    private static final Logger audit = LogCategory.AUDIT.logger();

    /** Bounds of temp_password_valid_hours; the same range is enforced by a CHECK constraint (SQL/010). */
    public static final int MIN_TEMP_PASSWORD_VALID_HOURS = 1;
    public static final int MAX_TEMP_PASSWORD_VALID_HOURS = 720;

    private static final String GET_SETTINGS_SQL = """
            SELECT enterprise_pk, temp_password_valid_hours, updated_at
            FROM "prism"."System_Settings"
            WHERE enterprise_pk = ?
            """;

    private static final String UPSERT_SETTINGS_SQL = """
            INSERT INTO "prism"."System_Settings" (enterprise_pk, temp_password_valid_hours)
            VALUES (?, ?)
            ON CONFLICT (enterprise_pk)
            DO UPDATE SET temp_password_valid_hours = EXCLUDED.temp_password_valid_hours
            """;

    private final JdbcTemplate jdbcTemplate;

    public SystemSettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Settings of one enterprise; empty if the enterprise has no settings row yet. */
    public Optional<Map<String, Object>> getSettings(long enterprisePk) {
        log.debug("Started");
        try {
            return Optional.of(jdbcTemplate.queryForMap(GET_SETTINGS_SQL, enterprisePk));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Validates and stores the enterprise's settings (creating its row if needed). */
    public UserActionResult saveSettings(long enterprisePk, Integer tempPasswordValidHours) {
        log.debug("Started");
        if (tempPasswordValidHours == null
                || tempPasswordValidHours < MIN_TEMP_PASSWORD_VALID_HOURS
                || tempPasswordValidHours > MAX_TEMP_PASSWORD_VALID_HOURS) {
            return UserActionResult.failed("Temporary passwords must be valid for between %d and %d hours."
                    .formatted(MIN_TEMP_PASSWORD_VALID_HOURS, MAX_TEMP_PASSWORD_VALID_HOURS));
        }
        jdbcTemplate.update(UPSERT_SETTINGS_SQL, enterprisePk, tempPasswordValidHours);
        audit.info("SETTINGS_CHANGED enterprisePk={} tempPasswordValidHours={}", enterprisePk, tempPasswordValidHours);
        return UserActionResult.succeeded("Settings saved.");
    }
}
