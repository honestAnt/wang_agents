package com.enterpriseai.memory.service;

import com.enterpriseai.memory.entity.MemoryEntry;
import com.enterpriseai.memory.repository.MemoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    void store_shouldCreateEntry() {
        when(memoryRepository.save(any(MemoryEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        MemoryEntry result = memoryService.store("t1", "u1", "s1", "episodic", "User asked about billing", null);
        assertEquals("episodic", result.getType());
        assertEquals("u1", result.getUserId());
        assertNotNull(result.getId());
    }

    @Test
    void retrieve_shouldFilterByTenantAndUser() {
        when(memoryRepository.findByTenantIdAndUserId("t1", "u1")).thenReturn(List.of(new MemoryEntry()));
        List<MemoryEntry> result = memoryService.retrieve("t1", "u1");
        assertEquals(1, result.size());
    }

    @Test
    void retrieveByType_shouldFilterByMemoryType() {
        when(memoryRepository.findByTenantIdAndUserIdAndType("t1", "u1", "semantic"))
                .thenReturn(List.of(new MemoryEntry()));
        List<MemoryEntry> result = memoryService.retrieveByType("t1", "u1", "semantic");
        assertEquals(1, result.size());
    }

    @Test
    void recordAccess_shouldUpdateCount() {
        MemoryEntry entry = new MemoryEntry();
        entry.setId("m1");
        entry.setAccessCount(5);
        when(memoryRepository.findById("m1")).thenReturn(Optional.of(entry));
        when(memoryRepository.save(any(MemoryEntry.class))).thenReturn(entry);

        memoryService.recordAccess("m1");
        assertEquals(6, entry.getAccessCount());
        assertNotNull(entry.getLastAccessed());
    }
}
