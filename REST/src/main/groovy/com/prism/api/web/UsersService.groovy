package com.prism.api.web

import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Service
import com.prism.api.common.utils.SecurityUtils

@Service
class UsersService {

    private final JdbcTemplate jdbcTemplate

    UsersService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate
    }

    List<Map<String, Object>> listUsers() {
        def sql = 'SELECT user_id, enterprise_pk, email, username, is_active, is_temp_password, failed_login_count, locked_until, last_login_at, created_at, updated_at FROM "prism"."users" ORDER BY created_at DESC'
        jdbcTemplate.queryForList(sql)
    }

    Map<String, Object> getUser(String userId) {
        def sql = 'SELECT user_id, enterprise_pk, email, username, is_active, is_temp_password, failed_login_count, locked_until, last_login_at, created_at, updated_at FROM "prism"."users" WHERE user_id = ?'
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
            INSERT INTO "prism"."users" (enterprise_pk, email, username, password_hash, password_algo, password_params, is_temp_password)
            VALUES (?, ?, ?, ?, ?, ?, TRUE)
            RETURNING user_id, enterprise_pk, email, username, is_active, is_temp_password, created_at, updated_at
        '''
        jdbcTemplate.queryForMap(sql, enterprisePk, email, username, eph, algo, paramsJson)
    }

    int updateUser(String userId, String email, String username, Boolean isActive) {
        def sql = 'UPDATE "prism"."users" SET email = ?, username = ?, is_active = COALESCE(?, is_active) WHERE user_id = ?'
        jdbcTemplate.update(sql, email, username, isActive, userId)
    }

    int updatePassword(String userId, String rawPassword) {
        String algo = 'bcrypt'
        String hash = SecurityUtils.encryptPassword(rawPassword);
        def paramsJson = '{"cost":12}'
        def sql = 'UPDATE "prism"."users" SET password_hash = ?, password_algo = ?, password_params = ?, password_updated_at = now(), is_temp_password = FALSE WHERE user_id = ?'
        jdbcTemplate.update(sql, hash, algo, paramsJson, userId)
    }

    int deleteUser(String userId) {
        def sql = 'DELETE FROM "prism"."users" WHERE user_id = ?'
        jdbcTemplate.update(sql, userId)
    }
}


