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
