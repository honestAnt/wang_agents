package com.enterpriseai.user.service;

import com.enterpriseai.common.auth.SecurityContextHolder;
import com.enterpriseai.user.dto.TenantCreateRequest;
import com.enterpriseai.user.entity.Tenant;
import com.enterpriseai.user.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    public Tenant getById(String id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + id));
    }

    public Tenant create(TenantCreateRequest request) {
        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID().toString());
        tenant.setName(request.getName());
        tenant.setDomain(request.getDomain());
        tenant.setPlan(request.getPlan() != null ? request.getPlan() : "free");
        tenant.setMaxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 50);
        tenant.setMaxAgents(request.getMaxAgents() != null ? request.getMaxAgents() : 10);
        return tenantRepository.save(tenant);
    }

    public Tenant update(String id, TenantCreateRequest request) {
        Tenant tenant = getById(id);
        tenant.setName(request.getName());
        if (request.getDomain() != null) tenant.setDomain(request.getDomain());
        if (request.getPlan() != null) tenant.setPlan(request.getPlan());
        if (request.getMaxUsers() != null) tenant.setMaxUsers(request.getMaxUsers());
        if (request.getMaxAgents() != null) tenant.setMaxAgents(request.getMaxAgents());
        return tenantRepository.save(tenant);
    }

    public void delete(String id) {
        tenantRepository.deleteById(id);
    }
}
