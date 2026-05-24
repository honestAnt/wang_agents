package com.enterpriseai.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "models")
public class ModelEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 36)
    private String providerId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 256)
    private String displayName;

    @Column(nullable = false, length = 32)
    private String type = "chat";

    private Integer maxTokens = 4096;

    @Column(precision = 10, scale = 6)
    private BigDecimal inputPrice = BigDecimal.ZERO;

    @Column(precision = 10, scale = 6)
    private BigDecimal outputPrice = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String capabilities;

    @Column(nullable = false, length = 16)
    private String status = "active";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
