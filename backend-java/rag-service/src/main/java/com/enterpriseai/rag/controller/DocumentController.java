package com.enterpriseai.rag.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.rag.entity.Document;
import com.enterpriseai.rag.service.DocumentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases/{kbId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<Document>> list(@PathVariable String kbId) {
        return ApiResponse.ok(documentService.listByKb(kbId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Document> upload(@PathVariable String kbId,
                                         @RequestParam String tenantId,
                                         @RequestParam String title,
                                         @RequestParam String fileType,
                                         @RequestParam(required = false) String fileUrl,
                                         @RequestParam(required = false) Long fileSize) {
        return ApiResponse.ok(documentService.upload(
                kbId, tenantId, title, fileType, fileUrl, fileSize));
    }

    @PutMapping("/{docId}/status")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Document> updateStatus(@PathVariable String docId,
                                               @RequestParam String status) {
        return ApiResponse.ok(documentService.updateStatus(docId, status));
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String docId) {
        documentService.delete(docId);
        return ApiResponse.ok(null);
    }
}
