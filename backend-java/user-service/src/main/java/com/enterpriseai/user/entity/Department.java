package com.enterpriseai.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "departments")
public class Department {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(length = 36)
    private String parentId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String path;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
