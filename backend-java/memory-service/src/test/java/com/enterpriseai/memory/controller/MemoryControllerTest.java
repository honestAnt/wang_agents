package com.enterpriseai.memory.controller;

import com.enterpriseai.memory.entity.MemoryEntry;
import com.enterpriseai.memory.repository.MemoryRepository;
import com.enterpriseai.memory.service.MemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoryControllerTest {

    @Mock
    private MemoryRepository repo;

    @Test
    void retrieve_shouldReturnOk() {
        var controller = new MemoryController(new MemoryService(repo));
        when(repo.findByTenantIdAndUserId("t1", "u1")).thenReturn(List.of());
        var response = controller.retrieve("t1", "u1");
        assertEquals(200, response.getCode());
    }

    @Test
    void getSession_shouldReturnOk() {
        var controller = new MemoryController(new MemoryService(repo));
        when(repo.findBySessionId("s1")).thenReturn(List.of());
        var response = controller.getSession("s1");
        assertEquals(200, response.getCode());
    }

    @Test
    void store_shouldReturnCreatedMemory() {
        var controller = new MemoryController(new MemoryService(repo));
        when(repo.save(any(MemoryEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        var response = controller.store("t1", "u1", "s1", "episodic", "test", null);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }
}
