package com.enterpriseai.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/**
 * Client for querying trace data: sessions, spans, cost analysis.
 */
public class TraceClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public TraceClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : "http://localhost:8080";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public List<Map<String, Object>> listSessions(String tenantId, String userId,
                                                    String agentId, int limit) throws Exception {
        StringBuilder uri = new StringBuilder(baseUrl + "/api/trace/sessions?tenantId=" + tenantId +
                                               "&limit=" + limit);
        if (userId != null) uri.append("&userId=").append(userId);
        if (agentId != null) uri.append("&agentId=").append(agentId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri.toString()))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (List<Map<String, Object>>) result.getOrDefault("data", List.of());
    }

    public Map<String, Object> getSession(String sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/trace/sessions/" + sessionId))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (Map<String, Object>) result.getOrDefault("data", Map.of());
    }

    public Map<String, Object> getCostSummary(String tenantId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/trace/cost-summary?tenantId=" + tenantId))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (Map<String, Object>) result.getOrDefault("data", Map.of());
    }
}
