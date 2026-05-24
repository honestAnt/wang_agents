package com.enterpriseai.agent.repository;

import com.enterpriseai.agent.entity.MarketplaceAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketplaceAgentRepository extends JpaRepository<MarketplaceAgent, String> {

    List<MarketplaceAgent> findByStatus(String status);

    List<MarketplaceAgent> findByCategoryAndStatus(String category, String status);

    List<MarketplaceAgent> findByTenantIdAndStatus(String tenantId, String status);

    List<MarketplaceAgent> findByAgentId(String agentId);

    @Query("SELECT m FROM MarketplaceAgent m WHERE m.status = :status AND " +
           "(LOWER(m.tags) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.category) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MarketplaceAgent> searchByKeyword(@Param("keyword") String keyword,
                                           @Param("status") String status);

    @Query("SELECT m FROM MarketplaceAgent m WHERE m.status = :status ORDER BY m.installCount DESC")
    List<MarketplaceAgent> findPopular(@Param("status") String status);
}
