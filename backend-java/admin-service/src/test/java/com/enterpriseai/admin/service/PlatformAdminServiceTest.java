package com.enterpriseai.admin.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlatformAdminServiceTest {

    private final PlatformAdminService service = new PlatformAdminService();

    @Test
    void getPlatformOverview_shouldReturnHealthStatus() {
        Map<String, Object> result = service.getPlatformOverview();

        assertTrue(result.containsKey("totalTenants"));
        assertTrue(result.containsKey("usage"));
        assertTrue(result.containsKey("healthStatus"));

        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) result.get("healthStatus");
        assertTrue(health.containsKey("gateway"));
        assertTrue(health.containsKey("agent-service"));
    }

    @Test
    void listTenants_shouldReturnTenants() {
        List<Map<String, Object>> tenants = service.listTenants();

        assertFalse(tenants.isEmpty());
        assertTrue(tenants.get(0).containsKey("name"));
        assertTrue(tenants.get(0).containsKey("plan"));
    }

    @Test
    void getTenantDetail_shouldReturnQuotaAndUsage() {
        Map<String, Object> detail = service.getTenantDetail("t1");

        assertEquals("t1", detail.get("tenantId"));
        assertTrue(detail.containsKey("quota"));
        assertTrue(detail.containsKey("usage"));
    }

    @Test
    void getRateLimitConfig_shouldReturnPerProvider() {
        Map<String, Object> config = service.getRateLimitConfig();

        assertTrue(config.containsKey("perProvider"));
        assertTrue(config.containsKey("globalLimits"));
    }

    @Test
    void updateRateLimit_shouldReturnUpdated() {
        Map<String, Object> result = service.updateRateLimit("tenant", "t1", 500);
        assertEquals("updated", result.get("status"));
        assertEquals(500, result.get("maxQps"));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getCircuitBreakerStatus_shouldReturnAllProviders() {
        Map<String, Object> status = service.getCircuitBreakerStatus();

        List providers = (List) status.get("providers");
        assertNotNull(providers);
        assertEquals(5, providers.size());

        Map first = (Map) providers.get(0);
        assertNotNull(first.get("provider"));
    }

    @Test
    void resetCircuitBreaker_shouldReturnClosed() {
        Map<String, Object> result = service.resetCircuitBreaker("qwen");
        assertEquals("qwen", result.get("provider"));
        assertEquals("closed", result.get("status"));
    }

    @Test
    void createAnnouncement_shouldReturnPublished() {
        Map<String, Object> result = service.createAnnouncement(
                "Maintenance", "Scheduled downtime", "warning", "all");
        assertEquals("Maintenance", result.get("title"));
        assertEquals("published", result.get("status"));
        assertNotNull(result.get("id"));
    }

    @Test
    void listAnnouncements_shouldReturnList() {
        List<Map<String, Object>> announcements = service.listAnnouncements();
        assertEquals(2, announcements.size());
    }
}
