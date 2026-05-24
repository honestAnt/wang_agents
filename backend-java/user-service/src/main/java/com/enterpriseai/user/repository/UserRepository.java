package com.enterpriseai.user.repository;

import com.enterpriseai.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByTenantId(String tenantId);

    List<User> findByTenantIdAndDepartmentId(String tenantId, String departmentId);
}
