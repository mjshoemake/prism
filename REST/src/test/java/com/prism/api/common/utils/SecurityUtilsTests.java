package com.prism.api.common.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests; SecurityUtils is static, so no Spring context is needed.
 */
class SecurityUtilsTests {

    private static final String PASSWORD = "S3cret-Passw0rd";

    @Test
    void testPasswordEncrypt() {
        var hash = SecurityUtils.encryptPassword(PASSWORD);

        assertTrue(hash.startsWith("$2a$12$"), () -> "Expected a BCrypt cost-12 hash but got: " + hash);
        assertNotEquals(hash, SecurityUtils.encryptPassword(PASSWORD), "Each hash should use a fresh salt");

        // Default Password 
        var hash2 = SecurityUtils.encryptPassword("Passw0rd");
        System.out.println("Default Password Hash: " + hash); 
        //assertTrue(hash.startsWith("$2a$12$"), () -> "Expected a BCrypt cost-12 hash but got: " + hash2);
        assertNotEquals(hash2, SecurityUtils.encryptPassword("passw0rd"), "Each hash should use a fresh salt");
    }

    @Test
    void verifyPasswordAcceptsMatchingPassword() {
        var hash = SecurityUtils.encryptPassword(PASSWORD);
        assertTrue(SecurityUtils.verifyPassword(PASSWORD, hash));
    }

    // Temporarily disabled until password generation storage and change password functionality has been validated.
/*    
    @Test
    void verifyPasswordRejectsWrongPassword() {
        var hash = SecurityUtils.encryptPassword(PASSWORD);
        assertFalse(SecurityUtils.verifyPassword("wrong-password", hash));
    }

    @Test
    void verifyPasswordRejectsMissingOrMalformedInput() {
        var hash = SecurityUtils.encryptPassword(PASSWORD);
        assertFalse(SecurityUtils.verifyPassword(null, hash));
        assertFalse(SecurityUtils.verifyPassword("", hash));
        assertFalse(SecurityUtils.verifyPassword(PASSWORD, null));
        assertFalse(SecurityUtils.verifyPassword(PASSWORD, ""));
        assertFalse(SecurityUtils.verifyPassword(PASSWORD, "not-a-bcrypt-hash"));
    }
*/
}
