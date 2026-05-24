package com.enterpriseai.memory.repository;

import com.enterpriseai.memory.entity.MemoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemoryRepository extends JpaRepository<MemoryEntry, String> {
    List<MemoryEntry> findByTenantIdAndUserId(String tenantId, String userId);
    List<MemoryEntry> findByTenantIdAndUserIdAndType(String tenantId, String userId, String type);
    List<MemoryEntry> findBySessionId(String sessionId);
}
