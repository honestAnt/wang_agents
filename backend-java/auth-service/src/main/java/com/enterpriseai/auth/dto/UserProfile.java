package com.enterpriseai.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfile {

    private String userId;
    private String username;
    private String email;
    private String displayName;
    private String tenantId;
    private String tenantName;
    private List<String> roles;
    private List<String> permissions;
}
