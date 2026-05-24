package com.enterpriseai.tool.repository;

import com.enterpriseai.tool.entity.Tool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToolRepository extends JpaRepository<Tool, String> {

    List<Tool> findByTenantId(String tenantId);

    List<Tool> findByTenantIdAndToolType(String tenantId, String toolType);
}
