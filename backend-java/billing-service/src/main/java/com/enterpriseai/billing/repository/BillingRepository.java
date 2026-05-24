package com.enterpriseai.billing.repository;

import com.enterpriseai.billing.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<BillingRecord, String> {
    List<BillingRecord> findByTenantId(String tenantId);
}
