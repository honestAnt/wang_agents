package com.enterpriseai.tool.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.tool.entity.Tool;
import com.enterpriseai.tool.service.ToolService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/stats")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Map<String, Object>> stats(@RequestParam String tenantId) {
        return ApiResponse.ok(toolService.getStats(tenantId));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Tool> resolve(@RequestParam String name,
                                     @RequestParam String tenantId) {
        return ApiResponse.ok(toolService.resolveByName(tenantId, name));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Tool> get(@PathVariable String id) {
        return ApiResponse.ok(toolService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Tool> register(@RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        String name = (String) body.get("name");
        String displayName = (String) body.getOrDefault("displayName", name);
        String description = (String) body.get("description");
        String toolType = (String) body.getOrDefault("toolType", "http");
        String schemaJson = (String) body.get("schemaJson");
        String endpointUrl = (String) body.get("endpointUrl");
        String method = (String) body.getOrDefault("method", "POST");
        Integer timeoutMs = body.get("timeoutMs") != null
                ? ((Number) body.get("timeoutMs")).intValue() : 30000;
        Integer retryCount = body.get("retryCount") != null
                ? ((Number) body.get("retryCount")).intValue() : 0;
        String retryBackoff = (String) body.getOrDefault("retryBackoff", "fixed");
        return ApiResponse.ok(toolService.register(tenantId, name, displayName,
                description, toolType, schemaJson, endpointUrl, method,
                timeoutMs, retryCount, retryBackoff));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Tool> update(@PathVariable String id,
                                    @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String displayName = (String) body.get("displayName");
        String description = (String) body.get("description");
        String toolType = (String) body.get("toolType");
        String schemaJson = (String) body.get("schemaJson");
        String endpointUrl = (String) body.get("endpointUrl");
        String method = (String) body.get("method");
        Integer timeoutMs = body.get("timeoutMs") != null
                ? ((Number) body.get("timeoutMs")).intValue() : null;
        Integer retryCount = body.get("retryCount") != null
                ? ((Number) body.get("retryCount")).intValue() : null;
        String retryBackoff = (String) body.get("retryBackoff");
        String status = (String) body.get("status");
        return ApiResponse.ok(toolService.update(id, name, displayName, description,
                toolType, schemaJson, endpointUrl, method, timeoutMs, retryCount,
                retryBackoff, status));
    }

    @PostMapping("/execute")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Map<String, Object>> execute(@RequestBody Map<String, Object> body) {
        String toolId = (String) body.get("toolId");
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Map.of());
        return ApiResponse.ok(toolService.execute(toolId, params));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        toolService.delete(id);
        return ApiResponse.ok(null);
    }
}
