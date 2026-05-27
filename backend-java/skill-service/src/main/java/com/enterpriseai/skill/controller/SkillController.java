package com.enterpriseai.skill.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.service.SkillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/categories")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<String>> categories(@RequestParam String tenantId) {
        return ApiResponse.ok(skillService.listCategories(tenantId));
    }

    @GetMapping("/match")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<List<Skill>> match(@RequestParam String intent,
                                          @RequestParam String tenantId) {
        return ApiResponse.ok(skillService.matchByIntent(tenantId, intent));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Skill> get(@PathVariable String id) {
        return ApiResponse.ok(skillService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Skill> create(@RequestBody Map<String, Object> body) {
        String tenantId = (String) body.get("tenantId");
        String name = (String) body.get("name");
        String displayName = (String) body.getOrDefault("displayName", name);
        String description = (String) body.get("description");
        String promptTemplate = (String) body.get("promptTemplate");
        String category = (String) body.getOrDefault("category", "general");
        String inputSchema = (String) body.get("inputSchema");
        String outputSchema = (String) body.get("outputSchema");
        String iconUrl = (String) body.get("iconUrl");
        return ApiResponse.ok(skillService.createFull(tenantId, name, displayName,
                description, promptTemplate, category, inputSchema, outputSchema, iconUrl));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
    public ApiResponse<Skill> update(@PathVariable String id,
                                     @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String displayName = (String) body.get("displayName");
        String description = (String) body.get("description");
        String promptTemplate = (String) body.get("promptTemplate");
        String category = (String) body.get("category");
        String inputSchema = (String) body.get("inputSchema");
        String outputSchema = (String) body.get("outputSchema");
        String iconUrl = (String) body.get("iconUrl");
        String status = (String) body.get("status");
        return ApiResponse.ok(skillService.updateFull(id, name, displayName, description,
                promptTemplate, category, inputSchema, outputSchema, iconUrl, status));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Skill> publish(@PathVariable String id) {
        return ApiResponse.ok(skillService.publish(id));
    }

    @PostMapping("/{id}/install")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Skill> install(@PathVariable String id) {
        return ApiResponse.ok(skillService.install(id));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
    public ApiResponse<Map<String, Object>> execute(@PathVariable String id,
                                                     @RequestBody Map<String, Object> context) {
        return ApiResponse.ok(skillService.execute(id, context));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TenantAdmin')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        skillService.delete(id);
        return ApiResponse.ok(null);
    }
}
