package com.enterpriseai.billing.controller;

import com.enterpriseai.billing.entity.BillingRecord;
import com.enterpriseai.billing.repository.BillingRepository;
import com.enterpriseai.billing.service.BillingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingControllerTest {

    @Mock
    private BillingRepository repo;

    @Test
    void list_shouldReturnOk() {
        var controller = new BillingController(new BillingService(repo));
        when(repo.findByTenantId("t1")).thenReturn(List.of());
        var response = controller.list("t1");
        assertEquals(200, response.getCode());
    }

    @Test
    void record_shouldReturnCreatedRecord() {
        var controller = new BillingController(new BillingService(repo));
        when(repo.save(any(BillingRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        var response = controller.record("t1", "u1", "a1", "gpt-4.1", "trace-1", 1000, 500, 0.06);
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }
}
