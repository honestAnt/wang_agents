package com.enterpriseai.model.service;

import com.enterpriseai.model.entity.ModelEntity;
import com.enterpriseai.model.repository.ModelRepository;
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
class ModelServiceTest {

    @Mock
    private ModelRepository modelRepository;

    @InjectMocks
    private ModelService modelService;

    @Test
    void listByTenant_shouldReturnModels() {
        when(modelRepository.findByTenantId("t1")).thenReturn(List.of(new ModelEntity()));

        List<ModelEntity> result = modelService.listByTenant("t1");
        assertEquals(1, result.size());
    }

    @Test
    void getById_shouldReturnModel() {
        ModelEntity m = new ModelEntity();
        m.setId("m1");
        m.setName("gpt-4.1");
        when(modelRepository.findById("m1")).thenReturn(Optional.of(m));

        ModelEntity result = modelService.getById("m1");
        assertEquals("gpt-4.1", result.getName());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(modelRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> modelService.getById("bad"));
    }

    @Test
    void create_shouldSaveWithDefaults() {
        when(modelRepository.save(any(ModelEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelEntity result = modelService.create("t1", "p1", "gpt-4.1", "GPT-4.1", "chat", 8192);
        assertEquals("gpt-4.1", result.getName());
        assertEquals("chat", result.getType());
        assertEquals(8192, result.getMaxTokens());
        assertNotNull(result.getId());
    }

    @Test
    void create_shouldUseDefaultValuesWhenNull() {
        when(modelRepository.save(any(ModelEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ModelEntity result = modelService.create("t1", "p1", "test-model", null, null, null);
        assertEquals("test-model", result.getDisplayName()); // falls back to name
        assertEquals("chat", result.getType());
        assertEquals(4096, result.getMaxTokens());
    }

    @Test
    void delete_shouldCallRepository() {
        modelService.delete("m1");
        verify(modelRepository).deleteById("m1");
    }
}
