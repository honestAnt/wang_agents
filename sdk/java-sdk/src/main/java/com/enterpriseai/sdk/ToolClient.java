package com.enterpriseai.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for executing tools via the tool registry.
 */
public class ToolClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public ToolClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : "http://localhost:8080";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> execute(String tenantId, String toolName,
                                        Map<String, Object> parameters) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tenant_id", tenantId);
        body.put("tool_name", toolName);
        body.put("parameters", parameters != null ? parameters : Map.of());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/tools/execute"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Tool execution failed: " + response.body());
        }
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (Map<String, Object>) result.getOrDefault("data", Map.of());
    }

    public List<Map<String, Object>> list(String tenantId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/tools?tenantId=" + tenantId))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (List<Map<String, Object>>) result.getOrDefault("data", List.of());
    }
}
