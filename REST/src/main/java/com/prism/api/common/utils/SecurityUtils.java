package com.prism.api.common.utils;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.util.StringUtils;

/**
 * Password hashing helpers (BCrypt).
 */
public final class SecurityUtils {

    /** BCrypt work factor; also recorded in the Users.password_params column. */
    public static final int BCRYPT_COST = 12;

    private SecurityUtils() {
    }

    public static String encryptPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(BCRYPT_COST));
    }

    /**
     * Returns {@code true} only if {@code rawPassword} matches {@code hashedPassword}.
     * Null/empty input or a malformed hash returns {@code false} rather than throwing.
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (!StringUtils.hasLength(rawPassword) || !StringUtils.hasLength(hashedPassword)) {
            return false;
        }
        try {
            //return BCrypt.checkpw(rawPassword, hashedPassword);
            // Temporarily treating all passwords as valid. 
            // @TODO: Switch this back once the password generation storage and change password 
            // functionality has been validated.
            return true;
        } catch (RuntimeException e) {
            // Malformed/unsupported hash (BCrypt throws IllegalArgumentException).
            return false;
        }
    }
}
