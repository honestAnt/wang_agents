package com.enterpriseai.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "platform_stats")
public class PlatformStats {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false)
    private Instant statDate;

    private Long activeUsers;
    private Long totalSessions;
    private Long totalRequests;
    private Long totalTokens;
    private BigDecimal totalCost;

    private Long toolCalls;
    private Long toolFailures;
    private Long ragQueries;
    private Long ragHits;

    private Double avgLatencyMs;
    private Double avgSatisfaction;

    private String topModel;
    private String topSkill;
    private String topTool;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
