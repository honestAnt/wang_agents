package com.enterpriseai.user.repository;

import com.enterpriseai.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, String> {

    List<UserRole> findByUserIdAndTenantId(String userId, String tenantId);

    void deleteByUserIdAndTenantIdAndRole(String userId, String tenantId, String role);
}
