package com.enterpriseai.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DepartmentNode {

    private String id;
    private String name;
    private String parentId;
    private Integer sortOrder;
    private List<DepartmentNode> children;
}
