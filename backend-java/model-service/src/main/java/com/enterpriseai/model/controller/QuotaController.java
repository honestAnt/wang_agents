package com.enterpriseai.model.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.model.entity.ModelQuota;
import com.enterpriseai.model.service.QuotaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quotas")
public class QuotaController {

    private final QuotaService quotaService;

    public QuotaController(QuotaService quotaService) {
        this.quotaService = quotaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<List<ModelQuota>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(quotaService.listByTenant(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<ModelQuota> create(@RequestParam String tenantId,
                                           @RequestParam(required = false) String userId,
                                           @RequestParam(required = false) String agentId,
                                           @RequestParam(required = false) String modelId,
                                           @RequestParam(required = false) Long maxTokens,
                                           @RequestParam(required = false) Long maxRequests,
                                           @RequestParam(required = false) String period) {
        return ApiResponse.ok(
                quotaService.createOrUpdate(tenantId, userId, agentId, modelId,
                        maxTokens, maxRequests, period));
    }
}
