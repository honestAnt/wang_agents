package com.enterpriseai.model.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.model.entity.ApiKey;
import com.enterpriseai.model.service.ApiKeyService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<List<ApiKey>> list(@RequestParam String tenantId,
                                           @RequestParam String providerId) {
        return ApiResponse.ok(apiKeyService.listByProvider(tenantId, providerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<ApiKey> register(@RequestParam String tenantId,
                                         @RequestParam String providerId,
                                         @RequestParam String keyName,
                                         @RequestParam String rawKey) {
        return ApiResponse.ok(apiKeyService.register(tenantId, providerId, keyName, rawKey));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> revoke(@PathVariable String id) {
        apiKeyService.revoke(id);
        return ApiResponse.ok(null);
    }
}
