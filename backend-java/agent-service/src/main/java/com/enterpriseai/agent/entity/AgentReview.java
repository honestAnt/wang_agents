package com.enterpriseai.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "agent_reviews")
public class AgentReview {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String marketplaceId;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
