# Java 后端 (Backend Java)

14 个 Spring Boot 微服务，Maven 多模块管理。

## 结构

```
backend-java/
├── pom.xml              # Parent POM (版本管理)
├── common-lib/          # 公共库
├── gateway-service/     # API 网关
├── auth-service/        # 认证服务 (Keycloak)
├── user-service/        # 用户/租户/部门管理
├── agent-service/       # Agent 配置 + 市场
├── model-service/       # 模型注册与治理
├── rag-service/         # 知识库管理
├── tool-service/        # Tool Registry + MCP
├── skill-service/       # Skills 管理 + 技能市场
├── memory-service/      # 长期记忆
├── prompt-service/      # Prompt 平台
├── trace-service/       # 追踪与审计
├── billing-service/     # 计费与成本
├── admin-service/       # 运营分析 + 平台管理
└── audit-service/       # 安全审计
```

## 服务说明

### common-lib
所有服务共享：统一响应体 `ApiResponse<T>`、全局异常处理、JWT 工具、Feign 拦截器、Kafka 基础配置、TraceSpan 模型。

### gateway-service
Spring Cloud Gateway 统一入口：JWT 鉴权过滤器、租户级限流、CORS 配置、WebSocket 代理（SSE streaming 透传）、请求日志。

### auth-service
集成 Keycloak：登录/登出/Token 刷新/用户信息接口、RBAC 权限注解 `@PreAuthorize`、租户隔离过滤器、SSO/LDAP/OAuth2 多认证源。

### agent-service
Agent CRUD + 版本管理 (Draft → Test → Published → Rollback)、Tool/Skill 绑定、Agent 调试 API、**Agent Marketplace**（发布/发现/安装/评价）。

### model-service
模型注册 (GPT/Claude/Gemini/Qwen/DeepSeek/vLLM/Ollama)、API Key 加密存储、Quota 管理、Budget 控制、路由策略配置、多级 Fallback 链。

### rag-service
知识库 CRUD、文档上传 (PDF/Word/PPT/Excel/MD/HTML)、分块策略、Embedding 配置、Hybrid Search API、权限感知过滤、Rerank 配置。

### tool-service
统一工具注册中心：HTTP Tool / MCP Tool / SDK Tool、鉴权配置、限流配置、熔断配置 (Circuit Breaker)、调用日志。

### skill-service
`Prompt + Tool + Workflow` 能力封装、版本管理、动态加载+热更新、Skill Router、企业内部技能市场。

### trace-service
Span 接收与存储、会话列表/详情 (Span 树)、Kafka 消费端、Token 成本统计、Langfuse + OpenTelemetry 集成。

### admin-service
运营分析中心：使用总结/趋势图/排行/质量指标/成本拆解/周报月报。**平台运营管理**：全局限流、Provider 熔断、平台公告。

### audit-service
安全审计：Prompt Injection 告警、IP 黑名单、审计日志、安全概览。

---

## 编码规范

### 1. 项目结构约定

每个服务模块遵循统一的包结构：

```
<service>/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/enterpriseai/<service>/
    │   │   ├── config/       # AutoConfiguration (spring.factories)
    │   │   ├── controller/   # REST Controller
    │   │   ├── service/      # Business Service
    │   │   ├── repository/   # JPA Repository (Mapper)
    │   │   └── entity/       # JPA Entity (Domain Model)
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/ # Flyway SQL
    └── test/
        └── java/com/enterpriseai/<service>/
            ├── controller/   # Controller 测试
            └── service/      # Service 单元测试
```

**无需 DTO/VO 层**：当前阶段 Entity 直接作为 API 返回体。当需要隔离内部表结构与外部 API 时再引入。

---

### 2. Controller 规范

**职责**：接收请求 → 参数校验 → 调用 Service → 返回 `ApiResponse`。Controller 不包含业务逻辑。

```java
package com.enterpriseai.skill.controller;

import com.enterpriseai.common.api.ApiResponse;
import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.service.SkillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;

    // 构造器注入（不用 @Autowired，不用 setter 注入）
    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }
```

**规则**：

| 规则 | 说明 |
|------|------|
| 类注解 | `@RestController` + `@RequestMapping("/api/<资源名>")` |
| 注入方式 | **构造器注入**，字段 `private final`，不用 `@Autowired` |
| 返回类型 | 始终 `ApiResponse<T>`，用 `ApiResponse.ok(data)` 或 `ApiResponse.error(code, msg)` |
| 权限控制 | 每个端点加 `@PreAuthorize`，明确角色 |
| 参数获取 | 简单参数用 `@RequestParam`，复杂对象用 `@RequestBody Map<String, Object>` |
| 路径变量 | `@PathVariable String id` |
| 异常处理 | 不在 Controller 写 try-catch，交给 `GlobalExceptionHandler` |
| 请求方法 | GET 查询、POST 创建/动作、PUT 全量更新、DELETE 删除 |

**端点命名**：

```
GET    /api/{resources}              # 列表
GET    /api/{resources}/{id}         # 详情
POST   /api/{resources}              # 创建
PUT    /api/{resources}/{id}         # 更新
DELETE /api/{resources}/{id}         # 删除
POST   /api/{resources}/{id}/publish # 动作（动词）
GET    /api/{resources}/stats        # 统计子资源放前面
```

**权限标注示例**：

```java
@GetMapping
@PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer') or hasRole('User')")
public ApiResponse<List<Skill>> list(@RequestParam String tenantId) { ... }

@PostMapping
@PreAuthorize("hasRole('TenantAdmin') or hasRole('Developer')")
public ApiResponse<Skill> create(@RequestBody Map<String, Object> body) { ... }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('TenantAdmin')")
public ApiResponse<Void> delete(@PathVariable String id) { ... }
```

---

### 3. Service 规范

**职责**：业务逻辑、数据组装、调用 Repository。Service 方法返回 Entity 或基础类型，不返回 `ApiResponse`。

```java
@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }
```

**规则**：

| 规则 | 说明 |
|------|------|
| 类注解 | `@Service` |
| 注入方式 | **构造器注入**，字段 `private final` |
| 事务 | 写操作需要时加 `@Transactional`，读操作不加 |
| 异常 | 用 `throw new RuntimeException("Skill not found: " + id)` ，不带数据 |
| 方法命名 | 与 Repository 对齐：`listByTenant`, `getById`, `create`, `update`, `delete`, `publish` |
| ID 生成 | `UUID.randomUUID().toString()` 在 Service 层生成，不在 Entity 用 `@GeneratedValue` |
| 默认值 | 在 Service 层设置（如 `category != null ? category : "general"`），不在 Entity |
| 局部更新 | update 方法用 `if (x != null) entity.setX(x)` 模式，只更新传入的非 null 字段 |

**方法命名约定**：

| 前缀 | 含义 | 示例 |
|------|------|------|
| `list*` | 返回列表 | `listByTenant`, `listPublished` |
| `get*` | 返回单个，找不到抛异常 | `getById` |
| `create*` | 新建 | `create`, `createFull` |
| `update*` | 修改 | `update`, `updateFull` |
| `delete*` | 删除 | `delete` |
| 动词 | 业务动作 | `publish`, `install`, `execute` |

---

### 4. Repository (Mapper) 规范

使用 Spring Data JPA，接口继承 `JpaRepository`，**不写 SQL**（除非复杂查询）。

```java
@Repository
public interface SkillRepository extends JpaRepository<Skill, String> {

    List<Skill> findByTenantId(String tenantId);

    List<Skill> findByTenantIdAndStatus(String tenantId, String status);

    List<Skill> findByTenantIdAndName(String tenantId, String name);
}
```

**规则**：

| 规则 | 说明 |
|------|------|
| 类注解 | `@Repository` |
| 继承 | `JpaRepository<Entity, ID类型>` |
| 方法命名 | Spring Data 方法命名约定，不用 `@Query` 除非必要 |
| 返回值 | 单条用 `Optional<Entity>`，多条用 `List<Entity>` |
| ID 类型 | 统一 `String`（UUID） |
| 多租户 | 查询方法名 **必须以 `findByTenantId` 开头** |

**常用方法命名模式**：

```
findByTenantId(String tenantId)
findByTenantIdAndStatus(String tenantId, String status)
findByTenantIdAndCategory(String tenantId, String category)
findByTenantIdAndName(String tenantId, String name)
findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name)
findByTenantIdAndStatusAndCategory(String tenantId, String status, String category)
```

---

### 5. Entity 规范

使用 Lombok `@Data`，统一 ID 为 `String` 类型（UUID），手动管理时间戳。

```java
package com.enterpriseai.skill.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private String id;                          // UUID 字符串，不由数据库生成

    @Column(nullable = false, length = 36)
    private String tenantId;                    // 多租户标识

    @Column(nullable = false, length = 128)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 16)
    private String status = "draft";            // 默认值

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
```

**规则**：

| 规则 | 说明 |
|------|------|
| 类注解 | `@Data` + `@Entity` + `@Table(name = "表名")` |
| ID | `@Id private String id`，**数据库不自动生成**，由 Service 设 UUID |
| 必填字段 | `@Column(nullable = false)` |
| 长度限制 | `@Column(length = N)` 对 VARCHAR 字段 |
| 大文本 | `@Column(columnDefinition = "TEXT")` 不用 `@Lob` |
| 默认值 | 直接在字段声明处赋值（`private String status = "draft"`） |
| 时间 | 统一 `java.time.Instant`，默认 `Instant.now()` |
| 金额 | 用 `BigDecimal`，不用 `Float`/`Double` |
| 枚举 | 用 `String` 存储，不用 `@Enumerated`（数据库可读性好） |
| 关联 | 现阶段不用 `@ManyToOne`/`@OneToMany`，需要时通过 ID 字段 + Service 层手动关联 |
| 表名 | 复数形式：`skills`, `tools`, `agents` |
| JSON 长文本 | `inputSchema`, `outputSchema`, `schemaJson` 等纯 JSON 字段用 `TEXT` |

**字段声明顺序**：

1. `id`
2. `tenantId`
3. 业务字段（name, displayName, description...）
4. 状态字段（status, version...）
5. 时间字段（createdAt, updatedAt）

---

### 6. 注释规范

**原则**：代码即文档，注释只解释 Why 不解释 What。

**需要写注释的情况**：
- 非直觉的业务规则或约束
- 临时 workaround 及原因
- 性能敏感的代码为什么这样写

**不需要写注释的情况**：
- 方法名已经自解释（如 `listByTenant`）
- 标准的 getter/setter
- Controller 的标准 CRUD
- 参数名清晰的简单逻辑

```java
// GOOD — 解释 why
// Use LinkedHashMap to preserve insertion order for trace output
Map<String, Object> result = new LinkedHashMap<>();

// BAD — 重复 what code already says
// Get skill by id
Skill skill = getById(skillId);

// BAD — 冗余的 Javadoc
/**
 * List skills by tenant.
 * @param tenantId the tenant id
 * @return list of skills
 */
public List<Skill> listByTenant(String tenantId) { ... }
```

**类级别注释**：不要求 Javadoc，类名和包名已说明职责。

**方法级别**：仅当行为有隐藏约束时写一行注释。

```java
// Returns only published skills, filters out draft/test/archived
public List<Skill> matchByIntent(String tenantId, String intent) { ... }
```

---

### 7. AutoConfiguration 规范

每个服务模块提供一个 `XxxModuleAutoConfiguration`，在 `spring.factories` 中注册，由 `agent-application` 自动加载。

```java
package com.enterpriseai.skill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import jakarta.annotation.PostConstruct;

@AutoConfiguration
@ComponentScan(basePackages = {"com.enterpriseai.skill"})
@EnableJpaRepositories(basePackages = {"com.enterpriseai.skill.repository"})
@EntityScan(basePackages = {"com.enterpriseai.skill.entity"})
@Slf4j
public class SkillModuleAutoConfiguration {

    @PostConstruct
    public void init() {
        log.info("SkillModuleAutoConfiguration loaded.");
    }
}
```

**`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**：

```
com.enterpriseai.skill.config.SkillModuleAutoConfiguration
```

注意：Spring Boot 3.x 用 `AutoConfiguration.imports` 文件，不用旧版 `spring.factories` 的 `EnableAutoConfiguration` key。

---

### 8. 单元测试规范

测试框架：**JUnit 5 + Mockito**，服务层用纯 Mockito，不启动 Spring 上下文。

```java
package com.enterpriseai.skill.service;

import com.enterpriseai.skill.entity.Skill;
import com.enterpriseai.skill.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillService skillService;

    @Test
    void create_shouldSaveSkill() {
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        Skill result = skillService.create("t1", "data_analysis", "desc", "prompt", "analysis");
        assertEquals("data_analysis", result.getName());
        assertEquals("draft", result.getStatus());
        assertNotNull(result.getId());
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(skillRepository.findById("bad")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> skillService.getById("bad"));
    }

    @Test
    void delete_shouldCallRepository() {
        skillService.delete("s1");
        verify(skillRepository).deleteById("s1");
    }
}
```

**规则**：

| 规则 | 说明 |
|------|------|
| 框架 | JUnit 5 (`org.junit.jupiter.api`) + Mockito |
| 扩展 | `@ExtendWith(MockitoExtension.class)` — **不启动 Spring Context** |
| Mock | `@Mock` 依赖，`@InjectMocks` 被测对象 |
| 测试类命名 | `<被测类名>Test`，如 `SkillServiceTest` |
| 测试方法命名 | `方法名_should行为描述`，如 `create_shouldSaveSkill`, `getById_shouldThrowWhenNotFound` |
| 可见性 | 测试类和测试方法都用 **package-private**（不用 `public`） |
| 断言 | `assertEquals`, `assertNotNull`, `assertThrows`, `assertTrue` |
| verify | 用 `verify(repository).deleteById("s1")` 验证调用 |
| stub | `when(...).thenReturn(...)` / `when(...).thenAnswer(...)` |
| 不测什么 | 不测框架代码（Repository 方法、Entity getter/setter） |

**必须覆盖的测试场景**：

每个 Service 至少覆盖：

1. **正常创建** — 验证默认值设置、ID 生成
2. **正常查询** — 验证返回数据结构
3. **资源不存在** — 验证抛出 `RuntimeException`
4. **删除** — 验证 `verify` Repository 调用
5. **状态变更** — 如 publish、状态流转

**不需要测试的场景**：

- Controller 层（简单委托，无业务逻辑）
- Repository 层（框架生成）
- Entity 的 getter/setter
- 纯配置类

**测试文件位置**：镜像 `src/main` 的结构：

```
src/test/java/com/enterpriseai/skill/
├── controller/   # 如果需要
└── service/
    └── SkillServiceTest.java
```

---

### 9. 通用规则

**依赖注入**：始终构造器注入。

```java
// GOOD
public class SkillController {
    private final SkillService skillService;
    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }
}

// BAD — 字段注入
@Autowired
private SkillService skillService;
```

**包命名**：`com.enterpriseai.<service>.<layer>`，全小写，无下划线。

**类命名**：
- Controller: `<资源名>Controller`（如 `SkillController`）
- Service: `<资源名>Service`（如 `SkillService`）
- Repository: `<资源名>Repository`（如 `SkillRepository`）
- Entity: `<资源名>` 单数（如 `Skill`, `Tool`）

**Java 版本**：Java 21，用 `var` 简化局部变量、`switch` 表达式、Text Block `"""..."""` 等特性。

**Lombok**：用 `@Data`、`@Slf4j`、`@Builder`，不用 `@Getter`/`@Setter` 单独注解。

**异常处理**：统一用 `RuntimeException`（或其子类），由 `common-lib` 的 `GlobalExceptionHandler` 统一转 `ApiResponse`。

**API 路径**：`/api/<资源复数>` — 不用版本前缀（如 `/v1/`），因为单体应用内版本控制无意义。
