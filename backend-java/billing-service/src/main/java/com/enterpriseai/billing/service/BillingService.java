package com.enterpriseai.billing.service;

import com.enterpriseai.billing.entity.BillingRecord;
import com.enterpriseai.billing.repository.BillingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRepository billingRepository;
    private final ObjectMapper objectMapper;

    public BillingService(BillingRepository billingRepository, ObjectMapper objectMapper) {
        this.billingRepository = billingRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "llm.call", groupId = "billing-service")
    public void consumeLlmCall(String event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(event, Map.class);

            String tenantId = (String) payload.getOrDefault("tenant_id", "unknown");
            String userId = (String) payload.getOrDefault("user_id", "unknown");
            String agentId = (String) payload.getOrDefault("agent_id", "");
            String model = (String) payload.getOrDefault("model", "unknown");
            String traceId = (String) payload.getOrDefault("trace_id", "");
            int promptTokens = toInt(payload.get("prompt_tokens"), 0);
            int completionTokens = toInt(payload.get("completion_tokens"), 0);
            double costUsd = toDouble(payload.get("cost_usd"), 0.0);

            // Compute cost from token counts if not provided
            if (costUsd == 0.0 && (promptTokens > 0 || completionTokens > 0)) {
                costUsd = computeCost(model, promptTokens, completionTokens);
            }

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

            billingRepository.save(record);
            log.debug("Billing record saved: trace={}, model={}, cost=${}", traceId, model,
                    String.format("%.6f", costUsd));
        } catch (JsonProcessingException e) {
            log.error("Failed to parse llm.call event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to process billing event: {}", e.getMessage(), e);
        }
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

    private double computeCost(String model, int promptTokens, int completionTokens) {
        Map<String, double[]> pricing = Map.of(
                "gpt-4.1", new double[]{0.003, 0.006},
                "gpt-4.1-mini", new double[]{0.00015, 0.0006},
                "gpt-4.1-nano", new double[]{0.000075, 0.0003},
                "claude-sonnet-4-6", new double[]{0.003, 0.015},
                "claude-haiku-4-5-20251001", new double[]{0.0008, 0.004},
                "deepseek-v3", new double[]{0.00027, 0.0011},
                "qwen-max", new double[]{0.002, 0.006},
                "gemini-2.0-flash", new double[]{0.0001, 0.0004}
        );

        double[] rates = pricing.getOrDefault(model, new double[]{0.001, 0.002});
        return (promptTokens / 1000.0) * rates[0] + (completionTokens / 1000.0) * rates[1];
    }

    private int toInt(Object value, int defaultValue) {
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }

    private double toDouble(Object value, double defaultValue) {
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
}
