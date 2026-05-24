package com.enterpriseai.audit.service;

import com.enterpriseai.audit.entity.IpBlockRule;
import com.enterpriseai.audit.entity.SecurityAlert;
import com.enterpriseai.audit.entity.SecurityAuditLog;
import com.enterpriseai.audit.repository.IpBlockRuleRepository;
import com.enterpriseai.audit.repository.SecurityAlertRepository;
import com.enterpriseai.audit.repository.SecurityAuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

    @Mock
    private SecurityAuditLogRepository auditLogRepository;

    @Mock
    private SecurityAlertRepository alertRepository;

    @Mock
    private IpBlockRuleRepository ipBlockRuleRepository;

    @InjectMocks
    private SecurityAuditService auditService;

    @Test
    void log_shouldSaveAndReturn() {
        when(auditLogRepository.save(any(SecurityAuditLog.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SecurityAuditLog result = auditService.log(
                "t1", "u1", "prompt_injection", "high",
                "Prompt injection detected", "192.168.1.1",
                "agent-1", "execute", "{}");

        assertEquals("t1", result.getTenantId());
        assertEquals("u1", result.getUserId());
        assertEquals("prompt_injection", result.getEventType());
        assertEquals("high", result.getSeverity());
    }

    @Test
    void createAlert_shouldSaveAndReturn() {
        when(alertRepository.save(any(SecurityAlert.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SecurityAlert result = auditService.createAlert(
                "t1", "prompt_injection", "critical",
                "Jailbreak attempt detected", "u1",
                "ignore your previous instructions", "jailbreak");

        assertEquals("t1", result.getTenantId());
        assertEquals("prompt_injection", result.getAlertType());
        assertEquals("critical", result.getSeverity());
        assertFalse(result.getResolved());
    }

    @Test
    void resolveAlert_shouldSetResolved() {
        SecurityAlert alert = new SecurityAlert();
        alert.setId("a1");
        alert.setResolved(false);
        when(alertRepository.findById("a1")).thenReturn(Optional.of(alert));
        when(alertRepository.save(any(SecurityAlert.class))).thenReturn(alert);

        SecurityAlert result = auditService.resolveAlert("a1", "admin");

        assertTrue(result.getResolved());
        assertEquals("admin", result.getResolvedBy());
    }

    @Test
    void blockIp_shouldCreateBlockRule() {
        when(ipBlockRuleRepository.save(any(IpBlockRule.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        IpBlockRule result = auditService.blockIp(
                "t1", "10.0.0.5", "temporary",
                "Multiple injection attempts", 24, "admin");

        assertEquals("t1", result.getTenantId());
        assertEquals("10.0.0.5", result.getIpPattern());
        assertTrue(result.getActive());
        assertNotNull(result.getExpiresAt());
    }

    @Test
    void isIpBlocked_shouldReturnTrue() {
        IpBlockRule rule = new IpBlockRule();
        rule.setIpPattern("10.0.0.5");
        rule.setActive(true);
        when(ipBlockRuleRepository.findByTenantIdAndActiveTrue("t1"))
                .thenReturn(List.of(rule));

        assertTrue(auditService.isIpBlocked("t1", "10.0.0.5"));
    }

    @Test
    void isIpBlocked_shouldReturnFalse() {
        when(ipBlockRuleRepository.findByTenantIdAndActiveTrue("t1"))
                .thenReturn(List.of());

        assertFalse(auditService.isIpBlocked("t1", "10.0.0.5"));
    }

    @Test
    void getSecurityOverview_shouldReturnSummary() {
        when(auditLogRepository.countByTenantIdAndCreatedAtAfter(eq("t1"), any()))
                .thenReturn(100L);
        when(alertRepository.countByTenantIdAndResolved("t1", false))
                .thenReturn(5L);
        when(ipBlockRuleRepository.findByTenantIdAndActiveTrue("t1"))
                .thenReturn(List.of());
        when(alertRepository.findByTenantIdAndResolvedOrderByCreatedAtDesc(eq("t1"), eq(false)))
                .thenReturn(List.of());
        when(auditLogRepository.findByTenantIdAndCreatedAtAfterOrderByCreatedAtDesc(eq("t1"), any()))
                .thenReturn(List.of());

        Map<String, Object> result = auditService.getSecurityOverview("t1");

        assertEquals("t1", result.get("tenantId"));
        assertEquals(100L, result.get("totalEvents24h"));
        assertEquals(5L, result.get("unresolvedAlerts"));
    }
}
