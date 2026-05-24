package com.enterpriseai.trace.controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TraceControllerTest {

    private final TraceController controller = new TraceController();

    @Test
    void listSessions_shouldReturnOk() {
        var response = controller.listSessions(10, 0);
        assertEquals(200, response.getCode());
    }

    @Test
    void getSessionDetail_shouldReturnOk() {
        var response = controller.getSessionDetail("s-1");
        assertEquals(200, response.getCode());
        assertEquals("s-1", response.getData().get("id"));
    }

    @Test
    void getCostAnalytics_shouldReturnOk() {
        var response = controller.getCostAnalytics("model");
        assertEquals(200, response.getCode());
    }

    @Test
    void ingestSpans_shouldReturnAcceptedCount() {
        var body = new java.util.HashMap<String, Object>();
        body.put("spans", java.util.List.of(
                java.util.Map.of("trace_id", "t1", "span_id", "s1", "type", "llm")
        ));
        var response = controller.ingestSpans(body);
        assertEquals(200, response.getCode());
        assertEquals(1, ((java.util.Map<?, ?>)response.getData()).get("accepted"));
    }
}
