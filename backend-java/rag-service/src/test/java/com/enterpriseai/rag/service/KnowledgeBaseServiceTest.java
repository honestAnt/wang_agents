package com.enterpriseai.rag.service;

import com.enterpriseai.rag.entity.KnowledgeBase;
import com.enterpriseai.rag.repository.KnowledgeBaseRepository;
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
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseRepository kbRepository;

    @InjectMocks
    private KnowledgeBaseService kbService;

    @Test
    void listByTenant_shouldReturnKBs() {
        when(kbRepository.findByTenantId("t1")).thenReturn(List.of(new KnowledgeBase()));

        List<KnowledgeBase> result = kbService.listByTenant("t1");
        assertEquals(1, result.size());
    }

    @Test
    void create_shouldApplyDefaults() {
        when(kbRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> inv.getArgument(0));

        KnowledgeBase result = kbService.create("t1", "Docs", "Company docs", null, null, null, null, null, null);

        assertEquals("Docs", result.getName());
        assertEquals("text-embedding-3-small", result.getEmbeddingModel());
        assertEquals(1536, result.getEmbeddingDim());
        assertEquals(512, result.getChunkSize());
        assertEquals(50, result.getChunkOverlap());
        assertEquals("recursive", result.getChunkStrategy());
        assertEquals("hybrid", result.getIndexType());
    }

    @Test
    void create_shouldAcceptCustomConfig() {
        when(kbRepository.save(any(KnowledgeBase.class))).thenAnswer(inv -> inv.getArgument(0));

        KnowledgeBase result = kbService.create("t1", "Code", "Code docs",
                "custom-embed", 768, 1024, 100, "semantic", "vector");

        assertEquals("custom-embed", result.getEmbeddingModel());
        assertEquals(768, result.getEmbeddingDim());
        assertEquals(1024, result.getChunkSize());
        assertEquals("semantic", result.getChunkStrategy());
        assertEquals("vector", result.getIndexType());
    }

    @Test
    void delete_shouldCallRepository() {
        kbService.delete("kb1");
        verify(kbRepository).deleteById("kb1");
    }
}
