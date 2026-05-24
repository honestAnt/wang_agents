package com.enterpriseai.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void ok_shouldReturnCode200() {
        ApiResponse<String> resp = ApiResponse.ok("hello");
        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
        assertEquals("hello", resp.getData());
        assertTrue(resp.getTimestamp() > 0);
    }

    @Test
    void ok_shouldAcceptNullData() {
        ApiResponse<Void> resp = ApiResponse.ok(null);
        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
    }

    @Test
    void ok_shouldIncludeTraceId() {
        ApiResponse<String> resp = ApiResponse.ok("data", "trace-123");
        assertEquals("trace-123", resp.getTraceId());
    }

    @Test
    void error_shouldReturnGivenCode() {
        ApiResponse<Void> resp = ApiResponse.error(404, "Not found");
        assertEquals(404, resp.getCode());
        assertEquals("Not found", resp.getMessage());
        assertNull(resp.getData());
    }

    @Test
    void error_shouldIncludeTraceId() {
        ApiResponse<Void> resp = ApiResponse.error(500, "Error", "trace-abc");
        assertEquals("trace-abc", resp.getTraceId());
        assertEquals(500, resp.getCode());
    }

    @Test
    void builder_shouldCreateCompleteResponse() {
        ApiResponse<List<String>> resp = ApiResponse.<List<String>>builder()
                .code(201)
                .message("Created")
                .data(List.of("a", "b"))
                .traceId("t-1")
                .build();

        assertEquals(201, resp.getCode());
        assertEquals("Created", resp.getMessage());
        assertEquals(2, resp.getData().size());
    }

    @Test
    void shouldExcludeNullFields() {
        ApiResponse<String> resp = ApiResponse.ok("x");
        assertNull(resp.getTraceId());
    }
}
