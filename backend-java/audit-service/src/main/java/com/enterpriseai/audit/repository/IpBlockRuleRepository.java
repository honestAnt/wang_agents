package com.enterpriseai.audit.repository;

import com.enterpriseai.audit.entity.IpBlockRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpBlockRuleRepository extends JpaRepository<IpBlockRule, String> {

    List<IpBlockRule> findByTenantIdAndActiveTrue(String tenantId);

    Optional<IpBlockRule> findByTenantIdAndIpPatternAndActiveTrue(String tenantId, String ipPattern);

    List<IpBlockRule> findByTenantId(String tenantId);
}
