package com.enterpriseai.agent.controller;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.service.AgentService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
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
        // In production, this sends a test request to agent-python runtime
        // and returns the full execution trace
        return ApiResponse.ok("Debug result placeholder for agent: " + id);
    }
}
