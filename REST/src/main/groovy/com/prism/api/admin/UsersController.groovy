package com.prism.api.admin

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping('/api/users')
class UsersController {

    private final UsersService svc

    UsersController(UsersService svc) {
        this.svc = svc
    }

    @GetMapping()
    ResponseEntity<List<Map<String, Object>>> listUsers() {
        ResponseEntity.ok(svc.listUsers())
    }

    @GetMapping('{userId}')
    ResponseEntity<Map<String, Object>> getUser(@PathVariable String userId) {
        def user = svc.getUser(userId)
        return user ? ResponseEntity.ok(user) : ResponseEntity.notFound().build()
    }

    static class CreateUserRequest {
        Long enterprise_pk
        String email
        String username
    }

    @PostMapping()
    ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest req) {
        if (!req?.enterprise_pk || !req?.email) {
            return ResponseEntity.badRequest().build()
        }
        def created = svc.createUser(req.enterprise_pk, req.email, req.username)
        ResponseEntity.ok(created)
    }

    static class UpdateUserRequest {
        String email
        String username
        Boolean isActive
    }

    @PutMapping('{userId}')
    ResponseEntity<Void> updateUser(@PathVariable String userId, @RequestBody UpdateUserRequest req) {
        svc.updateUser(userId, req?.email, req?.username, req?.isActive)
        ResponseEntity.noContent().build()
    }

    static class UpdatePasswordRequest {
        String password
    }

    @PutMapping('{userId}/password')
    ResponseEntity<Void> updatePassword(@PathVariable String userId, @RequestBody UpdatePasswordRequest req) {
        if (!req?.password) {
            return ResponseEntity.badRequest().build()
        }
        svc.updatePassword(userId, req.password)
        ResponseEntity.noContent().build()
    }

    static class ResetPasswordRequest {
        String newPassword
    }

    @PutMapping('{userId}/reset-password')
    ResponseEntity<Void> resetPassword(@PathVariable String userId, @RequestBody ResetPasswordRequest req) {
        if (!req?.newPassword) {
            return ResponseEntity.badRequest().build()
        }
        svc.updatePassword(userId, req.newPassword)
        ResponseEntity.noContent().build()
    }

    @DeleteMapping('{userId}')
    ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        svc.deleteUser(userId)
        ResponseEntity.noContent().build()
    }
}


