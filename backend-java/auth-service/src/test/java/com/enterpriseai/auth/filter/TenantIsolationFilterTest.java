package com.enterpriseai.auth.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class TenantIsolationFilterTest {

    private TenantIsolationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TenantIsolationFilter();
    }

    @Test
    void shouldPropagateTraceIdHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "trace-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertEquals("trace-123", response.getHeader("X-Trace-Id"));
    }

    @Test
    void shouldGenerateTraceIdWhenMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});

        assertNotNull(response.getHeader("X-Trace-Id"));
        assertEquals(36, response.getHeader("X-Trace-Id").length());
    }

    @Test
    void shouldExtractTenantIdFromHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {});
        // Filter should not throw; tenant ID is used downstream
        assertNotNull(response.getHeader("X-Trace-Id"));
    }
}
