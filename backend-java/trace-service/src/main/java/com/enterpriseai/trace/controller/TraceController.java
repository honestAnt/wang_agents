package com.enterpriseai.trace.controller;

import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    @PostMapping("/spans")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Map<String, Integer>> ingestSpans(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> spans = (List<Map<String, Object>>) body.get("spans");
        int accepted = spans != null ? spans.size() : 0;
        return ApiResponse.ok(Map.of("accepted", accepted, "rejected", 0));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<List<Map<String, Object>>> listSessions(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return ApiResponse.ok(List.of());
    }

    @GetMapping("/sessions/{sessionId}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Map<String, Object>> getSessionDetail(@PathVariable String sessionId) {
        return ApiResponse.ok(Map.of("id", sessionId));
    }

    @GetMapping("/costs")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<List<Map<String, Object>>> getCostAnalytics(
            @RequestParam(required = false) String groupBy) {
        return ApiResponse.ok(List.of());
    }
}
