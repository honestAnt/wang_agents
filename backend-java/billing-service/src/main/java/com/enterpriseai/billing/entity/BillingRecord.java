package com.enterpriseai.billing.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "billing_records")
public class BillingRecord {
    @Id
    private String id;
    @Column(nullable = false, length = 36)
    private String tenantId;
    @Column(length = 36)
    private String userId;
    @Column(length = 36)
    private String agentId;
    @Column(nullable = false, length = 128)
    private String model;
    @Column(length = 36)
    private String traceId;
    private Integer promptTokens = 0;
    private Integer completionTokens = 0;
    private BigDecimal costUsd = BigDecimal.ZERO;
    @Column(length = 8)
    private String costCurrency = "USD";
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
