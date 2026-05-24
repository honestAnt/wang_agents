package com.enterpriseai.user.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.user.dto.TenantCreateRequest;
import com.enterpriseai.user.entity.Tenant;
import com.enterpriseai.user.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<List<Tenant>> list() {
        return ApiResponse.ok(tenantService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SuperAdmin') or hasRole('TenantAdmin')")
    public ApiResponse<Tenant> get(@PathVariable String id) {
        return ApiResponse.ok(tenantService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Tenant> create(@Valid @RequestBody TenantCreateRequest request) {
        return ApiResponse.ok(tenantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Tenant> update(@PathVariable String id,
                                      @Valid @RequestBody TenantCreateRequest request) {
        return ApiResponse.ok(tenantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SuperAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        tenantService.delete(id);
        return ApiResponse.ok(null);
    }
}
