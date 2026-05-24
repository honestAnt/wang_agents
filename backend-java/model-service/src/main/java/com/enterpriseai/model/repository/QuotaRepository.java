package com.enterpriseai.model.repository;

import com.enterpriseai.model.entity.ModelQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotaRepository extends JpaRepository<ModelQuota, String> {

    List<ModelQuota> findByTenantId(String tenantId);

    List<ModelQuota> findByTenantIdAndUserId(String tenantId, String userId);
}
