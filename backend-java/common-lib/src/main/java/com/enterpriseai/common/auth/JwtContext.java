package com.enterpriseai.common.auth;

import lombok.Builder;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class JwtContext {

    private String userId;
    private String username;
    private String tenantId;

    @Builder.Default
    private List<String> roles = Collections.emptyList();

    @Builder.Default
    private Map<String, Object> claims = Collections.emptyMap();

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... roles) {
        for (String r : roles) {
            if (this.roles.contains(r)) return true;
        }
        return false;
    }
}
