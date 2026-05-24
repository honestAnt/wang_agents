package com.enterpriseai.user.middleware;

import com.enterpriseai.common.auth.SecurityContextHolder;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that enforces tenant-scoped access.
 * For advanced isolation, use Hibernate @Filter with tenant_id
 * or a multi-tenant connection provider.
 *
 * Usage: Add this as @EntityListeners on entities that need isolation.
 * For query-level isolation, use a Hibernate interceptor or
 * always filter by tenantId in repository queries.
 */
public class TenantFilter {

    @PrePersist
    @PreUpdate
    @PreRemove
    public void enforceTenant(Object entity) {
        String currentTenant = SecurityContextHolder.getTenantId();
        if (currentTenant == null) return;

        try {
            var field = entity.getClass().getDeclaredField("tenantId");
            field.setAccessible(true);
            String entityTenant = (String) field.get(entity);
            if (entityTenant != null && !entityTenant.equals(currentTenant)) {
                throw new SecurityException(
                        "Tenant mismatch: " + currentTenant + " != " + entityTenant);
            }
        } catch (NoSuchFieldException e) {
            // Entity does not have tenantId — skip
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access tenantId field", e);
        }
    }
}
