package com.enterpriseai.skill.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 256)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 64)
    private String category = "general";

    @Column(length = 512)
    private String iconUrl;

    @Column(nullable = false, length = 16)
    private String status = "draft";

    private Integer version = 1;

    @Column(columnDefinition = "TEXT")
    private String promptTemplate;

    @Column(columnDefinition = "TEXT")
    private String inputSchema;

    @Column(columnDefinition = "TEXT")
    private String outputSchema;

    private BigDecimal price = BigDecimal.ZERO;

    private Integer downloadCount = 0;

    private BigDecimal rating = BigDecimal.ZERO;

    @Column(length = 36)
    private String createdBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
