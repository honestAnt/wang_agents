package com.enterpriseai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantCreateRequest {

    @NotBlank @Size(max = 128)
    private String name;

    @Size(max = 256)
    private String domain;

    private String plan = "free";
    private Integer maxUsers = 50;
    private Integer maxAgents = 10;
}
