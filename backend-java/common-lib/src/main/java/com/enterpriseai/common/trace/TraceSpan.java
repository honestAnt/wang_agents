package com.enterpriseai.common.trace;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceSpan {

    @Builder.Default
    private String traceId = UUID.randomUUID().toString();

    @Builder.Default
    private String spanId = UUID.randomUUID().toString();

    private String parentId;

    private String type; // llm, tool, rag, memory, agent, skill, api, workflow

    private String name;

    private String tenantId;

    private String userId;

    private String agentId;

    private String sessionId;

    @Builder.Default
    private String startedAt = Instant.now().toString();

    private String endedAt;

    private Double latencyMs;

    @Builder.Default
    private String status = "ok";

    private String errorMessage;

    private Map<String, Object> input;

    private Map<String, Object> output;

    private SpanMetadata metadata;

    private List<String> tags;

    public void end() {
        this.endedAt = Instant.now().toString();
        if (this.startedAt != null) {
            this.latencyMs = (double) (Instant.now().toEpochMilli()
                    - Instant.parse(this.startedAt).toEpochMilli());
        }
    }

    public void endWithError(String errorMessage) {
        this.status = "error";
        this.errorMessage = errorMessage;
        end();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpanMetadata {
        private String model;
        private String provider;
        private Double costUsd;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private String toolName;
        private String toolType;
        private String kbId;
        private Integer chunksCount;
        private Double rerankScore;
        private String memoryType;
        private String skillName;
    }
}
