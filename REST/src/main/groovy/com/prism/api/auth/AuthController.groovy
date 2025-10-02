package com.prism.api.auth

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.prism.api.admin.UsersService

@RestController
@RequestMapping('/api/auth')
class AuthController {

    private final UsersService usersService

    AuthController(UsersService usersService) {
        this.usersService = usersService
    }

    static class LoginRequest {
        String email
        String password
    }

    @PostMapping('/login')
    ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (!request?.email || !request?.password) {
            return ResponseEntity.badRequest().body([
                success: false, 
                message: 'Email and password are required.'
            ])
        }

        def result = usersService.authenticateUser(request.email, request.password)
        
        if (result.success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.status(401).body(result)
        }
    }

    static class ChangePasswordRequest {
        String currentPassword
        String newPassword
    }

    @PostMapping('/change-password')
    ResponseEntity<Map<String, Object>> changePassword(
            @RequestHeader('X-User-ID') String userId,
            @RequestBody ChangePasswordRequest request) {
        
        if (!request?.currentPassword || !request?.newPassword) {
            return ResponseEntity.badRequest().body([
                success: false, 
                message: 'Current password and new password are required.'
            ])
        }

        if (request.newPassword.length() < 8) {
            return ResponseEntity.badRequest().body([
                success: false, 
                message: 'New password must be at least 8 characters long.'
            ])
        }

        def result = usersService.changePassword(userId, request.currentPassword, request.newPassword)
        
        if (result.success) {
            return ResponseEntity.ok(result)
        } else {
            return ResponseEntity.status(400).body(result)
        }
    }
}
