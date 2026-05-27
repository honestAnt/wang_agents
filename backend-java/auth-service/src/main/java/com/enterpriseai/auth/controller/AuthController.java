package com.enterpriseai.auth.controller;

import com.enterpriseai.auth.dto.LoginRequest;
import com.enterpriseai.auth.dto.TokenResponse;
import com.enterpriseai.auth.dto.UserProfile;
import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.common.auth.SecurityContextHolder;
import com.enterpriseai.common.exception.BusinessException;
import com.enterpriseai.user.entity.User;
import com.enterpriseai.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final RestTemplate restTemplate;
    private final UserService userService;

    @Value("${keycloak.realm:wang-agent}")
    private String realm;

    @Value("${keycloak.client-id:enterprise-ai-client}")
    private String clientId;

    @Value("${keycloak.client-secret:FFQvKPO9bQKBwkNuYlI6gkYiYvNa3clO}")
    private String clientSecret;

    @Value("${keycloak.auth-server-url:http://localhost:8080}")
    private String keycloakUrl;

    public AuthController(RestTemplateBuilder restTemplateBuilder, UserService userService) {
        this.restTemplate = restTemplateBuilder.build();
        this.userService = userService;
    }

    private String tokenEndpoint() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    private String logoutEndpoint() {
        return keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/logout";
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", request.getUsername());
            body.add("password", request.getPassword());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    tokenEndpoint(), entity, Map.class);

            if (tokenResponse == null) {
                throw new BusinessException(502, "Keycloak returned null response", HttpStatus.BAD_GATEWAY);
            }

            TokenResponse token = TokenResponse.builder()
                    .accessToken((String) tokenResponse.get("access_token"))
                    .refreshToken((String) tokenResponse.get("refresh_token"))
                    .tokenType((String) tokenResponse.get("token_type"))
                    .expiresIn(((Number) tokenResponse.get("expires_in")).intValue())
                    .refreshExpiresIn(((Number) tokenResponse.getOrDefault("refresh_expires_in", 1800)).intValue())
                    .scope((String) tokenResponse.getOrDefault("scope", "openid"))
                    .build();

            // Look up user in local database to get tenant_id
            try {
                User user = userService.getByUsername(request.getUsername());
                token.setTenantId(user.getTenantId());
                token.setUsername(user.getUsername());
                token.setEmail(user.getEmail());
                token.setDisplayName(user.getDisplayName());
            } catch (Exception e) {
                log.warn("User '{}' not found in local database, tenant_id will be empty", request.getUsername());
            }

            return ApiResponse.ok(token);
        } catch (RestClientException e) {
            log.error("Keycloak login failed: {}", e.getMessage());
            throw BusinessException.unauthorized("Invalid credentials");
        }
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String userId = SecurityContextHolder.getUserId();
        log.info("Logout for user: {}", userId);

        try {
            restTemplate.postForEntity(logoutEndpoint(), null, Void.class);
        } catch (Exception e) {
            log.warn("Keycloak logout call failed (session may already be expired): {}", e.getMessage());
        }

        return ApiResponse.ok(null);
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw BusinessException.badRequest("refresh_token is required");
        }

        log.info("Token refresh requested");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
            formBody.add("grant_type", "refresh_token");
            formBody.add("client_id", clientId);
            formBody.add("client_secret", clientSecret);
            formBody.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restTemplate.postForObject(
                    tokenEndpoint(), entity, Map.class);

            if (tokenResponse == null) {
                throw new BusinessException(502, "Keycloak returned null response", HttpStatus.BAD_GATEWAY);
            }

            TokenResponse token = TokenResponse.builder()
                    .accessToken((String) tokenResponse.get("access_token"))
                    .refreshToken((String) tokenResponse.get("refresh_token"))
                    .expiresIn(((Number) tokenResponse.get("expires_in")).intValue())
                    .refreshExpiresIn(((Number) tokenResponse.getOrDefault("refresh_expires_in", 1800)).intValue())
                    .build();

            return ApiResponse.ok(token);
        } catch (RestClientException e) {
            log.error("Keycloak refresh failed: {}", e.getMessage());
            throw BusinessException.unauthorized("Invalid or expired refresh token");
        }
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
