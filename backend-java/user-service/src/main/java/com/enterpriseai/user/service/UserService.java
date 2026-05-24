package com.enterpriseai.user.service;

import com.enterpriseai.user.entity.User;
import com.enterpriseai.user.entity.UserRole;
import com.enterpriseai.user.repository.UserRepository;
import com.enterpriseai.user.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }

    public List<User> listByTenant(String tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    public User getById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    public User create(String tenantId, String username, String email,
                       String displayName, String departmentId) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setTenantId(tenantId);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setDepartmentId(departmentId);
        return userRepository.save(user);
    }

    public List<UserRole> getUserRoles(String userId, String tenantId) {
        return userRoleRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    public UserRole addRole(String userId, String tenantId, String role, String grantedBy) {
        UserRole userRole = new UserRole();
        userRole.setId(UUID.randomUUID().toString());
        userRole.setUserId(userId);
        userRole.setTenantId(tenantId);
        userRole.setRole(role);
        userRole.setGrantedBy(grantedBy);
        return userRoleRepository.save(userRole);
    }

    public void removeRole(String userId, String tenantId, String role) {
        userRoleRepository.deleteByUserIdAndTenantIdAndRole(userId, tenantId, role);
    }
}
