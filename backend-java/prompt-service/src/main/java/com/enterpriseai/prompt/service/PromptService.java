package com.enterpriseai.prompt.service;

import com.enterpriseai.prompt.entity.Prompt;
import com.enterpriseai.prompt.repository.PromptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PromptService {

    private final PromptRepository promptRepository;

    public PromptService(PromptRepository promptRepository) {
        this.promptRepository = promptRepository;
    }

    public List<Prompt> listByTenant(String tenantId) {
        return promptRepository.findByTenantId(tenantId);
    }

    public Prompt getById(String id) {
        return promptRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prompt not found: " + id));
    }

    public Prompt create(String tenantId, String name, String description, String category) {
        Prompt prompt = new Prompt();
        prompt.setId(UUID.randomUUID().toString());
        prompt.setTenantId(tenantId);
        prompt.setName(name);
        prompt.setDescription(description);
        prompt.setCategory(category != null ? category : "general");
        return promptRepository.save(prompt);
    }

    public Prompt update(String id, String name, String description) {
        Prompt prompt = getById(id);
        if (name != null) prompt.setName(name);
        if (description != null) prompt.setDescription(description);
        return promptRepository.save(prompt);
    }

    public void delete(String id) {
        promptRepository.deleteById(id);
    }
}
