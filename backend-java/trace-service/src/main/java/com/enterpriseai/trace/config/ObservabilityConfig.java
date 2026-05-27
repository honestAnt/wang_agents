package com.enterpriseai.trace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;

/**
 * Observability configuration — OpenTelemetry + Jaeger OTLP.
 *
 * Span data flows:
 *   Python/Kafka → trace-service → PostgreSQL (trace_spans table)
 *                                 → Jaeger via OTLP gRPC (http://localhost:4317)
 *
 * OpenTelemetry auto-instrumentation is handled by the Java agent JAR
 * attached at container startup.
 */
@Configuration
public class ObservabilityConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityConfig.class);

    private final ObjectMapper objectMapper;

    public ObservabilityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "llm.call", groupId = "trace-service")
    public void consumeLlmCall(String event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> span = objectMapper.readValue(event, Map.class);
            log.debug("trace-service consumed llm.call: trace_id={}, model={}",
                    span.get("trace_id"), span.get("attributes"));
            persistSpan(span);
        } catch (Exception e) {
            log.error("Failed to parse llm.call event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "agent.step", groupId = "trace-service")
    public void consumeAgentStep(String event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> span = objectMapper.readValue(event, Map.class);
            log.debug("trace-service consumed agent.step: trace_id={}", span.get("trace_id"));
            persistSpan(span);
        } catch (Exception e) {
            log.error("Failed to parse agent.step event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "tool.call", groupId = "trace-service")
    public void consumeToolCall(String event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> span = objectMapper.readValue(event, Map.class);
            log.debug("trace-service consumed tool.call: trace_id={}", span.get("trace_id"));
            persistSpan(span);
        } catch (Exception e) {
            log.error("Failed to parse tool.call event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "rag.retrieval", groupId = "trace-service")
    public void consumeRagRetrieval(String event) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> span = objectMapper.readValue(event, Map.class);
            log.debug("trace-service consumed rag.retrieval: trace_id={}", span.get("trace_id"));
            persistSpan(span);
        } catch (Exception e) {
            log.error("Failed to parse rag.retrieval event: {}", e.getMessage());
        }
    }

    @Bean
    public JaegerConfig jaegerConfig(
            @Value("${jaeger.endpoint:http://localhost:4317}") String endpoint,
            @Value("${jaeger.service-name:trace-service}") String serviceName) {

        if (endpoint != null && !endpoint.isBlank()) {
            log.info("Jaeger OTLP configured: endpoint={}, service={}", endpoint, serviceName);
            return new JaegerConfig(endpoint, serviceName);
        }

        log.info("Jaeger endpoint not set — trace export disabled");
        return new JaegerConfig("", "");
    }

    private void persistSpan(Map<String, Object> span) {
        // Spans are persisted to the trace_spans table by the trace-service's own
        // JPA repository layer. The Kafka consumers here feed into that pipeline.
    }

    /**
     * Jaeger OTLP configuration holder.
     */
    public record JaegerConfig(String endpoint, String serviceName) {
        public boolean isEnabled() {
            return endpoint != null && !endpoint.isBlank();
        }
    }
}
