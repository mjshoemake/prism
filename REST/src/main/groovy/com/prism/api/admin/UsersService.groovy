package com.prism.api.admin

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import com.prism.api.common.utils.SecurityUtils

@Service
class UsersService {

    private final JdbcTemplate jdbcTemplate

    UsersService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    List<Map<String, Object>> listUsers() {
        def sql = 'SELECT user_id, enterprise_pk, email, username, is_active, is_temp_password, failed_login_count, locked_until, last_login_at, created_at, updated_at FROM "prism"."Users" ORDER BY created_at DESC'
        jdbcTemplate.queryForList(sql)
    }

    Map<String, Object> getUser(String userId) {
        def sql = 'SELECT user_id, enterprise_pk, email, username, is_active, is_temp_password, failed_login_count, locked_until, last_login_at, created_at, updated_at FROM "prism"."Users" WHERE user_id = ?'
        try {
            jdbcTemplate.queryForMap(sql, userId)
        } catch (EmptyResultDataAccessException e) {
            return null
        }
    }

    Map<String, Object> createUser(Long enterprisePk, String email, String username) {
        // Fetch enterprise temp password hash
        def eph = jdbcTemplate.queryForObject('SELECT temp_password_hash FROM "prism"."enterprises" WHERE enterprise_pk = ?', String, enterprisePk)
        if (!eph) {
            throw new IllegalStateException('Enterprise has no temp password configured')
        }
        String algo = 'bcrypt'
        def paramsJson = '{"cost":12}'
        def sql = '''
            INSERT INTO "prism"."Users" (enterprise_pk, email, username, password_hash, password_algo, password_params, is_temp_password)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            RETURNING user_id, enterprise_pk, email, username, is_active, is_temp_password, created_at, updated_at
        '''
        jdbcTemplate.queryForMap(sql, enterprisePk, email, username, eph, algo, paramsJson)
    }

    int updateUser(String userId, String email, String username, Boolean isActive) {
        def sql = 'UPDATE "prism"."Users" SET email = ?, username = ?, is_active = COALESCE(?, is_active) WHERE user_id = ?'
        jdbcTemplate.update(sql, email, username, isActive, userId)
    }

    int updatePassword(String userId, String rawPassword) {
        String algo = 'bcrypt'
        String hash = SecurityUtils.encryptPassword(rawPassword);
        def paramsJson = '{"cost":12}'
        def sql = 'UPDATE "prism"."Users" SET password_hash = ?, password_algo = ?, password_params = ?, password_updated_at = now(), is_temp_password = FALSE WHERE user_id = ?'
        jdbcTemplate.update(sql, hash, algo, paramsJson, userId)
    }

    int deleteUser(String userId) {
        def sql = 'DELETE FROM "prism"."Users" WHERE user_id = ?'
        jdbcTemplate.update(sql, userId)
    }

    Map<String, Object> authenticateUser(String email, String password) {
        println "UserService.authenticateUser(email: '" + email + "', password: '" + password + "') called."
        def sql = '''
            SELECT user_id, enterprise_pk, email, username, password_hash, password_algo, 
                   is_active, is_temp_password, failed_login_count, locked_until, last_login_at 
            FROM "prism"."Users" 
            WHERE email = ? AND is_active = TRUE
        '''
        try {
            def user = jdbcTemplate.queryForMap(sql, email)
            
            // Check if account is locked
            if (user.locked_until && user.locked_until > new Date()) {
                return [success: false, message: 'Account is temporarily locked. Please try again later.']
            }
            
            // Check if password hash exists
            if (!user.password_hash) {
                incrementFailedLoginCount(user.user_id.toString())
                return [success: false, message: 'Invalid email or password.']
            }
            
            // Verify password
            if (SecurityUtils.verifyPassword(password, user.password_hash)) {
                // Reset failed login count and update last login
                def updateSql = '''
                    UPDATE "prism"."Users" 
                    SET failed_login_count = 0, last_login_at = now() 
                    WHERE user_id = ?
                '''
                jdbcTemplate.update(updateSql, user.user_id)
                
                // Return user info (without password hash)
                user.remove('password_hash')
                user.remove('password_algo')
                return [success: true, user: user]
            } else {
                // Increment failed login count
                incrementFailedLoginCount(user.user_id.toString())
                return [success: false, message: 'Invalid email or password.']
            }
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return [success: false, message: 'Invalid email or password.']
        }
    }

    private void incrementFailedLoginCount(String userId) {
        def sql = '''
            UPDATE "prism"."Users" 
            SET failed_login_count = failed_login_count + 1,
                locked_until = CASE 
                    WHEN failed_login_count + 1 >= 5 THEN now() + INTERVAL '15 minutes'
                    ELSE locked_until
                END
            WHERE user_id = ?
        '''
        jdbcTemplate.update(sql, UUID.fromString(userId))
    }

    Map<String, Object> changePassword(String userId, String currentPassword, String newPassword) {
        // First verify current password
        def sql = 'SELECT password_hash FROM "prism"."Users" WHERE user_id = ? AND is_active = TRUE'
        try {
            def currentHash = jdbcTemplate.queryForObject(sql, String.class, userId)
            
            if (!SecurityUtils.verifyPassword(currentPassword, currentHash)) {
                return [success: false, message: 'Current password is incorrect.']
            }
            
            // Update password
            updatePassword(userId, newPassword)
            return [success: true, message: 'Password changed successfully.']
            
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return [success: false, message: 'User not found.']
        }
    }
}


