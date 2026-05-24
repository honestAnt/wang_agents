package com.enterpriseai.agent.service;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.repository.AgentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AgentService {

    private final AgentRepository agentRepository;

    public AgentService(AgentRepository agentRepository) {
        this.agentRepository = agentRepository;
    }

    public List<Agent> listByTenant(String tenantId) {
        return agentRepository.findByTenantId(tenantId);
    }

    public Agent getById(String id) {
        return agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + id));
    }

    public Agent create(String tenantId, String name, String description,
                        String systemPrompt, String modelId) {
        Agent agent = new Agent();
        agent.setId(UUID.randomUUID().toString());
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setDescription(description);
        agent.setSystemPrompt(systemPrompt);
        agent.setModelId(modelId);
        return agentRepository.save(agent);
    }

    public Agent update(String id, String name, String description,
                        String systemPrompt, String modelId) {
        Agent agent = getById(id);
        if (name != null) agent.setName(name);
        if (description != null) agent.setDescription(description);
        if (systemPrompt != null) agent.setSystemPrompt(systemPrompt);
        if (modelId != null) agent.setModelId(modelId);
        return agentRepository.save(agent);
    }

    public Agent publish(String id) {
        Agent agent = getById(id);
        agent.setStatus("published");
        agent.setVersion(agent.getVersion() + 1);
        return agentRepository.save(agent);
    }

    public void delete(String id) {
        agentRepository.deleteById(id);
    }
}
