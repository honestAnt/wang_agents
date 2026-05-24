package com.enterpriseai.audit.repository;

import com.enterpriseai.audit.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, String> {

    List<SecurityAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<SecurityAuditLog> findByTenantIdAndEventType(String tenantId, String eventType);

    List<SecurityAuditLog> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, String userId);

    List<SecurityAuditLog> findByTenantIdAndCreatedAtAfterOrderByCreatedAtDesc(String tenantId, Instant since);

    List<SecurityAuditLog> findByTenantIdAndSeverity(String tenantId, String severity);

    long countByTenantIdAndCreatedAtAfter(String tenantId, Instant since);
}
