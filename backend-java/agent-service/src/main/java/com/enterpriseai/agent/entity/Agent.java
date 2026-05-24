package com.enterpriseai.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "agents")
public class Agent {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 512)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(length = 36)
    private String modelId;

    @Column(precision = 3, scale = 2)
    private BigDecimal temperature = new BigDecimal("0.7");

    private Integer maxTokens = 4096;

    @Column(length = 16)
    private String memoryStrategy = "short_term";

    @Column(length = 16)
    private String visibility = "tenant";

    @Column(nullable = false, length = 16)
    private String status = "draft";  // draft, test, published, archived

    private Integer version = 1;

    private Instant publishedAt;

    @Column(length = 36)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
