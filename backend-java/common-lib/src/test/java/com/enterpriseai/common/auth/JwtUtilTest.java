package com.enterpriseai.common.auth;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void parseUnsigned_shouldExtractClaims() {
        // Build a minimal JWT payload
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString(
                "{\"sub\":\"user-1\",\"preferred_username\":\"alice\",\"tenant_id\":\"t-1\",\"realm_access\":{\"roles\":[\"Developer\"]}}".getBytes());
        String token = header + "." + payload + ".signature";

        JwtContext ctx = JwtUtil.parseUnsigned(token);

        assertEquals("user-1", ctx.getUserId());
        assertEquals("alice", ctx.getUsername());
        assertEquals("t-1", ctx.getTenantId());
        assertTrue(ctx.getRoles().contains("Developer"));
    }

    @Test
    void parseUnsigned_shouldHandleMissingClaims() {
        String header = Base64.getUrlEncoder().encodeToString("{\"alg\":\"RS256\"}".getBytes());
        String payload = Base64.getUrlEncoder().encodeToString("{\"sub\":\"minimal\"}".getBytes());
        String token = header + "." + payload + ".x";

        JwtContext ctx = JwtUtil.parseUnsigned(token);

        assertEquals("minimal", ctx.getUserId());
        assertEquals("minimal", ctx.getUsername()); // falls back to sub
        assertEquals("", ctx.getTenantId());
        assertTrue(ctx.getRoles().isEmpty());
    }

    @Test
    void parseUnsigned_shouldRejectMalformedToken() {
        assertThrows(RuntimeException.class, () -> JwtUtil.parseUnsigned("not.a.jwt.token"));
    }

    @Test
    void hasRole_shouldCheckRolePresence() {
        JwtContext ctx = JwtContext.builder()
                .roles(java.util.List.of("Admin", "User"))
                .build();

        assertTrue(ctx.hasRole("Admin"));
        assertFalse(ctx.hasRole("SuperAdmin"));
    }

    @Test
    void hasAnyRole_shouldMatchAny() {
        JwtContext ctx = JwtContext.builder()
                .roles(java.util.List.of("Developer"))
                .build();

        assertTrue(ctx.hasAnyRole("Admin", "Developer"));
        assertFalse(ctx.hasAnyRole("Admin", "SuperAdmin"));
    }
}
