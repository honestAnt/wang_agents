package com.enterpriseai.agent.controller;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.service.AgentService;
import com.enterpriseai.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private static final Logger log = LoggerFactory.getLogger(AgentController.class);

    private final AgentService agentService;
    private final RestTemplate restTemplate;

    @Value("${agent-runtime.url:http://localhost:8000}")
    private String agentRuntimeUrl;

    public AgentController(AgentService agentService, RestTemplateBuilder restTemplateBuilder) {
        this.agentService = agentService;
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('Operator')")
    public ApiResponse<List<Agent>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(agentService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Agent> get(@PathVariable String id) {
        return ApiResponse.ok(agentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Agent> create(@RequestParam String tenantId,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String systemPrompt,
                                      @RequestParam(required = false) String modelId) {
        return ApiResponse.ok(agentService.create(
                tenantId, name, description, systemPrompt, modelId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Agent> update(@PathVariable String id,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String systemPrompt,
                                      @RequestParam(required = false) String modelId) {
        return ApiResponse.ok(agentService.update(
                id, name, description, systemPrompt, modelId));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Agent> publish(@PathVariable String id) {
        return ApiResponse.ok(agentService.publish(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        agentService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/debug")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<String> debug(@PathVariable String id,
                                      @RequestBody String message) {
        Agent agent = agentService.getById(id);
        if (agent == null) {
            return ApiResponse.error(404, "Agent not found: " + id);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                    "tenant_id", agent.getTenantId(),
                    "user_message", message,
                    "agent_id", id
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(
                    agentRuntimeUrl + "/api/v1/chat", entity, String.class);

            return ApiResponse.ok(response != null ? response : "No response from agent runtime");
        } catch (RestClientException e) {
            log.error("Agent runtime debug failed: {}", e.getMessage());
            return ApiResponse.error(502, "Agent runtime error: " + e.getMessage());
        }
    }
}
