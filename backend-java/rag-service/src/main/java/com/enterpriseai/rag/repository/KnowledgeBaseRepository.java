package com.enterpriseai.rag.repository;

import com.enterpriseai.rag.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, String> {

    List<KnowledgeBase> findByTenantId(String tenantId);
}
