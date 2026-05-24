package com.enterpriseai.rag.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "documents")
public class Document {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String kbId;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 512)
    private String title;

    @Column(nullable = false, length = 16)
    private String fileType;

    @Column(length = 1024)
    private String fileUrl;

    private Long fileSize;

    @Column(nullable = false, length = 16)
    private String status = "uploading";

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
