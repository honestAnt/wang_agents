package com.enterpriseai.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "agent_marketplace")
public class MarketplaceAgent {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String agentId;

    @Column(nullable = false, length = 36)
    private String publisherId;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String category = "general";

    @Column(length = 512)
    private String tags;

    @Column(length = 512)
    private String iconUrl;

    @Column(length = 512)
    private String bannerUrl;

    @Column(columnDefinition = "TEXT")
    private String readme;

    private Integer version = 1;

    private Integer installCount = 0;

    @Column(precision = 2, scale = 1)
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    private Integer ratingCount = 0;

    @Column(nullable = false, length = 16)
    private String status = "published";

    private Instant publishedAt;

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
