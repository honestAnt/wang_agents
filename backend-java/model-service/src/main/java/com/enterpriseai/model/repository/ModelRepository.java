package com.enterpriseai.model.repository;

import com.enterpriseai.model.entity.ModelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<ModelEntity, String> {

    List<ModelEntity> findByTenantId(String tenantId);

    List<ModelEntity> findByTenantIdAndProviderId(String tenantId, String providerId);

    List<ModelEntity> findByTenantIdAndStatus(String tenantId, String status);
}
