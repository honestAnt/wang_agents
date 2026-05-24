package com.enterpriseai.skill.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.service.SkillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<Skill>> list(@RequestParam String tenantId) {
        return ApiResponse.ok(skillService.listByTenant(tenantId));
    }

    @GetMapping("/marketplace")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<Skill>> marketplace(@RequestParam String tenantId) {
        return ApiResponse.ok(skillService.listPublished(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Skill> get(@PathVariable String id) {
        return ApiResponse.ok(skillService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Skill> create(@RequestParam String tenantId,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String promptTemplate,
                                      @RequestParam(required = false) String category) {
        return ApiResponse.ok(skillService.create(tenantId, name, description, promptTemplate, category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Skill> update(@PathVariable String id,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam(required = false) String promptTemplate) {
        return ApiResponse.ok(skillService.update(id, name, description, promptTemplate));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Skill> publish(@PathVariable String id) {
        return ApiResponse.ok(skillService.publish(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        skillService.delete(id);
        return ApiResponse.ok(null);
    }
}
