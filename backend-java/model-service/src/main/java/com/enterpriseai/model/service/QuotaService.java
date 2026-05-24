package com.enterpriseai.model.service;

import com.enterpriseai.model.entity.ModelQuota;
import com.enterpriseai.model.repository.QuotaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class QuotaService {

    private final QuotaRepository quotaRepository;

    public QuotaService(QuotaRepository quotaRepository) {
        this.quotaRepository = quotaRepository;
    }

    public List<ModelQuota> listByTenant(String tenantId) {
        return quotaRepository.findByTenantId(tenantId);
    }

    public ModelQuota createOrUpdate(String tenantId, String userId, String agentId,
                                      String modelId, Long maxTokens, Long maxRequests,
                                      String period) {
        ModelQuota quota = new ModelQuota();
        quota.setId(UUID.randomUUID().toString());
        quota.setTenantId(tenantId);
        quota.setUserId(userId);
        quota.setAgentId(agentId);
        quota.setModelId(modelId);
        quota.setMaxTokens(maxTokens != null ? maxTokens : 1_000_000L);
        quota.setMaxRequests(maxRequests != null ? maxRequests : 10_000L);
        quota.setPeriod(period != null ? period : "monthly");
        return quotaRepository.save(quota);
    }
}
