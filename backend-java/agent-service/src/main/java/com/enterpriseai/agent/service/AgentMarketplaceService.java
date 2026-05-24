package com.enterpriseai.agent.service;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.entity.AgentInstall;
import com.enterpriseai.agent.entity.AgentReview;
import com.enterpriseai.agent.entity.MarketplaceAgent;
import com.enterpriseai.agent.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AgentMarketplaceService {

    private final MarketplaceAgentRepository marketplaceAgentRepository;
    private final AgentReviewRepository agentReviewRepository;
    private final AgentInstallRepository agentInstallRepository;
    private final AgentRepository agentRepository;
    private final AgentService agentService;

    public AgentMarketplaceService(MarketplaceAgentRepository marketplaceAgentRepository,
                                    AgentReviewRepository agentReviewRepository,
                                    AgentInstallRepository agentInstallRepository,
                                    AgentRepository agentRepository,
                                    AgentService agentService) {
        this.marketplaceAgentRepository = marketplaceAgentRepository;
        this.agentReviewRepository = agentReviewRepository;
        this.agentInstallRepository = agentInstallRepository;
        this.agentRepository = agentRepository;
        this.agentService = agentService;
    }

    // ── Publish ──────────────────────────────────────────────

    @Transactional
    public MarketplaceAgent publish(String agentId, String publisherId,
                                     String category, String tags, String readme) {
        Agent agent = agentService.getById(agentId);
        if (!"published".equals(agent.getStatus())) {
            throw new IllegalStateException("Agent must be published before adding to marketplace");
        }

        // Check not already listed
        List<MarketplaceAgent> existing = marketplaceAgentRepository.findByAgentId(agentId);
        if (!existing.isEmpty()) {
            throw new IllegalStateException("Agent is already in the marketplace");
        }

        MarketplaceAgent mp = new MarketplaceAgent();
        mp.setId(UUID.randomUUID().toString());
        mp.setAgentId(agentId);
        mp.setPublisherId(publisherId);
        mp.setTenantId(agent.getTenantId());
        mp.setCategory(category != null ? category : "general");
        mp.setTags(tags != null ? tags : "");
        mp.setReadme(readme);
        mp.setVersion(agent.getVersion());
        mp.setStatus("published");
        mp.setPublishedAt(Instant.now());
        mp.setUpdatedAt(Instant.now());

        return marketplaceAgentRepository.save(mp);
    }

    @Transactional
    public void unpublish(String marketplaceId) {
        MarketplaceAgent mp = marketplaceAgentRepository.findById(marketplaceId)
                .orElseThrow(() -> new RuntimeException("Marketplace entry not found: " + marketplaceId));
        mp.setStatus("unpublished");
        mp.setUpdatedAt(Instant.now());
        marketplaceAgentRepository.save(mp);
    }

    // ── Discovery ────────────────────────────────────────────

    public List<MarketplaceAgent> list(String category, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return marketplaceAgentRepository.searchByKeyword(keyword.trim(), "published");
        }
        if (category != null && !category.isBlank()) {
            return marketplaceAgentRepository.findByCategoryAndStatus(category, "published");
        }
        return marketplaceAgentRepository.findByStatus("published");
    }

    public List<MarketplaceAgent> popular(int limit) {
        List<MarketplaceAgent> results = marketplaceAgentRepository.findPopular("published");
        return results.size() > limit ? results.subList(0, limit) : results;
    }

    public MarketplaceAgent getById(String marketplaceId) {
        return marketplaceAgentRepository.findById(marketplaceId)
                .orElseThrow(() -> new RuntimeException("Marketplace entry not found: " + marketplaceId));
    }

    // ── Install ──────────────────────────────────────────────

    @Transactional
    public AgentInstall install(String marketplaceId, String tenantId, String userId) {
        MarketplaceAgent mp = getById(marketplaceId);
        Agent sourceAgent = agentService.getById(mp.getAgentId());

        // Clone the agent into the tenant's workspace
        Agent cloned = agentService.create(
                tenantId,
                sourceAgent.getName() + " (from Marketplace)",
                sourceAgent.getDescription(),
                sourceAgent.getSystemPrompt(),
                sourceAgent.getModelId()
        );
        cloned.setTemperature(sourceAgent.getTemperature());
        cloned.setMaxTokens(sourceAgent.getMaxTokens());
        cloned.setMemoryStrategy(sourceAgent.getMemoryStrategy());
        cloned.setStatus("draft");
        agentRepository.save(cloned);

        // Record install
        AgentInstall install = new AgentInstall();
        install.setId(UUID.randomUUID().toString());
        install.setMarketplaceId(marketplaceId);
        install.setTenantId(tenantId);
        install.setUserId(userId);
        install.setSourceAgentId(mp.getAgentId());
        install.setClonedAgentId(cloned.getId());
        install.setStatus("installed");
        install.setInstalledAt(Instant.now());
        AgentInstall saved = agentInstallRepository.save(install);

        // Update install count
        mp.setInstallCount(mp.getInstallCount() + 1);
        mp.setUpdatedAt(Instant.now());
        marketplaceAgentRepository.save(mp);

        return saved;
    }

    public List<AgentInstall> listInstalls(String tenantId) {
        return agentInstallRepository.findByTenantId(tenantId);
    }

    // ── Reviews ──────────────────────────────────────────────

    @Transactional
    public AgentReview addReview(String marketplaceId, String userId, int rating, String comment) {
        MarketplaceAgent mp = getById(marketplaceId);

        // Upsert: one review per user per marketplace entry
        AgentReview review = agentReviewRepository
                .findByMarketplaceIdAndUserId(marketplaceId, userId)
                .orElseGet(() -> {
                    AgentReview r = new AgentReview();
                    r.setId(UUID.randomUUID().toString());
                    r.setMarketplaceId(marketplaceId);
                    r.setUserId(userId);
                    return r;
                });

        review.setRating(rating);
        review.setComment(comment);
        AgentReview saved = agentReviewRepository.save(review);

        // Recalculate average rating
        recalculateRating(marketplaceId);

        return saved;
    }

    public List<AgentReview> listReviews(String marketplaceId) {
        return agentReviewRepository.findByMarketplaceId(marketplaceId);
    }

    private void recalculateRating(String marketplaceId) {
        List<AgentReview> reviews = agentReviewRepository.findByMarketplaceId(marketplaceId);
        if (reviews.isEmpty()) return;

        double avg = reviews.stream()
                .mapToInt(AgentReview::getRating)
                .average()
                .orElse(0.0);

        MarketplaceAgent mp = getById(marketplaceId);
        mp.setRatingAvg(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        mp.setRatingCount(reviews.size());
        mp.setUpdatedAt(Instant.now());
        marketplaceAgentRepository.save(mp);
    }
}
