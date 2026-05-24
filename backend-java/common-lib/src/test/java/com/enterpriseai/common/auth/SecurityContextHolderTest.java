package com.enterpriseai.common.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHolderTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clear();
    }

    @Test
    void shouldStoreAndRetrieveContext() {
        JwtContext ctx = JwtContext.builder()
                .userId("u1")
                .tenantId("t1")
                .roles(List.of("Admin"))
                .build();
        SecurityContextHolder.set(ctx);

        assertEquals("u1", SecurityContextHolder.getUserId());
        assertEquals("t1", SecurityContextHolder.getTenantId());
        assertEquals("u1", SecurityContextHolder.get().getUserId());
    }

    @Test
    void shouldReturnNullWhenNotSet() {
        assertNull(SecurityContextHolder.get());
        assertNull(SecurityContextHolder.getUserId());
        assertNull(SecurityContextHolder.getTenantId());
        assertNull(SecurityContextHolder.getUsername());
    }

    @Test
    void shouldIsolateBetweenThreads() throws Exception {
        SecurityContextHolder.set(JwtContext.builder().userId("main").build());

        Thread t = new Thread(() -> {
            assertNull(SecurityContextHolder.getUserId());
            SecurityContextHolder.set(JwtContext.builder().userId("child").build());
        });
        t.start();
        t.join();

        // Main thread should still have its own context
        assertEquals("main", SecurityContextHolder.getUserId());
    }

    @Test
    void clear_shouldRemoveContext() {
        SecurityContextHolder.set(JwtContext.builder().userId("u").build());
        SecurityContextHolder.clear();
        assertNull(SecurityContextHolder.get());
    }
}
