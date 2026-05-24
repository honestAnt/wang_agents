package com.enterpriseai.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEvent {

    @Builder.Default
    private String eventId = UUID.randomUUID().toString();

    private String traceId;
    private String spanId;
    private String tenantId;
    private String userId;
    private String eventType;
    private String payloadJson;

    @Builder.Default
    private String timestamp = Instant.now().toString();
}
