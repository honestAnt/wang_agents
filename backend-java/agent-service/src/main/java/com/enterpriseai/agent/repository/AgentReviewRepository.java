package com.enterpriseai.agent.repository;

import com.enterpriseai.agent.entity.AgentReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentReviewRepository extends JpaRepository<AgentReview, String> {

    List<AgentReview> findByMarketplaceId(String marketplaceId);

    Optional<AgentReview> findByMarketplaceIdAndUserId(String marketplaceId, String userId);

    long countByMarketplaceId(String marketplaceId);
}
