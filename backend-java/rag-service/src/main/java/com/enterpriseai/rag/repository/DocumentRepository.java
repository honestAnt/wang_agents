package com.enterpriseai.rag.repository;

import com.enterpriseai.rag.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String> {

    List<Document> findByKbId(String kbId);

    List<Document> findByTenantId(String tenantId);
}
