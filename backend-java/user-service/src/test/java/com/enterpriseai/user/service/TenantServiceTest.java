package com.enterpriseai.user.service;

import com.enterpriseai.user.dto.TenantCreateRequest;
import com.enterpriseai.user.entity.Tenant;
import com.enterpriseai.user.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void listAll_shouldReturnTenants() {
        when(tenantRepository.findAll()).thenReturn(List.of(new Tenant(), new Tenant()));

        List<Tenant> result = tenantService.listAll();
        assertEquals(2, result.size());
    }

    @Test
    void getById_shouldReturnTenant() {
        Tenant tenant = new Tenant();
        tenant.setId("t1");
        tenant.setName("Test");
        when(tenantRepository.findById("t1")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getById("t1");
        assertEquals("Test", result.getName());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(tenantRepository.findById("no-exist")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> tenantService.getById("no-exist"));
    }

    @Test
    void create_shouldSaveTenant() {
        TenantCreateRequest req = new TenantCreateRequest();
        req.setName("Acme Corp");
        req.setDomain("acme.com");

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.create(req);
        assertEquals("Acme Corp", result.getName());
        assertEquals("acme.com", result.getDomain());
        assertEquals("free", result.getPlan());
        assertNotNull(result.getId());
    }

    @Test
    void create_shouldUseDefaultPlan() {
        TenantCreateRequest req = new TenantCreateRequest();
        req.setName("Test");

        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        Tenant result = tenantService.create(req);
        assertEquals("free", result.getPlan());
        assertEquals(50, result.getMaxUsers());
    }

    @Test
    void delete_shouldCallRepository() {
        tenantService.delete("t1");
        verify(tenantRepository).deleteById("t1");
    }
}
