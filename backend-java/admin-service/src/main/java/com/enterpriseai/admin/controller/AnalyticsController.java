package com.enterpriseai.admin.controller;

import com.enterpriseai.admin.service.AnalyticsService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> usageSummary(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "today") String period) {
        return ApiResponse.ok(analyticsService.getUsageSummary(tenantId, period));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<Map<String, Object>>> trend(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "requests") String metric,
            @RequestParam(defaultValue = "week") String period) {
        return ApiResponse.ok(analyticsService.getTrend(tenantId, metric, period));
    }

    @GetMapping("/rankings")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> rankings(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "models") String category,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(analyticsService.getRankings(tenantId, category, limit));
    }

    @GetMapping("/quality")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> quality(@RequestParam String tenantId) {
        return ApiResponse.ok(analyticsService.getQualityMetrics(tenantId));
    }

    @GetMapping("/costs")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> costBreakdown(@RequestParam String tenantId) {
        return ApiResponse.ok(analyticsService.getCostBreakdown(tenantId));
    }

    @GetMapping("/report")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> report(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "weekly") String type) {
        return ApiResponse.ok(analyticsService.generateReport(tenantId, type));
    }
}
