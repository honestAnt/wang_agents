package com.enterpriseai.common.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TraceContextTest {

    @AfterEach
    void tearDown() {
        TraceContext.clear();
    }

    @Test
    void init_shouldGenerateIds() {
        TraceContext.init();
        assertNotNull(TraceContext.getTraceId());
        assertNotNull(TraceContext.getSpanId());
        assertEquals(36, TraceContext.getTraceId().length());
    }

    @Test
    void init_shouldAcceptExistingTraceId() {
        TraceContext.init("custom-trace-id");
        assertEquals("custom-trace-id", TraceContext.getTraceId());
        assertNotNull(TraceContext.getSpanId());
    }

    @Test
    void getOrCreate_shouldAlwaysReturnId() {
        String id = TraceContext.getOrCreateTraceId();
        assertNotNull(id);
        assertEquals(id, TraceContext.getOrCreateTraceId());
    }

    @Test
    void newSpanId_shouldGenerateNewId() {
        TraceContext.init();
        String span1 = TraceContext.getSpanId();
        String span2 = TraceContext.newSpanId();
        assertNotNull(span2);
        assertNotEquals(span1, span2);
    }

    @Test
    void clear_shouldRemoveIds() {
        TraceContext.init();
        TraceContext.clear();
        assertNull(TraceContext.getTraceId());
        assertNull(TraceContext.getSpanId());
    }

    @Test
    void shouldIsolateBetweenThreads() throws Exception {
        TraceContext.init("main-trace");
        Thread t = new Thread(() -> {
            assertNull(TraceContext.getTraceId());
            TraceContext.init("child-trace");
        });
        t.start();
        t.join();
        assertEquals("main-trace", TraceContext.getTraceId());
    }
}
