package com.prism.api.common.utils

import org.springframework.security.crypto.bcrypt.BCrypt

class SecurityUtils {

    static String encryptPassword(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12))
    }

    static boolean verifyPassword(String rawPassword, String hashedPassword) {
        println "SecurityUtils(rawPassword: '" + rawPassword + "', hashedPassword: '" + hashedPassword + "') called."
        if (!rawPassword || !hashedPassword) {
            return false
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword)
        } catch (Exception e) {
            return false
        }
    }

}
