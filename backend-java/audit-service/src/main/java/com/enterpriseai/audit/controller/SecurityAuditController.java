package com.enterpriseai.audit.controller;

import com.enterpriseai.audit.entity.IpBlockRule;
import com.enterpriseai.audit.entity.SecurityAlert;
import com.enterpriseai.audit.entity.SecurityAuditLog;
import com.enterpriseai.audit.service.SecurityAuditService;
import com.enterpriseai.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class SecurityAuditController {

    private final SecurityAuditService auditService;

    public SecurityAuditController(SecurityAuditService auditService) {
        this.auditService = auditService;
    }

    // ── Audit Logs ───────────────────────────────────────────

    @PostMapping("/log")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<SecurityAuditLog> createLog(
            @RequestParam String tenantId,
            @RequestParam(required = false) String userId,
            @RequestParam String eventType,
            @RequestParam(defaultValue = "info") String severity,
            @RequestParam String description,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String details) {
        return ApiResponse.ok(auditService.log(
                tenantId, userId, eventType, severity, description, sourceIp, resource, action, details));
    }

    @GetMapping("/logs")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<SecurityAuditLog>> listLogs(
            @RequestParam String tenantId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Integer hours) {
        return ApiResponse.ok(auditService.listLogs(tenantId, eventType, hours));
    }

    @GetMapping("/logs/user/{userId}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<SecurityAuditLog>> userLogs(
            @RequestParam String tenantId, @PathVariable String userId) {
        return ApiResponse.ok(auditService.listUserLogs(tenantId, userId));
    }

    // ── Alerts ────────────────────────────────────────────────

    @PostMapping("/alerts")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<SecurityAlert> createAlert(
            @RequestParam String tenantId,
            @RequestParam String alertType,
            @RequestParam(defaultValue = "medium") String severity,
            @RequestParam String message,
            @RequestParam(required = false) String sourceUserId,
            @RequestParam(required = false) String rawPrompt,
            @RequestParam(required = false) String matchedPatterns) {
        return ApiResponse.ok(auditService.createAlert(
                tenantId, alertType, severity, message, sourceUserId, rawPrompt, matchedPatterns));
    }

    @GetMapping("/alerts")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<SecurityAlert>> listAlerts(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "false") boolean resolved) {
        return ApiResponse.ok(auditService.listAlerts(tenantId, resolved));
    }

    @PostMapping("/alerts/{id}/resolve")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<SecurityAlert> resolveAlert(
            @PathVariable String id, @RequestParam String resolvedBy) {
        return ApiResponse.ok(auditService.resolveAlert(id, resolvedBy));
    }

    // ── IP Block ──────────────────────────────────────────────

    @PostMapping("/ip-block")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<IpBlockRule> blockIp(
            @RequestParam String tenantId,
            @RequestParam String ipPattern,
            @RequestParam(defaultValue = "temporary") String blockType,
            @RequestParam String reason,
            @RequestParam(required = false) Integer durationHours,
            @RequestParam String createdBy) {
        return ApiResponse.ok(auditService.blockIp(
                tenantId, ipPattern, blockType, reason, durationHours, createdBy));
    }

    @GetMapping("/ip-blocks")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<List<IpBlockRule>> listBlocks(@RequestParam String tenantId) {
        return ApiResponse.ok(auditService.listBlockRules(tenantId));
    }

    @GetMapping("/ip-check")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Boolean> checkIpBlocked(
            @RequestParam String tenantId, @RequestParam String ip) {
        return ApiResponse.ok(auditService.isIpBlocked(tenantId, ip));
    }

    @DeleteMapping("/ip-block/{ruleId}")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Void> unblockIp(@PathVariable String ruleId) {
        auditService.unblockIp(ruleId);
        return ApiResponse.ok(null);
    }

    // ── Overview ──────────────────────────────────────────────

    @GetMapping("/overview")
    @PreAuthorize("hasRole('TenantAdmin') or hasRole('SuperAdmin')")
    public ApiResponse<Map<String, Object>> overview(@RequestParam String tenantId) {
        return ApiResponse.ok(auditService.getSecurityOverview(tenantId));
    }
}
