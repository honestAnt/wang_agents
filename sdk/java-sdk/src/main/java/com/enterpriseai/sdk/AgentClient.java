package com.enterpriseai.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for managing agents: create, list, get, debug.
 */
public class AgentClient {

    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public AgentClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : "http://localhost:8080";
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }

    public Map<String, Object> create(String tenantId, String name, String description,
                                       String systemPrompt, String modelId) throws Exception {
        String uri = baseUrl + "/api/agents?tenantId=" + tenantId +
                     "&name=" + name +
                     (description != null ? "&description=" + description : "") +
                     (systemPrompt != null ? "&systemPrompt=" + systemPrompt : "") +
                     (modelId != null ? "&modelId=" + modelId : "");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(uri))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (Map<String, Object>) result.getOrDefault("data", Map.of());
    }

    public List<Map<String, Object>> list(String tenantId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/agents?tenantId=" + tenantId))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (List<Map<String, Object>>) result.getOrDefault("data", List.of());
    }

    public Map<String, Object> get(String agentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(baseUrl + "/api/agents/" + agentId))
                .header("Authorization", "Bearer " + (apiKey != null ? apiKey : ""))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = mapper.readValue(response.body(), new TypeReference<>() {});
        return (Map<String, Object>) result.getOrDefault("data", Map.of());
    }
}
