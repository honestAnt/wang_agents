package com.enterpriseai.tool.service;

import com.enterpriseai.tool.entity.Tool;
import com.enterpriseai.tool.repository.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ToolService {

    private final ToolRepository toolRepository;

    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    public List<Tool> listByTenant(String tenantId) {
        return toolRepository.findByTenantId(tenantId);
    }

    public Tool getById(String id) {
        return toolRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tool not found: " + id));
    }

    public Tool resolveByName(String tenantId, String name) {
        List<Tool> tools = toolRepository.findByTenantIdAndName(tenantId, name);
        if (tools.isEmpty()) {
            throw new RuntimeException("Tool not found: " + name);
        }
        return tools.get(0);
    }

    public Map<String, Object> execute(String toolId, Map<String, Object> params) {
        Tool tool = getById(toolId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tool_id", toolId);
        result.put("tool_name", tool.getName());
        result.put("status", "completed");
        result.put("input", params);
        result.put("output", Map.of("message", "Tool " + tool.getName() + " executed successfully"));
        return result;
    }

    public Map<String, Object> getStats(String tenantId) {
        List<Tool> all = toolRepository.findByTenantId(tenantId);
        List<Tool> active = toolRepository.findByTenantIdAndStatus(tenantId, "active");
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", all.size());
        stats.put("active", active.size());
        stats.put("inactive", all.size() - active.size());
        return stats;
    }

    public Tool register(String tenantId, String name, String displayName,
                         String description, String toolType, String schemaJson,
                         String endpointUrl, String method,
                         Integer timeoutMs, Integer retryCount, String retryBackoff) {
        Tool tool = new Tool();
        tool.setId(UUID.randomUUID().toString());
        tool.setTenantId(tenantId);
        tool.setName(name);
        tool.setDisplayName(displayName != null ? displayName : name);
        tool.setDescription(description);
        tool.setToolType(toolType != null ? toolType : "http");
        tool.setSchemaJson(schemaJson);
        tool.setEndpointUrl(endpointUrl);
        tool.setMethod(method != null ? method : "POST");
        tool.setTimeoutMs(timeoutMs != null ? timeoutMs : 30000);
        tool.setRetryCount(retryCount != null ? retryCount : 0);
        tool.setRetryBackoff(retryBackoff != null ? retryBackoff : "fixed");
        return toolRepository.save(tool);
    }

    public Tool update(String id, String name, String displayName, String description,
                       String toolType, String schemaJson, String endpointUrl,
                       String method, Integer timeoutMs, Integer retryCount,
                       String retryBackoff, String status) {
        Tool tool = getById(id);
        if (name != null) tool.setName(name);
        if (displayName != null) tool.setDisplayName(displayName);
        if (description != null) tool.setDescription(description);
        if (toolType != null) tool.setToolType(toolType);
        if (schemaJson != null) tool.setSchemaJson(schemaJson);
        if (endpointUrl != null) tool.setEndpointUrl(endpointUrl);
        if (method != null) tool.setMethod(method);
        if (timeoutMs != null) tool.setTimeoutMs(timeoutMs);
        if (retryCount != null) tool.setRetryCount(retryCount);
        if (retryBackoff != null) tool.setRetryBackoff(retryBackoff);
        if (status != null) tool.setStatus(status);
        tool.setUpdatedAt(java.time.Instant.now());
        return toolRepository.save(tool);
    }

    public void delete(String id) {
        toolRepository.deleteById(id);
    }
}
