package com.enterpriseai.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "security_alerts")
public class SecurityAlert {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 32)
    private String alertType;  // prompt_injection, privilege_escalation, data_exfiltration, anomaly

    @Column(nullable = false, length = 16)
    private String severity;  // low, medium, high, critical

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 36)
    private String sourceUserId;

    @Column(columnDefinition = "TEXT")
    private String rawPrompt;

    @Column(columnDefinition = "TEXT")
    private String matchedPatterns;

    private Boolean resolved = false;
    private String resolvedBy;
    private Instant resolvedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
