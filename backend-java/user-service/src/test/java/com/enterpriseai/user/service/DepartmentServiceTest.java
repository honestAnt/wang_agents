package com.enterpriseai.user.service;

import com.enterpriseai.user.dto.DepartmentNode;
import com.enterpriseai.user.entity.Department;
import com.enterpriseai.user.repository.DepartmentRepository;
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
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private DepartmentService departmentService;

    @Test
    void getTree_shouldReturnRootNodes() {
        Department dept = new Department();
        dept.setId("d1");
        dept.setName("Engineering");
        dept.setParentId(null);

        when(departmentRepository.findByTenantIdOrderBySortOrder("t1"))
                .thenReturn(List.of(dept));

        List<DepartmentNode> tree = departmentService.getTree("t1");
        assertEquals(1, tree.size());
        assertEquals("Engineering", tree.get(0).getName());
        assertTrue(tree.get(0).getChildren().isEmpty());
    }

    @Test
    void getTree_shouldBuildNestedStructure() {
        Department parent = new Department();
        parent.setId("d1");
        parent.setName("Engineering");
        parent.setParentId(null);

        Department child = new Department();
        child.setId("d2");
        child.setName("Backend");
        child.setParentId("d1");

        when(departmentRepository.findByTenantIdOrderBySortOrder("t1"))
                .thenReturn(List.of(parent, child));

        List<DepartmentNode> tree = departmentService.getTree("t1");
        assertEquals(1, tree.size());
        assertEquals("Engineering", tree.get(0).getName());
        assertEquals(1, tree.get(0).getChildren().size());
        assertEquals("Backend", tree.get(0).getChildren().get(0).getName());
    }

    @Test
    void create_shouldSetPath() {
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> inv.getArgument(0));

        Department dept = departmentService.create("t1", null, "Root");
        assertNotNull(dept.getId());
        assertEquals("Root", dept.getName());
    }
}
