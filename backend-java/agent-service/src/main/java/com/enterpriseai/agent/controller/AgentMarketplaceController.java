package com.enterpriseai.agent.controller;

import com.enterpriseai.agent.entity.AgentInstall;
import com.enterpriseai.agent.entity.AgentReview;
import com.enterpriseai.agent.entity.MarketplaceAgent;
import com.enterpriseai.agent.service.AgentMarketplaceService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class AgentMarketplaceController {

    private final AgentMarketplaceService marketplaceService;

    public AgentMarketplaceController(AgentMarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    // ── Publish / Unpublish ──────────────────────────────────

    @PostMapping("/publish")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<MarketplaceAgent> publish(@RequestParam String agentId,
                                                  @RequestParam String publisherId,
                                                  @RequestParam(required = false, defaultValue = "general") String category,
                                                  @RequestParam(required = false) String tags,
                                                  @RequestParam(required = false) String readme) {
        return ApiResponse.ok(marketplaceService.publish(agentId, publisherId, category, tags, readme));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Void> unpublish(@PathVariable String id) {
        marketplaceService.unpublish(id);
        return ApiResponse.ok(null);
    }

    // ── Discovery ────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<MarketplaceAgent>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.ok(marketplaceService.list(category, keyword));
    }

    @GetMapping("/popular")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<MarketplaceAgent>> popular(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(marketplaceService.popular(limit));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<MarketplaceAgent> get(@PathVariable String id) {
        return ApiResponse.ok(marketplaceService.getById(id));
    }

    // ── Install ──────────────────────────────────────────────

    @PostMapping("/{id}/install")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<AgentInstall> install(@PathVariable String id,
                                              @RequestParam String tenantId,
                                              @RequestParam String userId) {
        return ApiResponse.ok(marketplaceService.install(id, tenantId, userId));
    }

    @GetMapping("/installs")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<AgentInstall>> listInstalls(@RequestParam String tenantId) {
        return ApiResponse.ok(marketplaceService.listInstalls(tenantId));
    }

    // ── Reviews ──────────────────────────────────────────────

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<AgentReview> addReview(@PathVariable String id,
                                               @RequestParam String userId,
                                               @RequestParam int rating,
                                               @RequestParam(required = false) String comment) {
        return ApiResponse.ok(marketplaceService.addReview(id, userId, rating, comment));
    }

    @GetMapping("/{id}/reviews")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<AgentReview>> listReviews(@PathVariable String id) {
        return ApiResponse.ok(marketplaceService.listReviews(id));
    }
}
