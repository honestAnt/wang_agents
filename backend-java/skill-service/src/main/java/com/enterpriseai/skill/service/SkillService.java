package com.enterpriseai.skill.service;

import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public List<Skill> listByTenant(String tenantId) {
        return skillRepository.findByTenantId(tenantId);
    }

    public List<Skill> listPublished(String tenantId) {
        return skillRepository.findByTenantIdAndStatus(tenantId, "published");
    }

    public Skill getById(String id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
    }

    public List<Skill> matchByIntent(String tenantId, String intent) {
        return skillRepository
                .findByTenantIdAndNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                        tenantId, intent, intent, intent, intent)
                .stream()
                .filter(s -> "published".equals(s.getStatus()))
                .collect(Collectors.toList());
    }

    public Map<String, Object> execute(String skillId, Map<String, Object> context) {
        Skill skill = getById(skillId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("skill_id", skillId);
        result.put("skill_name", skill.getName());
        result.put("status", "completed");
        result.put("prompt_template", skill.getPromptTemplate());
        result.put("input", context);
        result.put("output", Map.of("message", "Skill " + skill.getName() + " executed successfully"));
        return result;
    }

    public List<String> listCategories(String tenantId) {
        return skillRepository.findByTenantId(tenantId).stream()
                .map(Skill::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public Skill create(String tenantId, String name, String description,
                        String promptTemplate, String category) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID().toString());
        skill.setTenantId(tenantId);
        skill.setName(name);
        skill.setDisplayName(name);
        skill.setDescription(description);
        skill.setPromptTemplate(promptTemplate);
        skill.setCategory(category != null ? category : "general");
        return skillRepository.save(skill);
    }

    public Skill createFull(String tenantId, String name, String displayName,
                            String description, String promptTemplate, String category,
                            String inputSchema, String outputSchema, String iconUrl) {
        Skill skill = new Skill();
        skill.setId(UUID.randomUUID().toString());
        skill.setTenantId(tenantId);
        skill.setName(name);
        skill.setDisplayName(displayName != null ? displayName : name);
        skill.setDescription(description);
        skill.setPromptTemplate(promptTemplate);
        skill.setCategory(category != null ? category : "general");
        skill.setInputSchema(inputSchema);
        skill.setOutputSchema(outputSchema);
        skill.setIconUrl(iconUrl);
        return skillRepository.save(skill);
    }

    public Skill publish(String id) {
        Skill skill = getById(id);
        skill.setStatus("published");
        skill.setVersion(skill.getVersion() + 1);
        return skillRepository.save(skill);
    }

    public Skill install(String id) {
        Skill skill = getById(id);
        skill.setDownloadCount(skill.getDownloadCount() + 1);
        return skillRepository.save(skill);
    }

    public Skill update(String id, String name, String description, String promptTemplate) {
        Skill skill = getById(id);
        if (name != null) skill.setName(name);
        if (description != null) skill.setDescription(description);
        if (promptTemplate != null) skill.setPromptTemplate(promptTemplate);
        return skillRepository.save(skill);
    }

    public Skill updateFull(String id, String name, String displayName, String description,
                            String promptTemplate, String category, String inputSchema,
                            String outputSchema, String iconUrl, String status) {
        Skill skill = getById(id);
        if (name != null) skill.setName(name);
        if (displayName != null) skill.setDisplayName(displayName);
        if (description != null) skill.setDescription(description);
        if (promptTemplate != null) skill.setPromptTemplate(promptTemplate);
        if (category != null) skill.setCategory(category);
        if (inputSchema != null) skill.setInputSchema(inputSchema);
        if (outputSchema != null) skill.setOutputSchema(outputSchema);
        if (iconUrl != null) skill.setIconUrl(iconUrl);
        if (status != null) skill.setStatus(status);
        return skillRepository.save(skill);
    }

    public void delete(String id) {
        skillRepository.deleteById(id);
    }
}
