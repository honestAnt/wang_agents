package com.enterpriseai.agent.service;

import com.enterpriseai.agent.entity.Agent;
import com.enterpriseai.agent.repository.AgentRepository;
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
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private AgentService agentService;

    @Test
    void create_shouldSaveAgent() {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent result = agentService.create("t1", "Helper", "A helpful agent",
                "You are a helpful assistant", "m1");

        assertEquals("Helper", result.getName());
        assertEquals("draft", result.getStatus());
        assertEquals(1, result.getVersion());
        assertNotNull(result.getId());
    }

    @Test
    void publish_shouldUpdateStatusAndVersion() {
        Agent agent = new Agent();
        agent.setId("a1");
        agent.setStatus("draft");
        agent.setVersion(1);
        when(agentRepository.findById("a1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        Agent result = agentService.publish("a1");

        assertEquals("published", result.getStatus());
        assertEquals(2, result.getVersion());
    }

    @Test
    void update_shouldOnlyChangeProvidedFields() {
        Agent agent = new Agent();
        agent.setId("a1");
        agent.setName("Old");
        agent.setSystemPrompt("Old prompt");
        when(agentRepository.findById("a1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenReturn(agent);

        Agent result = agentService.update("a1", "New", null, null, null);

        assertEquals("New", result.getName());
        assertEquals("Old prompt", result.getSystemPrompt()); // unchanged
    }

    @Test
    void delete_shouldCallRepository() {
        agentService.delete("a1");
        verify(agentRepository).deleteById("a1");
    }
}
