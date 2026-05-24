package com.enterpriseai.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(length = 36)
    private String departmentId;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(length = 256)
    private String email;

    @Column(length = 128)
    private String displayName;

    @Column(length = 32)
    private String phone;

    @Column(length = 512)
    private String avatarUrl;

    @Column(nullable = false, length = 16)
    private String status = "active";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
