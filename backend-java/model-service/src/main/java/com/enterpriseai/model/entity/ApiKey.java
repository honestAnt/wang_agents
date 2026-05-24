package com.enterpriseai.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 36)
    private String providerId;

    @Column(nullable = false, length = 128)
    private String keyName;

    @Column(nullable = false, length = 512)
    private String encryptedKey;

    @Column(nullable = false, length = 16)
    private String status = "active";

    private Instant lastUsedAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
