package com.enterpriseai.tool.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.tool.entity.Tool;
import com.enterpriseai.tool.service.ToolService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolService toolService;

    public ToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<Tool>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(toolService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Tool> get(@PathVariable String id) {
        return ApiResponse.ok(toolService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Tool> register(
            @RequestParam String tenantId,
            @RequestParam String name,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String toolType,
            @RequestParam(required = false) String schemaJson,
            @RequestParam(required = false) String endpointUrl,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) Integer timeoutMs,
            @RequestParam(required = false) Integer retryCount,
            @RequestParam(required = false) String retryBackoff) {
        return ApiResponse.ok(toolService.register(tenantId, name, displayName,
                description, toolType, schemaJson, endpointUrl, method,
                timeoutMs, retryCount, retryBackoff));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        toolService.delete(id);
        return ApiResponse.ok(null);
    }
}
