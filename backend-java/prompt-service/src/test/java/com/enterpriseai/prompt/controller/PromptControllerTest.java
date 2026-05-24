package com.enterpriseai.prompt.controller;

import com.enterpriseai.prompt.entity.Prompt;
import com.enterpriseai.prompt.repository.PromptRepository;
import com.enterpriseai.prompt.service.PromptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromptControllerTest {

    @Mock
    private PromptRepository repo;

    @Test
    void list_shouldReturnOk() {
        var controller = new PromptController(new PromptService(repo));
        when(repo.findByTenantId("t1")).thenReturn(List.of());
        var response = controller.list("t1");
        assertEquals(200, response.getCode());
    }

    @Test
    void create_shouldReturnCreatedPrompt() {
        var controller = new PromptController(new PromptService(repo));
        when(repo.save(any(Prompt.class))).thenAnswer(inv -> inv.getArgument(0));
        var response = controller.create("t1", "Test", "Desc", "general");
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }
}
