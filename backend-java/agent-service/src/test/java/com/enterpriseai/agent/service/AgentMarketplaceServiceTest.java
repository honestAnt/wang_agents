package com.enterpriseai.agent.service;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.entity.AgentInstall;
import com.enterpriseai.agent.entity.AgentReview;
import com.enterpriseai.agent.entity.MarketplaceAgent;
import com.enterpriseai.agent.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentMarketplaceServiceTest {

    @Mock
    private MarketplaceAgentRepository marketplaceAgentRepository;

    @Mock
    private AgentReviewRepository agentReviewRepository;

    @Mock
    private AgentInstallRepository agentInstallRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AgentService agentService;

    @InjectMocks
    private AgentMarketplaceService marketplaceService;

    // ── Publish ──────────────────────────────────────────────

    @Test
    void publish_shouldCreateMarketplaceEntry() {
        Agent agent = new Agent();
        agent.setId("a1");
        agent.setTenantId("t1");
        agent.setStatus("published");
        agent.setVersion(1);
        when(agentService.getById("a1")).thenReturn(agent);
        when(marketplaceAgentRepository.findByAgentId("a1")).thenReturn(List.of());
        when(marketplaceAgentRepository.save(any(MarketplaceAgent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MarketplaceAgent result = marketplaceService.publish(
                "a1", "u1", "analytics", "data,reports", "# My Agent");

        assertEquals("a1", result.getAgentId());
        assertEquals("u1", result.getPublisherId());
        assertEquals("analytics", result.getCategory());
        assertEquals("data,reports", result.getTags());
        assertEquals("published", result.getStatus());
    }

    @Test
    void publish_shouldRejectNonPublishedAgent() {
        Agent agent = new Agent();
        agent.setId("a1");
        agent.setStatus("draft");
        when(agentService.getById("a1")).thenReturn(agent);

        assertThrows(IllegalStateException.class, () ->
                marketplaceService.publish("a1", "u1", "general", "", ""));
    }

    @Test
    void publish_shouldRejectDuplicate() {
        Agent agent = new Agent();
        agent.setId("a1");
        agent.setStatus("published");
        when(agentService.getById("a1")).thenReturn(agent);
        when(marketplaceAgentRepository.findByAgentId("a1"))
                .thenReturn(List.of(new MarketplaceAgent()));

        assertThrows(IllegalStateException.class, () ->
                marketplaceService.publish("a1", "u1", "general", "", ""));
    }

    // ── Discovery ────────────────────────────────────────────

    @Test
    void list_shouldReturnPublishedByDefault() {
        when(marketplaceAgentRepository.findByStatus("published"))
                .thenReturn(List.of(new MarketplaceAgent(), new MarketplaceAgent()));

        List<MarketplaceAgent> result = marketplaceService.list(null, null);

        assertEquals(2, result.size());
    }

    @Test
    void list_shouldFilterByCategory() {
        when(marketplaceAgentRepository.findByCategoryAndStatus("analytics", "published"))
                .thenReturn(List.of(new MarketplaceAgent()));

        List<MarketplaceAgent> result = marketplaceService.list("analytics", null);

        assertEquals(1, result.size());
    }

    @Test
    void list_shouldSearchByKeyword() {
        when(marketplaceAgentRepository.searchByKeyword("data", "published"))
                .thenReturn(List.of(new MarketplaceAgent()));

        List<MarketplaceAgent> result = marketplaceService.list(null, "data");

        assertEquals(1, result.size());
    }

    // ── Install ──────────────────────────────────────────────

    @Test
    void install_shouldCloneAgentAndRecord() {
        MarketplaceAgent mp = new MarketplaceAgent();
        mp.setId("mp1");
        mp.setAgentId("a1");
        mp.setInstallCount(5);

        Agent sourceAgent = new Agent();
        sourceAgent.setId("a1");
        sourceAgent.setName("Test Agent");
        sourceAgent.setDescription("Desc");
        sourceAgent.setSystemPrompt("Prompt");
        sourceAgent.setModelId("m1");

        when(marketplaceAgentRepository.findById("mp1")).thenReturn(Optional.of(mp));
        when(agentService.getById("a1")).thenReturn(sourceAgent);
        when(agentService.create(any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    Agent cloned = new Agent();
                    cloned.setId("cloned-1");
                    cloned.setName(inv.getArgument(1));
                    return cloned;
                });
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentInstallRepository.save(any(AgentInstall.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(marketplaceAgentRepository.save(any(MarketplaceAgent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentInstall result = marketplaceService.install("mp1", "t2", "u1");

        assertEquals("mp1", result.getMarketplaceId());
        assertEquals("t2", result.getTenantId());
        assertEquals("u1", result.getUserId());
        assertEquals("a1", result.getSourceAgentId());
        assertEquals("cloned-1", result.getClonedAgentId());
        assertEquals("installed", result.getStatus());
    }

    // ── Reviews ──────────────────────────────────────────────

    @Test
    void addReview_shouldSaveAndRecalculate() {
        MarketplaceAgent mp = new MarketplaceAgent();
        mp.setId("mp1");
        mp.setRatingAvg(java.math.BigDecimal.ZERO);
        mp.setRatingCount(0);

        when(marketplaceAgentRepository.findById("mp1")).thenReturn(Optional.of(mp));
        when(agentReviewRepository.findByMarketplaceIdAndUserId("mp1", "u1"))
                .thenReturn(Optional.empty());
        when(agentReviewRepository.save(any(AgentReview.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // Stub a non-empty review list so recalculateRating updates the mp entity
        AgentReview existingReview = new AgentReview();
        existingReview.setId("r1");
        existingReview.setRating(5);
        when(agentReviewRepository.findByMarketplaceId("mp1"))
                .thenReturn(List.of(existingReview));
        when(marketplaceAgentRepository.save(any(MarketplaceAgent.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AgentReview result = marketplaceService.addReview("mp1", "u1", 4, "Great!");

        assertEquals(4, result.getRating());
        assertEquals("Great!", result.getComment());
        assertEquals("u1", result.getUserId());
        verify(marketplaceAgentRepository).save(any(MarketplaceAgent.class));
    }
}
