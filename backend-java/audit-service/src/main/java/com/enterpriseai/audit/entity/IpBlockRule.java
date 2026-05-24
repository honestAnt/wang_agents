package com.enterpriseai.audit.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "ip_block_rules")
public class IpBlockRule {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String ipPattern;  // IP or CIDR range

    @Column(length = 32)
    private String blockType;  // permanent, temporary, rate_limit

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Instant expiresAt;

    @Column(length = 36)
    private String createdBy;

    private Boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
