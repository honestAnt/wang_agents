package com.enterpriseai.memory.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.memory.entity.MemoryEntry;
import com.enterpriseai.memory.service.MemoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final MemoryService memoryService;

    public MemoryController(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<MemoryEntry> store(@RequestParam String tenantId,
                                          @RequestParam String userId,
                                          @RequestParam(required = false) String sessionId,
                                          @RequestParam String type,
                                          @RequestParam String content,
                                          @RequestParam(required = false) String metadataJson) {
        return ApiResponse.ok(memoryService.store(tenantId, userId, sessionId, type, content, metadataJson));
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<MemoryEntry>> retrieve(@RequestParam String tenantId,
                                                     @RequestParam String userId) {
        return ApiResponse.ok(memoryService.retrieve(tenantId, userId));
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<MemoryEntry>> getSession(@PathVariable String sessionId) {
        return ApiResponse.ok(memoryService.getSessionMemories(sessionId));
    }
}
