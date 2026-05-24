package com.enterpriseai.rag.service;

import com.enterpriseai.rag.entity.KnowledgeBase;
import com.enterpriseai.rag.repository.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository) {
        this.kbRepository = kbRepository;
    }

    public List<KnowledgeBase> listByTenant(String tenantId) {
        return kbRepository.findByTenantId(tenantId);
    }

    public KnowledgeBase getById(String id) {
        return kbRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found: " + id));
    }

    public KnowledgeBase create(String tenantId, String name, String description,
                                 String embeddingModel, Integer embeddingDim,
                                 Integer chunkSize, Integer chunkOverlap,
                                 String chunkStrategy, String indexType) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(UUID.randomUUID().toString());
        kb.setTenantId(tenantId);
        kb.setName(name);
        kb.setDescription(description);
        if (embeddingModel != null) kb.setEmbeddingModel(embeddingModel);
        if (embeddingDim != null) kb.setEmbeddingDim(embeddingDim);
        if (chunkSize != null) kb.setChunkSize(chunkSize);
        if (chunkOverlap != null) kb.setChunkOverlap(chunkOverlap);
        if (chunkStrategy != null) kb.setChunkStrategy(chunkStrategy);
        if (indexType != null) kb.setIndexType(indexType);
        return kbRepository.save(kb);
    }

    public void delete(String id) {
        kbRepository.deleteById(id);
    }
}
