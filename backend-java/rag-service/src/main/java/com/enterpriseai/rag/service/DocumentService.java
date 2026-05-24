package com.enterpriseai.rag.service;

import com.enterpriseai.rag.entity.Document;
import com.enterpriseai.rag.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<Document> listByKb(String kbId) {
        return documentRepository.findByKbId(kbId);
    }

    public Document upload(String kbId, String tenantId, String title,
                           String fileType, String fileUrl, Long fileSize) {
        Document doc = new Document();
        doc.setId(UUID.randomUUID().toString());
        doc.setKbId(kbId);
        doc.setTenantId(tenantId);
        doc.setTitle(title);
        doc.setFileType(fileType);
        doc.setFileUrl(fileUrl);
        doc.setFileSize(fileSize);
        doc.setStatus("uploading");
        return documentRepository.save(doc);
    }

    public Document updateStatus(String id, String status) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));
        doc.setStatus(status);
        return documentRepository.save(doc);
    }

    public void delete(String id) {
        documentRepository.deleteById(id);
    }
}
