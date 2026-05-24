package com.enterpriseai.model.service;

import com.enterpriseai.model.entity.ApiKey;
import com.enterpriseai.model.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public List<ApiKey> listByProvider(String tenantId, String providerId) {
        return apiKeyRepository.findByTenantIdAndProviderId(tenantId, providerId);
    }

    public ApiKey register(String tenantId, String providerId, String keyName, String rawKey) {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(UUID.randomUUID().toString());
        apiKey.setTenantId(tenantId);
        apiKey.setProviderId(providerId);
        apiKey.setKeyName(keyName);
        // Simple obfuscation — in production, use KMS / Vault
        apiKey.setEncryptedKey(Base64.getEncoder().encodeToString(rawKey.getBytes()));
        return apiKeyRepository.save(apiKey);
    }

    public void revoke(String id) {
        ApiKey key = apiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API key not found: " + id));
        key.setStatus("revoked");
        apiKeyRepository.save(key);
    }
}
