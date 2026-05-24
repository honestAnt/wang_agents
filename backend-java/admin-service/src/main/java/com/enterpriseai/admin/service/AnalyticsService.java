package com.enterpriseai.admin.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AnalyticsService {

    /**
     * Get platform usage summary: active users, sessions, tokens, cost.
     * In production, aggregates from trace-service and billing-service.
     */
    public Map<String, Object> getUsageSummary(String tenantId, String period) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        summary.put("period", period != null ? period : "today");

        // Placeholder data — in production, queries trace-service / billing-service
        summary.put("activeUsers", 1450);
        summary.put("totalSessions", 8920);
        summary.put("totalRequests", 124500);
        summary.put("totalTokens", 45_600_000L);
        summary.put("totalCost", 234.50);

        summary.put("avgLatencyMs", 320.5);
        summary.put("avgSatisfaction", 4.2);
        summary.put("p95LatencyMs", 1200.0);

        return summary;
    }

    /**
     * Get usage trend data for chart rendering.
     */
    public List<Map<String, Object>> getTrend(String tenantId, String metric, String period) {
        // In production, queries time-series data from trace-service
        List<Map<String, Object>> dataPoints = new ArrayList<>();
        String[] dates = generateDateRange(period);

        for (String date : dates) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);

            switch (metric != null ? metric : "requests") {
                case "tokens" -> point.put("value", 500_000 + new Random().nextInt(200_000));
                case "cost" -> point.put("value", 10.0 + new Random().nextDouble() * 20);
                case "users" -> point.put("value", 100 + new Random().nextInt(50));
                case "sessions" -> point.put("value", 500 + new Random().nextInt(300));
                default -> point.put("value", 5000 + new Random().nextInt(2000));
            }
            dataPoints.add(point);
        }
        return dataPoints;
    }

    /**
     * Top-N rankings: models, tools, skills, users.
     */
    public Map<String, Object> getRankings(String tenantId, String category, int limit) {
        Map<String, Object> rankings = new LinkedHashMap<>();
        rankings.put("tenantId", tenantId);
        rankings.put("category", category != null ? category : "models");

        List<Map<String, Object>> items = switch (category != null ? category : "models") {
            case "models" -> List.of(
                Map.of("name", "gpt-4.1", "count", 45200, "cost", 156.80),
                Map.of("name", "claude-sonnet-4-6", "count", 32100, "cost", 45.20),
                Map.of("name", "deepseek-v3", "count", 28700, "cost", 12.30),
                Map.of("name", "claude-haiku-4-5-20251001", "count", 15600, "cost", 8.40)
            );
            case "tools" -> List.of(
                Map.of("name", "search_kb", "count", 23400),
                Map.of("name", "query_db", "count", 18900),
                Map.of("name", "send_email", "count", 12300),
                Map.of("name", "create_ticket", "count", 8900)
            );
            case "skills" -> List.of(
                Map.of("name", "data_analysis", "count", 15600),
                Map.of("name", "customer_service", "count", 14200),
                Map.of("name", "legal_review", "count", 8700),
                Map.of("name", "code_review", "count", 7600)
            );
            case "users" -> List.of(
                Map.of("name", "user-1", "count", 8900, "cost", 45.20),
                Map.of("name", "user-2", "count", 7600, "cost", 38.10),
                Map.of("name", "user-3", "count", 5400, "cost", 22.50)
            );
            default -> List.of();
        };

        rankings.put("items", items.size() > limit ? items.subList(0, limit) : items);
        rankings.put("total", items.size());
        return rankings;
    }

    /**
     * Quality metrics: satisfaction, tool success rate, RAG hit rate, hallucination rate.
     */
    public Map<String, Object> getQualityMetrics(String tenantId) {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("tenantId", tenantId);

        metrics.put("satisfaction", Map.of("avg", 4.2, "distribution",
                Map.of("5", 45, "4", 30, "3", 15, "2", 7, "1", 3)));

        metrics.put("toolSuccessRate", Map.of("overall", 0.94, "byTool",
                Map.of("search_kb", 0.97, "query_db", 0.95, "send_email", 0.92)));

        metrics.put("ragHitRate", Map.of("overall", 0.82, "top1", 0.68, "top5", 0.89));

        metrics.put("hallucinationRate", 0.03);
        metrics.put("errorRate", 0.02);

        return metrics;
    }

    /**
     * Cost breakdown by model, department, user.
     */
    public Map<String, Object> getCostBreakdown(String tenantId) {
        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("tenantId", tenantId);
        breakdown.put("totalCost", 234.50);

        breakdown.put("byModel", List.of(
                Map.of("model", "gpt-4.1", "cost", 156.80, "percentage", 66.9),
                Map.of("model", "claude-sonnet-4-6", "cost", 45.20, "percentage", 19.3),
                Map.of("model", "deepseek-v3", "cost", 12.30, "percentage", 5.2),
                Map.of("model", "others", "cost", 20.20, "percentage", 8.6)
        ));

        breakdown.put("byDepartment", List.of(
                Map.of("department", "Engineering", "cost", 89.50, "percentage", 38.2),
                Map.of("department", "Product", "cost", 56.30, "percentage", 24.0),
                Map.of("department", "Marketing", "cost", 45.20, "percentage", 19.3),
                Map.of("department", "Finance", "cost", 23.80, "percentage", 10.1),
                Map.of("department", "HR", "cost", 19.70, "percentage", 8.4)
        ));

        breakdown.put("byUser", List.of(
                Map.of("user", "user-1", "cost", 45.20),
                Map.of("user", "user-2", "cost", 38.10),
                Map.of("user", "user-3", "cost", 22.50)
        ));

        // Cost savings: what cheaper models saved
        breakdown.put("savings", Map.of(
                "fromRouting", 45.60,
                "fromFallback", 12.30,
                "totalSaved", 57.90
        ));

        return breakdown;
    }

    /**
     * Generate operational report.
     */
    public Map<String, Object> generateReport(String tenantId, String reportType) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("tenantId", tenantId);
        report.put("type", reportType != null ? reportType : "weekly");
        report.put("generatedAt", Instant.now().toString());

        report.put("summary", getUsageSummary(tenantId, reportType));
        report.put("quality", getQualityMetrics(tenantId));
        report.put("costBreakdown", getCostBreakdown(tenantId));
        report.put("rankings", getRankings(tenantId, "models", 5));

        // Highlights
        report.put("highlights", List.of(
                "Token usage increased 15% week-over-week",
                "claude-haiku-4-5-20251001 usage grew 30%, reducing costs by 12%",
                "RAG hit rate improved from 78% to 82%"
        ));

        return report;
    }

    // ── internal ─────────────────────────────────────────────

    private String[] generateDateRange(String period) {
        int days = switch (period != null ? period : "week") {
            case "today" -> 1;
            case "week" -> 7;
            case "month" -> 30;
            case "quarter" -> 90;
            default -> 7;
        };

        String[] dates = new String[days];
        for (int i = days - 1; i >= 0; i--) {
            dates[days - 1 - i] = java.time.LocalDate.now().minusDays(i).toString();
        }
        return dates;
    }
}
