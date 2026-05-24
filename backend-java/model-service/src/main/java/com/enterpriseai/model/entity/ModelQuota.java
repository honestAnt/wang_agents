package com.enterpriseai.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "quotas")
public class ModelQuota {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(length = 36)
    private String userId;

    @Column(length = 36)
    private String agentId;

    @Column(length = 36)
    private String modelId;

    @Column(nullable = false)
    private Long maxTokens = 0L;

    @Column(nullable = false)
    private Long maxRequests = 0L;

    @Column(nullable = false, length = 16)
    private String period = "monthly";

    private Long currentTokens = 0L;

    private Long currentRequests = 0L;

    private Instant resetAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
