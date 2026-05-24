package com.enterpriseai.auth.controller;

import com.enterpriseai.auth.dto.LoginRequest;
import com.enterpriseai.auth.dto.TokenResponse;
import com.enterpriseai.auth.dto.UserProfile;
import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.common.auth.SecurityContextHolder;
import com.enterpriseai.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /**
     * Login — in production this would delegate to Keycloak's token endpoint.
     * For local development / MVP, we return mock tokens.
     */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        // In production, call Keycloak token endpoint:
        // POST {keycloak-url}/realms/{realm}/protocol/openid-connect/token
        // with grant_type=password, client_id, client_secret, username, password

        TokenResponse token = TokenResponse.builder()
                .accessToken("placeholder-jwt-token")
                .refreshToken("placeholder-refresh-token")
                .tokenType("Bearer")
                .expiresIn(300)
                .refreshExpiresIn(1800)
                .scope("openid profile email")
                .build();

        return ApiResponse.ok(token);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String userId = SecurityContextHolder.getUserId();
        log.info("Logout for user: {}", userId);
        // In production, call Keycloak logout endpoint to invalidate session
        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.badRequest("refresh_token is required");
        }

        log.info("Token refresh requested");

        // In production, call Keycloak token endpoint with grant_type=refresh_token
        TokenResponse token = TokenResponse.builder()
                .accessToken("placeholder-new-jwt-token")
                .refreshToken("placeholder-new-refresh-token")
                .expiresIn(300)
                .refreshExpiresIn(1800)
                .build();

        return ApiResponse.ok(token);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserProfile> me(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        List<String> roles = Collections.emptyList();
        if (realmAccess != null && realmAccess.get("roles") != null) {
            roles = (List<String>) realmAccess.get("roles");
        }

        UserProfile profile = UserProfile.builder()
                .userId(jwt.getSubject())
                .username(jwt.getClaimAsString("preferred_username"))
                .email(jwt.getClaimAsString("email"))
                .displayName(jwt.getClaimAsString("name"))
                .tenantId(jwt.getClaimAsString("tenant_id"))
                .roles(roles)
                .build();

        return ApiResponse.ok(profile);
    }
}
