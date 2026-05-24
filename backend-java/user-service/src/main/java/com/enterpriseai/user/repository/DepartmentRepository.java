package com.enterpriseai.user.repository;

import com.enterpriseai.user.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    List<Department> findByTenantIdOrderBySortOrder(String tenantId);

    List<Department> findByTenantIdAndParentId(String tenantId, String parentId);
}
