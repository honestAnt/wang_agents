package com.enterpriseai.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "security_audit_logs")
public class SecurityAuditLog {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(length = 36)
    private String userId;

    @Column(nullable = false, length = 32)
    private String eventType;  // prompt_injection, data_access, tool_call, login, permission_change, model_call

    @Column(nullable = false, length = 16)
    private String severity;  // info, warning, critical

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 64)
    private String sourceIp;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 256)
    private String resource;  // What was accessed: agent_id, tool_id, kb_id

    @Column(length = 16)
    private String action;  // create, read, update, delete, execute

    @Column(columnDefinition = "TEXT")
    private String details;  // JSON with extra context

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
