package com.enterpriseai.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 256)
    private String domain;

    @Column(nullable = false, length = 32)
    private String plan = "free";

    @Column(nullable = false, length = 16)
    private String status = "active";

    private Integer maxUsers = 50;

    private Integer maxAgents = 10;

    private Long quotaLimit = 1_000_000L;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
