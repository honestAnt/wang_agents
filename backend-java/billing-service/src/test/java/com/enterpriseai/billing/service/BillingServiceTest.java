package com.enterpriseai.billing.service;

import com.enterpriseai.billing.entity.BillingRecord;
import com.enterpriseai.billing.repository.BillingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillingRepository billingRepository;

    @InjectMocks
    private BillingService billingService;

    @Test
    void record_shouldCreateBillingRecord() {
        when(billingRepository.save(any(BillingRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        BillingRecord result = billingService.record("t1", "u1", "a1", "gpt-4.1", "trace-1", 1000, 500, 0.06);
        assertEquals("gpt-4.1", result.getModel());
        assertEquals(1000, result.getPromptTokens());
        assertEquals(500, result.getCompletionTokens());
        assertEquals(0.06, result.getCostUsd().doubleValue(), 0.001);
        assertNotNull(result.getId());
    }

    @Test
    void listByTenant_shouldReturnRecords() {
        when(billingRepository.findByTenantId("t1")).thenReturn(List.of(new BillingRecord()));
        List<BillingRecord> result = billingService.listByTenant("t1");
        assertEquals(1, result.size());
    }
}
