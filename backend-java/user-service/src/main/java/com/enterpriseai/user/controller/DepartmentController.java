package com.enterpriseai.user.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.user.dto.DepartmentNode;
import com.enterpriseai.user.service.DepartmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/tree")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<DepartmentNode>> tree(@RequestParam String tenantId) {
        return ApiResponse.ok(departmentService.getTree(tenantId));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<?> create(@RequestParam String tenantId,
                                  @RequestParam(required = false) String parentId,
                                  @RequestParam String name) {
        return ApiResponse.ok(departmentService.create(tenantId, parentId, name));
    }
}
