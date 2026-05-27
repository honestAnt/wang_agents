package com.enterpriseai.common.trace;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Servlet filter that creates an OpenTelemetry span for every HTTP request.
 *
 * Extracts W3C trace context from incoming headers (traceparent, tracestate)
 * so the trace chain propagates across services. Also reads our own X-Trace-Id
 * header for compatibility with Python agent and Feign interceptor.
 */
public class TraceFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TENANT_HEADER = "X-Tenant-Id";

    private static final List<String> IGNORED_PATHS = List.of(
            "/actuator/health", "/actuator/info", "/actuator/metrics",
            "/health", "/metrics", "/favicon.ico"
    );

    private final Tracer tracer;

    public TraceFilter() {
        this.tracer = GlobalOpenTelemetry.getTracer("enterprise-ai-java");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        String path = httpReq.getRequestURI();

        if (isIgnored(path)) {
            chain.doFilter(request, response);
            return;
        }

        // Extract parent context from W3C headers or our custom X-Trace-Id
        Context parentContext = extractContext(httpReq);

        // Create span
        String spanName = httpReq.getMethod() + " " + path;
        Span span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        // Set span attributes
        span.setAttribute("http.method", httpReq.getMethod());
        span.setAttribute("http.url", httpReq.getRequestURL().toString());
        span.setAttribute("http.status_code", httpResp.getStatus());

        String tenantId = httpReq.getHeader(TENANT_HEADER);
        if (tenantId != null) {
            span.setAttribute("tenant.id", tenantId);
            TraceContext.init(TraceContext.getOrCreateTraceId());
        }

        String traceId = httpReq.getHeader(TRACE_ID_HEADER);
        if (traceId != null) {
            span.setAttribute("x.trace_id", traceId);
            TraceContext.init(traceId);
        } else {
            TraceContext.init(span.getSpanContext().getTraceId());
        }

        // Propagate to TraceContext ThreadLocal for downstream Feign/Kafka
        try (Scope ignored = span.makeCurrent()) {
            chain.doFilter(request, response);
            span.setAttribute("http.status_code", httpResp.getStatus());
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.recordException(e);
            throw e;
        } finally {
            if (httpResp.getStatus() >= 400) {
                span.setStatus(StatusCode.ERROR);
            }
            span.end();
            TraceContext.clear();
        }
    }

    private Context extractContext(HttpServletRequest request) {
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator()
                .extract(Context.current(), request, new HttpHeaderGetter());
    }

    private boolean isIgnored(String path) {
        return IGNORED_PATHS.stream().anyMatch(path::equals);
    }

    private static class HttpHeaderGetter implements TextMapGetter<HttpServletRequest> {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            if (carrier == null) return null;
            return carrier.getHeader(key);
        }
    }
}
