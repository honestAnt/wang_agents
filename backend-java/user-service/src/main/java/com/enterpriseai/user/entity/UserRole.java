package com.enterpriseai.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "user_roles")
public class UserRole {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 64)
    private String role;

    @Column(length = 36)
    private String grantedBy;

    @Column(nullable = false)
    private Instant grantedAt = Instant.now();
}
