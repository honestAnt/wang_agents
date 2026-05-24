package com.enterpriseai.sdk;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Chat client for sending messages and streaming SSE responses.
 */
public class ChatClient {

    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChatClient(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/$", "") : "http://localhost:8080";
        this.apiKey = apiKey;
    }

    /**
     * Send a chat message and stream SSE responses to the callback.
     */
    public void chat(String tenantId, String message, String sessionId,
                     String agentId, String model, Consumer<String> onChunk) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("tenant_id", tenantId);
        body.put("message", message);
        if (sessionId != null) body.put("session_id", sessionId);
        if (agentId != null) body.put("agent_id", agentId);
        if (model != null) body.put("model", model);

        HttpURLConnection conn = post("/api/v1/chat", body);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    onChunk.accept(line.substring(6));
                }
            }
        }
    }

    private HttpURLConnection post(String path, Object body) throws Exception {
        URL url = new URI(baseUrl + path).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null) conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        String json = mapper.writeValueAsString(body);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }
}
