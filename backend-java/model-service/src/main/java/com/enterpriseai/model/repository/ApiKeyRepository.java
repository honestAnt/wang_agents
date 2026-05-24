package com.enterpriseai.model.repository;

import com.enterpriseai.model.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {

    List<ApiKey> findByTenantIdAndProviderId(String tenantId, String providerId);
}
