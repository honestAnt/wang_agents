package com.enterpriseai.common.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

/**
 * RestTemplate interceptor that creates spans for outbound HTTP calls
 * and propagates W3C trace context + custom headers to downstream services.
 */
public class RestTemplateTraceInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RestTemplateTraceInterceptor.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private final Tracer tracer;

    public RestTemplateTraceInterceptor() {
        this.tracer = GlobalOpenTelemetry.getTracer("enterprise-ai-java");
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                         ClientHttpRequestExecution execution) throws IOException {

        URI uri = request.getURI();
        Span span = tracer.spanBuilder("HTTP " + request.getMethod() + " " + uri.getHost() + uri.getPath())
                .setSpanKind(SpanKind.CLIENT)
                .startSpan();

        span.setAttribute("http.method", request.getMethod().name());
        span.setAttribute("http.url", uri.toString());
        span.setAttribute("peer.hostname", uri.getHost());
        span.setAttribute("peer.port", uri.getPort());

        // Propagate trace context via W3C headers
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .inject(Context.current(), request, new HttpHeaderSetter());

        // Also inject our custom headers
        String traceId = TraceContext.getTraceId();
        if (traceId != null) {
            request.getHeaders().set(TRACE_ID_HEADER, traceId);
        }

        String tenantId = getTenantId();
        if (tenantId != null) {
            request.getHeaders().set(TENANT_HEADER, tenantId);
        }

        try (Scope ignored = span.makeCurrent()) {
            ClientHttpResponse response = execution.execute(request, body);
            span.setAttribute("http.status_code", response.getStatusCode().value());
            if (response.getStatusCode().isError()) {
                span.setStatus(StatusCode.ERROR);
            }
            return response;
        } catch (IOException e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    private String getTenantId() {
        try {
            com.enterpriseai.common.auth.JwtContext ctx =
                    com.enterpriseai.common.auth.SecurityContextHolder.get();
            if (ctx != null) return ctx.getTenantId();
        } catch (Exception ignored) {}
        return null;
    }

    private static class HttpHeaderSetter implements TextMapSetter<HttpRequest> {
        @Override
        public void set(HttpRequest carrier, String key, String value) {
            carrier.getHeaders().set(key, value);
        }
    }
}
