package com.enterpriseai.user.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.user.entity.User;
import com.enterpriseai.user.entity.UserRole;
import com.enterpriseai.user.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<User>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(userService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<User> get(@PathVariable String id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<User> create(@RequestParam String tenantId,
                                     @RequestParam String username,
                                     @RequestParam(required = false) String email,
                                     @RequestParam(required = false) String displayName,
                                     @RequestParam(required = false) String departmentId) {
        return ApiResponse.ok(
                userService.create(tenantId, username, email, displayName, departmentId));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<UserRole>> getRoles(@PathVariable String id,
                                                  @RequestParam String tenantId) {
        return ApiResponse.ok(userService.getUserRoles(id, tenantId));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<UserRole> addRole(@PathVariable String id,
                                          @RequestParam String tenantId,
                                          @RequestParam String role,
                                          @RequestParam(required = false) String grantedBy) {
        return ApiResponse.ok(userService.addRole(id, tenantId, role, grantedBy));
    }

    @DeleteMapping("/{id}/roles")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Void> removeRole(@PathVariable String id,
                                         @RequestParam String tenantId,
                                         @RequestParam String role) {
        userService.removeRole(id, tenantId, role);
        return ApiResponse.ok(null);
    }
}
