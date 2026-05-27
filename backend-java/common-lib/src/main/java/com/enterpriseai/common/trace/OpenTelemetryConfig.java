package com.enterpriseai.common.trace;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.common.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OpenTelemetry SDK configuration — OTLP gRPC export to Jaeger.
 *
 * Env vars:
 *   JAEGER_ENDPOINT     — OTLP gRPC endpoint (default: http://localhost:4317)
 *   JAEGER_SERVICE_NAME — service name in traces (default: auto-detected)
 *   JAEGER_INSECURE     — use plaintext (default: true)
 */
@Configuration
public class OpenTelemetryConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenTelemetryConfig.class);

    @Bean
    public OpenTelemetry openTelemetry(
            @Value("${jaeger.endpoint:http://localhost:4317}") String endpoint,
            @Value("${jaeger.service-name:#{null}}") String serviceName,
            @Value("${jaeger.insecure:true}") boolean insecure,
            @Value("${spring.application.name:unknown-service}") String appName) {

        String svc = serviceName != null ? serviceName : appName;

        OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(endpoint)
                .setTimeout(10, TimeUnit.SECONDS)
                .build();

        Resource resource = Resource.create(
                Attributes.of(AttributeKey.stringKey("service.name"), svc));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter)
                        .setMaxQueueSize(2048)
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(5, TimeUnit.SECONDS)
                        .build())
                .build();

        OpenTelemetry otel = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal();

        log.info("OpenTelemetry initialized: service={}, endpoint={}", svc, endpoint);
        return otel;
    }
}
