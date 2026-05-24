package com.enterpriseai.billing.service;

import com.enterpriseai.billing.entity.BillingRecord;
import com.enterpriseai.billing.repository.BillingRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class BillingService {

    private final BillingRepository billingRepository;

    public BillingService(BillingRepository billingRepository) {
        this.billingRepository = billingRepository;
    }

    @KafkaListener(topics = "llm.call", groupId = "billing-service")
    public void consumeLlmCall(String event) {
        // Parse llm.call event JSON, extract cost info, save to billing_records
        // In production: parse JSON, compute cost, record to DB
    }

    public BillingRecord record(String tenantId, String userId, String agentId,
                                 String model, String traceId, int promptTokens,
                                 int completionTokens, double costUsd) {
        BillingRecord record = new BillingRecord();
        record.setId(UUID.randomUUID().toString());
        record.setTenantId(tenantId);
        record.setUserId(userId);
        record.setAgentId(agentId);
        record.setModel(model);
        record.setTraceId(traceId);
        record.setPromptTokens(promptTokens);
        record.setCompletionTokens(completionTokens);
        record.setCostUsd(BigDecimal.valueOf(costUsd));
        return billingRepository.save(record);
    }

    public List<BillingRecord> listByTenant(String tenantId) {
        return billingRepository.findByTenantId(tenantId);
    }
}
