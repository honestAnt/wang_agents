package com.enterpriseai.agent.repository;

import com.enterpriseai.agent.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findByTenantId(String tenantId);

    List<Agent> findByTenantIdAndStatus(String tenantId, String status);
}
