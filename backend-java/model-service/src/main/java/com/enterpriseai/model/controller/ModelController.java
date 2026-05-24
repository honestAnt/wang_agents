package com.enterpriseai.model.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.model.entity.ModelEntity;
import com.enterpriseai.model.service.ModelService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<ModelEntity>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(modelService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<ModelEntity> get(@PathVariable String id) {
        return ApiResponse.ok(modelService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<ModelEntity> create(@RequestParam String tenantId,
                                            @RequestParam String providerId,
                                            @RequestParam String name,
                                            @RequestParam(required = false) String displayName,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) Integer maxTokens) {
        return ApiResponse.ok(
                modelService.create(tenantId, providerId, name, displayName, type, maxTokens));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        modelService.delete(id);
        return ApiResponse.ok(null);
    }
}
