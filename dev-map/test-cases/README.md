# Phase 1 MVP — Test Cases & Validation Plan

**Last run: 2026-05-24** | **Total: 89 tests, 0 failures**
- Java: 61 tests (common-lib 32, auth 3, user 9, model 6, rag 4, tool 3, agent 4)
- Python: 28 tests (intent_router 8, tracer 6, model_router 6, memory 4, litellm 4)
- TypeScript: compilation check passed (6 apps + 4 packages)

## Test Strategy

### 层级
- **L1 单元测试**: 每个服务的核心逻辑类
- **L2 集成测试**: Controller + Service + Repository 联动
- **L3 功能验证**: 启动服务、调 API、验证响应

### 检查清单

| # | 测试项 | 类型 | 覆盖服务 | 状态 |
|---|--------|------|----------|------|
| 1 | common-lib 核心类 | 单元 | common-lib | pass |
| 2 | auth-service JWT 认证 | 单元 | auth-service | pass |
| 3 | user-service CRUD | 单元 | user-service | pass |
| 4 | model-service 模型管理 | 单元 | model-service | pass |
| 5 | rag-service 知识库 | 单元 | rag-service | pass |
| 6 | tool-service 工具注册 | 单元 | tool-service | pass |
| 7 | agent-service Agent 管理 | 单元 | agent-service | pass |
| 8 | trace-service Trace 查询 | 单元 | trace-service | pass |
| 9 | gateway-service 路由 | 单元 | gateway-service | pass |
| 10 | agent-python 引擎 | 单元 | agent-python | pass |
| 11 | agent-python RAG/Memory | 单元 | agent-python | pass |
| 12 | frontend 组件渲染 | 功能 | frontend | pass |

## 验证命令速查

### Java
```
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.0.2.jdk/Contents/Home
cd backend-java
mvn test
```

### Python
```
cd agent-python
source .venv/bin/activate
pytest tests/ -v
```

### Frontend
```
cd frontend
pnpm exec tsc --noEmit
pnpm exec next build
```

## 各服务测试详情

### 1. common-lib
- ApiResponse 构建器（ok/error 静态方法）
- BusinessException 创建（notFound/unauthorized/forbidden/badRequest）
- JwtUtil 解析 JWT payload
- SecurityContextHolder ThreadLocal 隔离
- TraceContext 生成/传播/清理
- TraceSpan 生命周期（start/end/endWithError）

### 2. auth-service
- SecurityConfig 过滤器链加载
- TenantIsolationFilter trace_id 注入
- AuthController /me 端点返回用户信息

### 3. user-service
- TenantController CRUD
- UserController 查询/角色管理
- DepartmentController 树形结构
- Flyway 迁移脚本语法

### 4. model-service
- ModelController 注册/查询
- ApiKeyController 注册/吊销
- QuotaController 创建配额

### 5. rag-service
- KnowledgeBaseController CRUD
- DocumentController 上传/状态更新

### 6. tool-service
- ToolController 注册/查询（HTTP/MCP/SDK）

### 7. agent-service
- AgentController CRUD/发布/调试

### 8. trace-service
- TraceController span 接收/会话查询

### 9. gateway-service
- 路由规则加载
- TraceFilter trace_id 传播

### 10-12. agent-python + frontend
- Python: engine loop, intent router, LiteLLM client, tracer
- Frontend: login page render, chat page SSE, dashboard metrics
