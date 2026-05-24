package com.enterpriseai.prompt.service;

import com.enterpriseai.prompt.entity.Prompt;
import com.enterpriseai.prompt.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private PromptRepository promptRepository;

    @InjectMocks
    private PromptService promptService;

    @Test
    void create_shouldSavePrompt() {
        when(promptRepository.save(any(Prompt.class))).thenAnswer(inv -> inv.getArgument(0));
        Prompt result = promptService.create("t1", "system_prompt_v2", "Improved prompt", "chat");
        assertEquals("system_prompt_v2", result.getName());
        assertEquals("chat", result.getCategory());
        assertEquals("draft", result.getStatus());
        assertNotNull(result.getId());
    }

    @Test
    void update_shouldChangeOnlyProvidedFields() {
        Prompt prompt = new Prompt();
        prompt.setId("p1");
        prompt.setName("Old");
        prompt.setDescription("Old desc");
        when(promptRepository.findById("p1")).thenReturn(Optional.of(prompt));
        when(promptRepository.save(any(Prompt.class))).thenReturn(prompt);

        Prompt result = promptService.update("p1", "New", null);
        assertEquals("New", result.getName());
        assertEquals("Old desc", result.getDescription());
    }
}
