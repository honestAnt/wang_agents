package com.enterpriseai.agent.repository;

import com.enterpriseai.agent.entity.AgentInstall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentInstallRepository extends JpaRepository<AgentInstall, String> {

    List<AgentInstall> findByTenantId(String tenantId);

    List<AgentInstall> findByMarketplaceId(String marketplaceId);

    long countByMarketplaceId(String marketplaceId);

    boolean existsByMarketplaceIdAndTenantId(String marketplaceId, String tenantId);
}
