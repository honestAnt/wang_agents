package com.enterpriseai.agent.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Data
@Entity
@Table(name = "agent_installs")
public class AgentInstall {

    @Id
    private String id;

    @Column(nullable = false, length = 36)
    private String marketplaceId;

    @Column(nullable = false, length = 36)
    private String tenantId;

    @Column(nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 36)
    private String sourceAgentId;

    @Column(length = 36)
    private String clonedAgentId;

    @Column(nullable = false, length = 16)
    private String status = "installed";

    @Column(nullable = false)
    private Instant installedAt = Instant.now();
}
