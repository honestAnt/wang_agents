package com.enterpriseai.memory.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "memory_entries")
public class MemoryEntry {
    @Id
    private String id;
    @Column(nullable = false, length = 36)
    private String tenantId;
    @Column(nullable = false, length = 36)
    private String userId;
    @Column(length = 36)
    private String sessionId;
    @Column(nullable = false, length = 16)
    private String type;  // episodic, semantic, procedural
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    private BigDecimal importance = new BigDecimal("0.5");
    private BigDecimal decayRate = new BigDecimal("0.01");
    private Integer accessCount = 0;
    private Instant lastAccessed;
    @Column(columnDefinition = "TEXT")
    private String metadataJson;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
