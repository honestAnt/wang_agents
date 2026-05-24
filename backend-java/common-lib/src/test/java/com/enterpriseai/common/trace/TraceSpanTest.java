package com.enterpriseai.common.trace;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TraceSpanTest {

    @Test
    void shouldCreateWithDefaults() {
        TraceSpan span = TraceSpan.builder()
                .type("llm")
                .name("gpt-4 call")
                .tenantId("t1")
                .build();

        assertNotNull(span.getTraceId());
        assertNotNull(span.getSpanId());
        assertEquals("llm", span.getType());
        assertEquals("gpt-4 call", span.getName());
        assertEquals("ok", span.getStatus());
        assertNotNull(span.getStartedAt());
    }

    @Test
    void end_shouldSetEndTimeAndLatency() {
        TraceSpan span = TraceSpan.builder()
                .type("tool")
                .name("weather_api")
                .tenantId("t1")
                .build();
        span.end();

        assertNotNull(span.getEndedAt());
        assertNotNull(span.getLatencyMs());
        assertTrue(span.getLatencyMs() >= 0);
        assertEquals("ok", span.getStatus());
    }

    @Test
    void endWithError_shouldSetErrorStatus() {
        TraceSpan span = TraceSpan.builder()
                .type("rag")
                .name("retrieve")
                .tenantId("t1")
                .build();
        span.endWithError("Connection refused");

        assertEquals("error", span.getStatus());
        assertEquals("Connection refused", span.getErrorMessage());
        assertNotNull(span.getEndedAt());
    }

    @Test
    void shouldSupportMetadata() {
        TraceSpan span = TraceSpan.builder()
                .type("llm")
                .name("completion")
                .tenantId("t1")
                .metadata(TraceSpan.SpanMetadata.builder()
                        .model("gpt-4.1")
                        .provider("openai")
                        .costUsd(0.015)
                        .promptTokens(500)
                        .completionTokens(200)
                        .totalTokens(700)
                        .build())
                .tags(List.of("production", "critical"))
                .build();

        assertNotNull(span.getMetadata());
        assertEquals("gpt-4.1", span.getMetadata().getModel());
        assertEquals(0.015, span.getMetadata().getCostUsd());
        assertEquals(700, span.getMetadata().getTotalTokens());
        assertEquals(2, span.getTags().size());
    }
}
