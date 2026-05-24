package com.enterpriseai.prompt.repository;

import com.enterpriseai.prompt.entity.Prompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptRepository extends JpaRepository<Prompt, String> {
    List<Prompt> findByTenantId(String tenantId);
    List<Prompt> findByTenantIdAndCategory(String tenantId, String category);
}
