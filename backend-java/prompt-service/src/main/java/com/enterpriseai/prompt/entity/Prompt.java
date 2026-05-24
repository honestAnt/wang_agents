package com.enterpriseai.prompt.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "prompts")
public class Prompt {
    @Id
    private String id;
    @Column(nullable = false, length = 36)
    private String tenantId;
    @Column(nullable = false, length = 128)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 64)
    private String category = "general";
    @Column(columnDefinition = "TEXT")
    private String tags;
    private Integer currentVersion = 1;
    @Column(nullable = false, length = 16)
    private String status = "draft";
    @Column(length = 36)
    private String createdBy;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
