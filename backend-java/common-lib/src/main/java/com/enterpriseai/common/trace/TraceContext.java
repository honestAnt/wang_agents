package com.enterpriseai.common.trace;

import java.util.UUID;

/**
 * ThreadLocal holder for the current trace context.
 * Automatically propagated through Feign interceptor and Kafka headers.
 */
public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();

    private TraceContext() {}

    public static void init() {
        TRACE_ID.set(UUID.randomUUID().toString());
        SPAN_ID.set(UUID.randomUUID().toString());
    }

    public static void init(String traceId) {
        TRACE_ID.set(traceId);
        SPAN_ID.set(UUID.randomUUID().toString());
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static String getSpanId() {
        return SPAN_ID.get();
    }

    public static String getOrCreateTraceId() {
        String id = TRACE_ID.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            TRACE_ID.set(id);
        }
        return id;
    }

    public static String newSpanId() {
        String id = UUID.randomUUID().toString();
        SPAN_ID.set(id);
        return id;
    }

    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
    }
}
