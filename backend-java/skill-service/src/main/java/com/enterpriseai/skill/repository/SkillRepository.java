package com.enterpriseai.skill.repository;

import com.enterpriseai.skill.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {

    List<Skill> findByTenantId(String tenantId);

    List<Skill> findByTenantIdAndStatus(String tenantId, String status);

    List<Skill> findByTenantIdAndCategory(String tenantId, String category);

    List<Skill> findByTenantIdAndName(String tenantId, String name);

    List<Skill> findByTenantIdAndNameContainingIgnoreCaseOrDisplayNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String tenantId, String name, String displayName, String description, String category);
}
