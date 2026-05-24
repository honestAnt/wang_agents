package com.enterpriseai.prompt.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.prompt.entity.Prompt;
import com.enterpriseai.prompt.service.PromptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<Prompt>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(promptService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Prompt> get(@PathVariable String id) {
        return ApiResponse.ok(promptService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Prompt> create(@RequestParam String tenantId,
                                       @RequestParam String name,
                                       @RequestParam(required = false) String description,
                                       @RequestParam(required = false) String category) {
        return ApiResponse.ok(promptService.create(tenantId, name, description, category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Prompt> update(@PathVariable String id,
                                       @RequestParam(required = false) String name,
                                       @RequestParam(required = false) String description) {
        return ApiResponse.ok(promptService.update(id, name, description));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        promptService.delete(id);
        return ApiResponse.ok(null);
    }
}
