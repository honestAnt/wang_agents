package com.enterpriseai.model.service;

import com.enterpriseai.model.entity.ModelEntity;
import com.enterpriseai.model.repository.ModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ModelService {

    private final ModelRepository modelRepository;

    public ModelService(ModelRepository modelRepository) {
        this.modelRepository = modelRepository;
    }

    public List<ModelEntity> listByTenant(String tenantId) {
        return modelRepository.findByTenantId(tenantId);
    }

    public ModelEntity getById(String id) {
        return modelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Model not found: " + id));
    }

    public ModelEntity create(String tenantId, String providerId, String name,
                              String displayName, String type, Integer maxTokens) {
        ModelEntity model = new ModelEntity();
        model.setId(UUID.randomUUID().toString());
        model.setTenantId(tenantId);
        model.setProviderId(providerId);
        model.setName(name);
        model.setDisplayName(displayName != null ? displayName : name);
        model.setType(type != null ? type : "chat");
        model.setMaxTokens(maxTokens != null ? maxTokens : 4096);
        return modelRepository.save(model);
    }

    public void delete(String id) {
        modelRepository.deleteById(id);
    }
}
