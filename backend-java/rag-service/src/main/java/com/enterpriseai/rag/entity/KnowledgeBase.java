package com.enterpriseai.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "knowledge_bases")
public class KnowledgeBase {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 256)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 128)
    private String embeddingModel = "text-embedding-3-small";

    private Integer embeddingDim = 1536;

    private Integer chunkSize = 512;

    private Integer chunkOverlap = 50;

    @Column(length = 32)
    private String chunkStrategy = "recursive";

    @Column(length = 32)
    private String indexType = "hybrid";

    @Column(nullable = false, length = 16)
    private String status = "active";

    private Integer docCount = 0;

    private Long chunkCount = 0L;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
