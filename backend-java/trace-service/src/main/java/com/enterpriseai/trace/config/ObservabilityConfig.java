package com.enterpriseai.trace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task 2.8 — Langfuse + OpenTelemetry integration scaffold.
 *
 * In production, add:
 * - opentelemetry-javaagent.jar for automatic instrumentation
 * - Langfuse SDK for LLM observability
 * - OTEL Collector endpoint configuration
 * - Grafana dashboard JSON templates
 */
@Configuration
public class ObservabilityConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityConfig.class);

    @KafkaListener(topics = "llm.call", groupId = "trace-service")
    public void consumeLlmCall(String event) {
        log.debug("Consumed llm.call event");
        // Parse and persist to trace_spans
    }

    @KafkaListener(topics = "agent.step", groupId = "trace-service")
    public void consumeAgentStep(String event) {
        log.debug("Consumed agent.step event");
    }

    @KafkaListener(topics = "tool.call", groupId = "trace-service")
    public void consumeToolCall(String event) {
        log.debug("Consumed tool.call event");
    }

    @KafkaListener(topics = "rag.retrieval", groupId = "trace-service")
    public void consumeRagRetrieval(String event) {
        log.debug("Consumed rag.retrieval event");
    }

    @Bean
    public String langfuseConfig() {
        // In production: configure Langfuse client with
        // LANGFUSE_PUBLIC_KEY, LANGFUSE_SECRET_KEY, LANGFUSE_HOST
        log.info("Langfuse integration ready (placeholder)");
        return "langfuse-placeholder";
    }
}
