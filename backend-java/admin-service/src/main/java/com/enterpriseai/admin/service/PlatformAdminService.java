package com.enterpriseai.admin.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class PlatformAdminService {

    // ── Global Monitoring ────────────────────────────────────

    public Map<String, Object> getPlatformOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();

        // Aggregate across all tenants
        overview.put("totalTenants", 42);
        overview.put("activeTenants24h", 38);
        overview.put("totalUsers", 12500);
        overview.put("totalAgents", 320);

        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("totalRequests24h", 2_450_000);
        usage.put("totalTokens24h", 890_000_000L);
        usage.put("totalCost24h", 3456.78);
        usage.put("avgLatencyMs", 285.3);
        usage.put("errorRate24h", 0.015);
        overview.put("usage", usage);

        overview.put("healthStatus", Map.of(
                "gateway", "healthy",
                "auth-service", "healthy",
                "agent-service", "healthy",
                "model-service", "degraded",
                "rag-service", "healthy",
                "tool-service", "healthy",
                "trace-service", "healthy"
        ));

        return overview;
    }

    // ── Tenant Management ────────────────────────────────────

    public List<Map<String, Object>> listTenants() {
        return List.of(
                createTenantSummary("t1", "Acme Corp", "enterprise", "active", 2340, 45, 450.00),
                createTenantSummary("t2", "Globex Inc", "business", "active", 890, 23, 123.50),
                createTenantSummary("t3", "Initech", "starter", "suspended", 45, 3, 2.30),
                createTenantSummary("t4", "Umbrella Corp", "enterprise", "active", 5670, 89, 1200.00)
        );
    }

    public Map<String, Object> getTenantDetail(String tenantId) {
        return Map.of(
                "tenantId", tenantId,
                "name", "Acme Corp",
                "plan", "enterprise",
                "status", "active",
                "createdAt", "2026-01-15T08:00:00Z",
                "quota", Map.of(
                        "maxUsers", 5000,
                        "maxAgents", 100,
                        "maxTokensPerDay", 10_000_000L,
                        "maxCostPerMonth", 5000.00
                ),
                "usage", Map.of(
                        "currentUsers", 2340,
                        "currentAgents", 45,
                        "tokensToday", 4_500_000L,
                        "costThisMonth", 2340.50
                )
        );
    }

    // ── Rate Limiting ────────────────────────────────────────

    public Map<String, Object> getRateLimitConfig() {
        return Map.of(
                "globalLimits", Map.of(
                        "maxQpsPerTenant", 1000,
                        "maxQpsPerUser", 100,
                        "maxConcurrentSessions", 5000,
                        "burstMultiplier", 2.0
                ),
                "perProvider", List.of(
                        Map.of("provider", "openai", "maxQps", 500, "currentQps", 320),
                        Map.of("provider", "anthropic", "maxQps", 300, "currentQps", 180),
                        Map.of("provider", "deepseek", "maxQps", 200, "currentQps", 95),
                        Map.of("provider", "qwen", "maxQps", 150, "currentQps", 60)
                ),
                "perEndpoint", List.of(
                        Map.of("endpoint", "/api/chat", "maxQps", 800, "currentQps", 520),
                        Map.of("endpoint", "/api/rag/search", "maxQps", 300, "currentQps", 145)
                )
        );
    }

    public Map<String, Object> updateRateLimit(String scope, String key, int maxQps) {
        return Map.of("scope", scope, "key", key, "maxQps", maxQps, "status", "updated");
    }

    // ── Provider Circuit Breaker ─────────────────────────────

    public Map<String, Object> getCircuitBreakerStatus() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("providers", List.of(
                createProviderStatus("openai", "closed", 0.01, "N/A", 0),
                createProviderStatus("anthropic", "half_open", 0.05, "2026-05-24T10:30:00Z", 2),
                createProviderStatus("deepseek", "closed", 0.02, "N/A", 0),
                createProviderStatus("qwen", "open", 0.15, "2026-05-24T11:00:00Z", 0),
                createProviderStatus("gemini", "closed", 0.005, "N/A", 0)
        ));

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("failureThreshold", 0.10);
        config.put("halfOpenMaxRequests", 5);
        config.put("openStateDurationSeconds", 60);
        config.put("successThresholdToClose", 3);
        result.put("globalConfig", config);

        return result;
    }

    private Map<String, Object> createProviderStatus(String provider, String status,
                                                      double failureRate, String lastFailure,
                                                      int halfOpenAttempts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("provider", provider);
        m.put("status", status);
        m.put("failureRate", failureRate);
        m.put("lastFailure", lastFailure);
        m.put("halfOpenAttempts", halfOpenAttempts);
        return m;
    }

    public Map<String, Object> resetCircuitBreaker(String provider) {
        return Map.of("provider", provider, "status", "closed", "action", "reset");
    }

    // ── Announcements ────────────────────────────────────────

    public Map<String, Object> createAnnouncement(String title, String content,
                                                   String level, String targetTenants) {
        return Map.of(
                "id", UUID.randomUUID().toString(),
                "title", title,
                "content", content,
                "level", level != null ? level : "info",
                "targetTenants", targetTenants,
                "createdAt", Instant.now().toString(),
                "status", "published"
        );
    }

    public List<Map<String, Object>> listAnnouncements() {
        return List.of(
                Map.of("id", "ann-1", "title", "Scheduled Maintenance",
                        "level", "warning", "createdAt", "2026-05-23T08:00:00Z"),
                Map.of("id", "ann-2", "title", "New Claude Sonnet model available",
                        "level", "info", "createdAt", "2026-05-20T14:00:00Z")
        );
    }

    // ── internal ─────────────────────────────────────────────

    private Map<String, Object> createTenantSummary(String id, String name, String plan,
                                                     String status, int users, int agents, double cost) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("name", name);
        t.put("plan", plan);
        t.put("status", status);
        t.put("users", users);
        t.put("agents", agents);
        t.put("costThisMonth", cost);
        return t;
    }
}
