package com.enterpriseai.user.service;

import com.enterpriseai.user.dto.DepartmentNode;
import com.enterpriseai.user.entity.Department;
import com.enterpriseai.user.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public Department create(String tenantId, String parentId, String name) {
        Department dept = new Department();
        dept.setId(UUID.randomUUID().toString());
        dept.setTenantId(tenantId);
        dept.setParentId(parentId);
        dept.setName(name);
        dept.setPath(parentId != null ? parentId + "/" + dept.getId() : dept.getId());
        return departmentRepository.save(dept);
    }

    public List<DepartmentNode> getTree(String tenantId) {
        List<Department> all = departmentRepository.findByTenantIdOrderBySortOrder(tenantId);
        Map<String, List<Department>> byParent = all.stream()
                .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : "root"));

        return buildChildren(byParent, "root");
    }

    private List<DepartmentNode> buildChildren(Map<String, List<Department>> byParent, String parentId) {
        List<Department> children = byParent.getOrDefault(parentId, List.of());
        List<DepartmentNode> nodes = new ArrayList<>();
        for (Department d : children) {
            nodes.add(DepartmentNode.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .parentId(d.getParentId())
                    .sortOrder(d.getSortOrder())
                    .children(buildChildren(byParent, d.getId()))
                    .build());
        }
        return nodes;
    }
}
