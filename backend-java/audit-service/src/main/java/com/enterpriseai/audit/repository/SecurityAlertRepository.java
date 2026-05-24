package com.enterpriseai.audit.repository;

import com.enterpriseai.audit.entity.SecurityAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, String> {

    List<SecurityAlert> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<SecurityAlert> findByTenantIdAndResolvedOrderByCreatedAtDesc(String tenantId, boolean resolved);

    List<SecurityAlert> findByTenantIdAndSeverityAndResolved(String tenantId, String severity, boolean resolved);

    long countByTenantIdAndResolved(String tenantId, boolean resolved);
}
