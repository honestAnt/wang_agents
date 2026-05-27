package com.enterpriseai.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsServiceTest {

    private final AnalyticsService service = new AnalyticsService(
            new RestTemplateBuilder(), "http://localhost:8085", "http://localhost:8087");

    @Test
    void getUsageSummary_shouldReturnAllFields() {
        Map<String, Object> result = service.getUsageSummary("t1", "today");

        assertNotNull(result.get("activeUsers"));
        assertNotNull(result.get("totalSessions"));
        assertNotNull(result.get("totalTokens"));
        assertNotNull(result.get("totalCost"));
        assertEquals("t1", result.get("tenantId"));
    }

    @Test
    void getTrend_shouldReturnCorrectNumberOfPoints() {
        List<Map<String, Object>> result = service.getTrend("t1", "requests", "week");
        assertEquals(7, result.size());
    }

    @Test
    void getTrend_month_returns30Days() {
        List<Map<String, Object>> result = service.getTrend("t1", "tokens", "month");
        assertEquals(30, result.size());
    }

    @Test
    void getRankings_models() {
        Map<String, Object> result = service.getRankings("t1", "models", 3);
        assertTrue(result.containsKey("items"));
        assertTrue(result.containsKey("total"));
    }

    @Test
    void getRankings_tools() {
        Map<String, Object> result = service.getRankings("t1", "tools", 5);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertNotNull(items);
    }

    @Test
    void getQualityMetrics_shouldReturnAllCategories() {
        Map<String, Object> result = service.getQualityMetrics("t1");

        assertTrue(result.containsKey("satisfaction"));
        assertTrue(result.containsKey("toolSuccessRate"));
        assertTrue(result.containsKey("ragHitRate"));
        assertTrue(result.containsKey("hallucinationRate"));
        assertTrue(result.containsKey("errorRate"));
    }

    @Test
    void getCostBreakdown_shouldIncludeAllDimensions() {
        Map<String, Object> result = service.getCostBreakdown("t1");

        assertTrue(result.containsKey("byModel"));
        assertTrue(result.containsKey("byDepartment"));
        assertTrue(result.containsKey("byUser"));
        assertTrue(result.containsKey("savings"));
    }

    @Test
    void generateReport_shouldCombineAllSections() {
        Map<String, Object> result = service.generateReport("t1", "weekly");

        assertTrue(result.containsKey("summary"));
        assertTrue(result.containsKey("quality"));
        assertTrue(result.containsKey("costBreakdown"));
        assertTrue(result.containsKey("highlights"));
    }
}
