package com.enterpriseai.memory.service;

import com.enterpriseai.memory.entity.MemoryEntry;
import com.enterpriseai.memory.repository.MemoryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;

    public MemoryService(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public MemoryEntry store(String tenantId, String userId, String sessionId,
                             String type, String content, String metadataJson) {
        MemoryEntry entry = new MemoryEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setTenantId(tenantId);
        entry.setUserId(userId);
        entry.setSessionId(sessionId);
        entry.setType(type);
        entry.setContent(content);
        entry.setMetadataJson(metadataJson);
        return memoryRepository.save(entry);
    }

    public List<MemoryEntry> retrieve(String tenantId, String userId) {
        return memoryRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    public List<MemoryEntry> retrieveByType(String tenantId, String userId, String type) {
        return memoryRepository.findByTenantIdAndUserIdAndType(tenantId, userId, type);
    }

    public List<MemoryEntry> getSessionMemories(String sessionId) {
        return memoryRepository.findBySessionId(sessionId);
    }

    public void recordAccess(String id) {
        memoryRepository.findById(id).ifPresent(entry -> {
            entry.setAccessCount(entry.getAccessCount() + 1);
            entry.setLastAccessed(Instant.now());
            // Apply time decay
            memoryRepository.save(entry);
        });
    }
}
