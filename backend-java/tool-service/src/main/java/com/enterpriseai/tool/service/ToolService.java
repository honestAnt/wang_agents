package com.enterpriseai.tool.service;

import com.enterpriseai.tool.entity.Tool;
import com.enterpriseai.tool.repository.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    public void delete(String id) {
        toolRepository.deleteById(id);
    }
}
