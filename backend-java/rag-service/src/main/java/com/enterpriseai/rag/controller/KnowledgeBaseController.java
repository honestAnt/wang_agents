package com.enterpriseai.rag.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.rag.entity.KnowledgeBase;
import com.enterpriseai.rag.service.KnowledgeBaseService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    public KnowledgeBaseController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<KnowledgeBase>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(kbService.listByTenant(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<KnowledgeBase> get(@PathVariable String id) {
        return ApiResponse.ok(kbService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<KnowledgeBase> create(
            @RequestParam String tenantId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String embeddingModel,
            @RequestParam(required = false) Integer embeddingDim,
            @RequestParam(required = false) Integer chunkSize,
            @RequestParam(required = false) Integer chunkOverlap,
            @RequestParam(required = false) String chunkStrategy,
            @RequestParam(required = false) String indexType) {
        return ApiResponse.ok(kbService.create(tenantId, name, description,
                embeddingModel, embeddingDim, chunkSize, chunkOverlap,
                chunkStrategy, indexType));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        kbService.delete(id);
        return ApiResponse.ok(null);
    }
}
