package com.enterpriseai.skill.service;

import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.repository.SkillRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

    public Skill publish(String id) {
        Skill skill = getById(id);
        skill.setStatus("published");
        skill.setVersion(skill.getVersion() + 1);
        return skillRepository.save(skill);
    }

    public Skill update(String id, String name, String description, String promptTemplate) {
        Skill skill = getById(id);
        if (name != null) skill.setName(name);
        if (description != null) skill.setDescription(description);
        if (promptTemplate != null) skill.setPromptTemplate(promptTemplate);
        return skillRepository.save(skill);
    }

    public void delete(String id) {
        skillRepository.deleteById(id);
    }
}
