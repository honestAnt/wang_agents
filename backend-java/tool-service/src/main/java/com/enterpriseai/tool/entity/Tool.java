package com.enterpriseai.tool.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "tools")
public class Tool {

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

    @Column(nullable = false, length = 16)
    private String toolType;  // http, mcp, sdk

    @Column(columnDefinition = "TEXT")
    private String schemaJson;

    @Column(length = 1024)
    private String endpointUrl;

    @Column(length = 8)
    private String method = "POST";

    private Integer timeoutMs = 30000;

    private Integer retryCount = 0;

    @Column(length = 16)
    private String retryBackoff = "fixed";

    @Column(nullable = false, length = 16)
    private String status = "active";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
