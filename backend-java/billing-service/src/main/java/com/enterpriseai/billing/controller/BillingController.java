package com.enterpriseai.billing.controller;

import com.enterpriseai.billing.entity.BillingRecord;
import com.enterpriseai.billing.service.BillingService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<List<BillingRecord>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(billingService.listByTenant(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<BillingRecord> record(@RequestParam String tenantId,
                                              @RequestParam(required = false) String userId,
                                              @RequestParam(required = false) String agentId,
                                              @RequestParam String model,
                                              @RequestParam(required = false) String traceId,
                                              @RequestParam int promptTokens,
                                              @RequestParam int completionTokens,
                                              @RequestParam double costUsd) {
        return ApiResponse.ok(billingService.record(tenantId, userId, agentId,
                model, traceId, promptTokens, completionTokens, costUsd));
    }
}
