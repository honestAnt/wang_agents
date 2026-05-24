package com.enterpriseai.audit.service;

import com.enterpriseai.audit.entity.IpBlockRule;
import com.enterpriseai.audit.entity.SecurityAlert;
import com.enterpriseai.audit.entity.SecurityAuditLog;
import com.enterpriseai.audit.repository.IpBlockRuleRepository;
import com.enterpriseai.audit.repository.SecurityAlertRepository;
import com.enterpriseai.audit.repository.SecurityAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final SecurityAlertRepository alertRepository;
    private final IpBlockRuleRepository ipBlockRuleRepository;

    public SecurityAuditService(SecurityAuditLogRepository auditLogRepository,
                                SecurityAlertRepository alertRepository,
                                IpBlockRuleRepository ipBlockRuleRepository) {
        this.auditLogRepository = auditLogRepository;
        this.alertRepository = alertRepository;
        this.ipBlockRuleRepository = ipBlockRuleRepository;
    }

    // ── Audit Logs ───────────────────────────────────────────

    @Transactional
    public SecurityAuditLog log(String tenantId, String userId, String eventType,
                                 String severity, String description, String sourceIp,
                                 String resource, String action, String details) {
        SecurityAuditLog log = new SecurityAuditLog();
        log.setId(UUID.randomUUID().toString());
        log.setTenantId(tenantId);
        log.setUserId(userId);
        log.setEventType(eventType);
        log.setSeverity(severity);
        log.setDescription(description);
        log.setSourceIp(sourceIp);
        log.setResource(resource);
        log.setAction(action);
        log.setDetails(details);
        log.setCreatedAt(Instant.now());
        return auditLogRepository.save(log);
    }

    public List<SecurityAuditLog> listLogs(String tenantId, String eventType, Integer hours) {
        if (hours != null && hours > 0) {
            Instant since = Instant.now().minusSeconds(hours * 3600L);
            return auditLogRepository.findByTenantIdAndCreatedAtAfterOrderByCreatedAtDesc(tenantId, since);
        }
        if (eventType != null && !eventType.isBlank()) {
            return auditLogRepository.findByTenantIdAndEventType(tenantId, eventType);
        }
        return auditLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    public List<SecurityAuditLog> listUserLogs(String tenantId, String userId) {
        return auditLogRepository.findByTenantIdAndUserIdOrderByCreatedAtDesc(tenantId, userId);
    }

    // ── Alerts ────────────────────────────────────────────────

    @Transactional
    public SecurityAlert createAlert(String tenantId, String alertType, String severity,
                                      String message, String sourceUserId, String rawPrompt,
                                      String matchedPatterns) {
        SecurityAlert alert = new SecurityAlert();
        alert.setId(UUID.randomUUID().toString());
        alert.setTenantId(tenantId);
        alert.setAlertType(alertType);
        alert.setSeverity(severity);
        alert.setMessage(message);
        alert.setSourceUserId(sourceUserId);
        alert.setRawPrompt(rawPrompt);
        alert.setMatchedPatterns(matchedPatterns);
        alert.setResolved(false);
        alert.setCreatedAt(Instant.now());
        return alertRepository.save(alert);
    }

    public List<SecurityAlert> listAlerts(String tenantId, boolean resolved) {
        return alertRepository.findByTenantIdAndResolvedOrderByCreatedAtDesc(tenantId, resolved);
    }

    @Transactional
    public SecurityAlert resolveAlert(String alertId, String resolvedBy) {
        SecurityAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setResolved(true);
        alert.setResolvedBy(resolvedBy);
        alert.setResolvedAt(Instant.now());
        return alertRepository.save(alert);
    }

    // ── IP Block Rules ───────────────────────────────────────

    @Transactional
    public IpBlockRule blockIp(String tenantId, String ipPattern, String blockType,
                                String reason, Integer durationHours, String createdBy) {
        IpBlockRule rule = new IpBlockRule();
        rule.setId(UUID.randomUUID().toString());
        rule.setTenantId(tenantId);
        rule.setIpPattern(ipPattern);
        rule.setBlockType(blockType != null ? blockType : "temporary");
        rule.setReason(reason);
        rule.setCreatedBy(createdBy);
        rule.setActive(true);
        rule.setCreatedAt(Instant.now());

        if (durationHours != null && durationHours > 0) {
            rule.setExpiresAt(Instant.now().plusSeconds(durationHours * 3600L));
        }

        return ipBlockRuleRepository.save(rule);
    }

    public List<IpBlockRule> listBlockRules(String tenantId) {
        return ipBlockRuleRepository.findByTenantId(tenantId);
    }

    public boolean isIpBlocked(String tenantId, String ip) {
        List<IpBlockRule> activeRules = ipBlockRuleRepository.findByTenantIdAndActiveTrue(tenantId);
        for (IpBlockRule rule : activeRules) {
            // Check exact match or prefix match
            if (rule.getIpPattern().equals(ip) || ip.startsWith(rule.getIpPattern().replace("*", ""))) {
                // Check expiry
                if (rule.getExpiresAt() != null && rule.getExpiresAt().isBefore(Instant.now())) {
                    rule.setActive(false);
                    ipBlockRuleRepository.save(rule);
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    @Transactional
    public void unblockIp(String ruleId) {
        IpBlockRule rule = ipBlockRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Block rule not found: " + ruleId));
        rule.setActive(false);
        ipBlockRuleRepository.save(rule);
    }

    // ── Dashboard ────────────────────────────────────────────

    public Map<String, Object> getSecurityOverview(String tenantId) {
        Instant last24h = Instant.now().minusSeconds(86400);
        long totalEvents24h = auditLogRepository.countByTenantIdAndCreatedAtAfter(tenantId, last24h);
        long unresolvedAlerts = alertRepository.countByTenantIdAndResolved(tenantId, false);
        long activeBlocks = ipBlockRuleRepository.findByTenantIdAndActiveTrue(tenantId).size();

        return Map.of(
                "tenantId", tenantId,
                "totalEvents24h", totalEvents24h,
                "unresolvedAlerts", unresolvedAlerts,
                "activeIpBlocks", activeBlocks,
                "recentAlerts", alertRepository.findByTenantIdAndResolvedOrderByCreatedAtDesc(tenantId, false),
                "recentLogs", auditLogRepository.findByTenantIdAndCreatedAtAfterOrderByCreatedAtDesc(tenantId, last24h)
        );
    }
}
