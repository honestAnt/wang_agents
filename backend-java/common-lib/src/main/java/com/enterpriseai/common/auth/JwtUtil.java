package com.enterpriseai.common.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility for parsing JWT tokens issued by Keycloak.
 * Does NOT validate signatures by default — the API Gateway handles that.
 * This class extracts claims for internal service-to-service context propagation.
 */
public final class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private JwtUtil() {}

    public static JwtContext parse(String token, PublicKey publicKey) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return buildContext(claims);
        } catch (JwtException e) {
            log.warn("JWT parse failed: {}", e.getMessage());
            throw new RuntimeException("Invalid JWT token", e);
        }
    }

    /**
     * Parse a JWT without signature verification — intended for use behind the
     * API Gateway where the token has already been validated.
     */
    public static JwtContext parseUnsigned(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid JWT format");
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> map = mapper.readValue(payload, Map.class);

            String userId = (String) map.getOrDefault("sub", "");
            String username = (String) map.getOrDefault("preferred_username", userId);
            String tenantId = (String) map.getOrDefault("tenant_id", "");

            @SuppressWarnings("unchecked")
            List<String> roles = map.containsKey("realm_access")
                    ? (List<String>) ((Map<String, Object>) map.get("realm_access")).getOrDefault("roles", Collections.emptyList())
                    : Collections.emptyList();

            return JwtContext.builder()
                    .userId(userId)
                    .username(username)
                    .tenantId(tenantId)
                    .roles(roles)
                    .claims(map)
                    .build();
        } catch (Exception e) {
            log.error("Failed to parse JWT payload", e);
            throw new RuntimeException("Failed to parse JWT payload", e);
        }
    }

    private static JwtContext buildContext(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("realm_access") != null
                ? (List<String>) ((Map<String, Object>) claims.get("realm_access"))
                    .getOrDefault("roles", Collections.emptyList())
                : Collections.emptyList();

        return JwtContext.builder()
                .userId(claims.getSubject())
                .username((String) claims.getOrDefault("preferred_username", claims.getSubject()))
                .tenantId((String) claims.getOrDefault("tenant_id", ""))
                .roles(roles)
                .claims(claims)
                .build();
    }

    public static PublicKey loadPublicKey(String pemEncoded) throws Exception {
        String pem = pemEncoded
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
