package com.enterpriseai.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "operational_alerts")
public class OperationalAlert {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 32)
    private String alertType;  // budget_exceeded, error_spike, latency_spike, model_failure

    @Column(nullable = false, length = 16)
    private String severity;  // info, warning, critical

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Boolean resolved = false;

    private Instant resolvedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
