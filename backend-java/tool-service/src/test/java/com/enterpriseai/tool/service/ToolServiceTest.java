package com.enterpriseai.tool.service;

import com.enterpriseai.tool.entity.Tool;
import com.enterpriseai.tool.repository.ToolRepository;
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
class ToolServiceTest {

    @Mock
    private ToolRepository toolRepository;

    @InjectMocks
    private ToolService toolService;

    @Test
    void register_shouldSetDefaults() {
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));

        Tool result = toolService.register("t1", "weather_api", "Weather API", "Gets weather",
                null, null, "https://api.weather.com", null, null, null, null);

        assertEquals("weather_api", result.getName());
        assertEquals("http", result.getToolType()); // default
        assertEquals("POST", result.getMethod());
        assertEquals(30000, result.getTimeoutMs());
        assertEquals(0, result.getRetryCount());
        assertEquals("fixed", result.getRetryBackoff());
        assertNotNull(result.getId());
    }

    @Test
    void register_shouldAcceptCustomToolType() {
        when(toolRepository.save(any(Tool.class))).thenAnswer(inv -> inv.getArgument(0));

        Tool result = toolService.register("t1", "mcp_tool", "MCP Tool", "Desc",
                "mcp", "{}", null, "GET", 5000, 3, "exponential");

        assertEquals("mcp", result.getToolType());
        assertEquals("GET", result.getMethod());
        assertEquals(5000, result.getTimeoutMs());
        assertEquals(3, result.getRetryCount());
        assertEquals("exponential", result.getRetryBackoff());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(toolRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> toolService.getById("bad"));
    }
}
