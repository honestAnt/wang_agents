package com.enterpriseai.admin.controller;

import com.enterpriseai.admin.service.PlatformAdminService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/platform")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    public PlatformAdminController(PlatformAdminService platformAdminService) {
        this.platformAdminService = platformAdminService;
    }

    // ── Overview ─────────────────────────────────────────────

    @GetMapping("/overview")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> platformOverview() {
        return ApiResponse.ok(platformAdminService.getPlatformOverview());
    }

    // ── Tenant Management ────────────────────────────────────

    @GetMapping("/tenants")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<List<Map<String, Object>>> listTenants() {
        return ApiResponse.ok(platformAdminService.listTenants());
    }

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> tenantDetail(@PathVariable String tenantId) {
        return ApiResponse.ok(platformAdminService.getTenantDetail(tenantId));
    }

    // ── Rate Limiting ────────────────────────────────────────

    @GetMapping("/rate-limits")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> rateLimits() {
        return ApiResponse.ok(platformAdminService.getRateLimitConfig());
    }

    @PutMapping("/rate-limits")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> updateRateLimit(
            @RequestParam String scope,
            @RequestParam String key,
            @RequestParam int maxQps) {
        return ApiResponse.ok(platformAdminService.updateRateLimit(scope, key, maxQps));
    }

    // ── Circuit Breaker ──────────────────────────────────────

    @GetMapping("/circuit-breakers")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> circuitBreakers() {
        return ApiResponse.ok(platformAdminService.getCircuitBreakerStatus());
    }

    @PostMapping("/circuit-breakers/{provider}/reset")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> resetCircuitBreaker(@PathVariable String provider) {
        return ApiResponse.ok(platformAdminService.resetCircuitBreaker(provider));
    }

    // ── Announcements ────────────────────────────────────────

    @PostMapping("/announcements")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> createAnnouncement(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "info") String level,
            @RequestParam(defaultValue = "all") String targetTenants) {
        return ApiResponse.ok(platformAdminService.createAnnouncement(
                title, content, level, targetTenants));
    }

    @GetMapping("/announcements")
    @PreAuthorize("hasRole('SuperAdmin') or hasRole('TenantAdmin')")
    public ApiResponse<List<Map<String, Object>>> listAnnouncements() {
        return ApiResponse.ok(platformAdminService.listAnnouncements());
    }
}
