# Phase 1-3 — Test Cases & Validation Plan (Full Coverage)

**Last run: 2026-05-24** | **Total: 143 tests, 0 failures** | **Target: 280+ tests**
- Java: 84 tests across 12 modules (service + controller layers)
  - common-lib: 32 (ApiResponse, BusinessException, JwtUtil, SecurityContextHolder, TraceContext, TraceSpan)
  - auth-service: 3 (TenantIsolationFilter)
  - user-service: 9 (TenantService, DepartmentService)
  - model-service: 6 (ModelService)
  - rag-service: 4 (KnowledgeBaseService)
  - tool-service: 3 (ToolService)
  - agent-service: 4 (AgentService)
  - skill-service: 4 (SkillService)
  - memory-service: 7 (MemoryService 4 + MemoryController 3)
  - prompt-service: 4 (PromptService 2 + PromptController 2)
  - billing-service: 4 (BillingService 2 + BillingController 2)
  - trace-service: 4 (TraceController)
- Python: 59 tests across 10 modules
  - intent_router: 8 | tracer: 6 | model_router: 6 | litellm_client: 4
  - memory: 4 | main(FastAPI): 2 | skill: 5 | multi_agent: 9
  - rag(embedding/reranker/permission/retriever): 7 | long_memory: 6 | executor: 2
- TypeScript: tsc --noEmit passed (6 apps + 4 packages)

## Test Strategy

### 层级
- **L1 单元测试**: 每个服务的核心逻辑类
- **L2 集成测试**: Controller + Service + Repository 联动
- **L3 功能验证**: 启动服务、调 API、验证响应

### 检查清单

| # | 测试项 | 类型 | 覆盖服务 | Phase | 状态 |
|---|--------|------|----------|-------|------|
| 1 | common-lib 核心类 | 单元 | common-lib | P1 | pass |
| 2 | auth-service JWT 认证 | 单元 | auth-service | P1 | pass |
| 3 | user-service CRUD | 单元 | user-service | P1 | pass |
| 4 | model-service 模型管理 | 单元 | model-service | P1 | pass |
| 5 | rag-service 知识库 | 单元 | rag-service | P1 | pass |
| 6 | tool-service 工具注册 | 单元 | tool-service | P1 | pass |
| 7 | agent-service Agent 管理 | 单元 | agent-service | P1 | pass |
| 8 | trace-service Trace 查询 | 单元 | trace-service | P1 | pass |
| 9 | gateway-service 路由 | 单元 | gateway-service | P1 | **todo** |
| 10 | agent-python 引擎 | 单元 | agent-python | P1 | pass |
| 11 | agent-python RAG/Memory | 单元 | agent-python | P1 | pass |
| 12 | frontend 组件渲染 | 功能 | frontend | P1 | pass(tsc) |
| 13 | model-service ApiKey/Quota/Fallback | 单元 | model-service | P1 | **todo** |
| 14 | rag-service Document/Chunk/HybridSearch | 单元 | rag-service | P1 | **todo** |
| 15 | tool-service Auth/CircuitBreaker/MCP | 单元 | tool-service | P1 | **todo** |
| 16 | agent-service Config/Version/Debug | 单元 | agent-service | P1 | **todo** |
| 17 | skill-service Marketplace/Router | 单元 | skill-service | P2 | **todo** |
| 18 | prompt-service ABTest/Grayscale/Template | 单元 | prompt-service | P2 | **todo** |
| 19 | memory-service Episodic/Semantic/Decay | 单元 | memory-service | P2 | **todo** |
| 20 | billing-service Kafka/Budget/Invoice | 单元 | billing-service | P2 | **todo** |
| 21 | trace-service Langfuse/OTEL 集成 | 集成 | trace-service | P2 | **todo** |
| 22 | Multi-Agent 协作测试 | 单元 | agent-python | P2 | pass |
| 23 | Workflow Temporal 流程 | 集成 | agent-python | P3 | **todo** |
| 24 | Agent 市场发布/发现/安装 | 单元 | agent-service | P3 | pass |
| 25 | 智能路由 TaskClassify/Fallback | 单元 | agent-python | P3 | pass |
| 26 | AI 运营分析中心 | 单元 | admin-service | P3 | pass |
| 27 | AI 安全治理全链路 | 单元+集成 | audit+python | P3 | pass |
| 28 | SDK (Java + Python) | 集成 | sdk/ | P3 | **todo** |
| 29 | K8s Helm + CI/CD Pipeline | 部署 | deploy/ | P3 | **todo** |
| 30 | E2E 跨服务流程 (11条) | E2E | 全栈 | ALL | **todo** |
| 31 | 多租户数据隔离验证 | 集成 | 全栈 | ALL | **todo** |

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

### 9. gateway-service ⚠️ 无测试文件
- 路由规则加载
- TraceFilter trace_id 传播
- JWT 鉴权过滤器 / 租户级限流 / CORS / WebSocket 代理

### 10-12. agent-python + frontend
- Python: engine loop, intent router, LiteLLM client, tracer
- Frontend: login page render, chat page SSE, dashboard metrics

---

## Phase 1 待补充测试 (扩展覆盖 → 目标 80+ 新用例)

以下模块已有基础测试但覆盖不全，需补充：

### gateway-service (现有 0 → 目标 12+)
当前无测试文件，需新建：
- **GatewayRoutesTest**: 路由规则加载验证 — auth/user/agent/model/rag/tool/chat 路径正确路由到对应服务
- **JwtAuthFilterTest**: JWT 鉴权过滤器 — 有效 token 放行 / 无效 token 返回 401 / 无 token 返回 401 / 过期 token 处理
- **TraceIdFilterTest**: trace_id 注入 — 请求头无 trace_id 时自动生成 / 有 trace_id 时透传 / 响应头包含 trace_id
- **RateLimiterTest**: 租户级限流 — 按 tenant_id + API path 限制 QPS / 超限返回 429 / 限流头正确设置
- **CorsConfigTest**: CORS 配置 — 允许的源/方法/头 / 预检请求 OPTIONS 处理
- **WebSocketProxyTest**: SSE streaming 透传 — WebSocket 连接建立 / 流式数据转发

### auth-service (现有 1 → 目标 8+)
- **AuthControllerTest**: /auth/login 成功返回 JWT / /auth/refresh 刷新 token / /auth/me 返回用户信息含角色租户 / /auth/logout 使 token 失效
- **SecurityConfigTest**: 过滤器链加载 / 白名单路径 (/health, /metrics) 无需认证 / RBAC 注解生效验证
- **MultiAuthSourceTest**: SSO 回调处理 / LDAP 用户同步 / OAuth2 多 provider 配置

### user-service (现有 2 → 目标 8+)
- **UserControllerTest**: 用户 CRUD / 按部门筛选用户 / 批量导入用户 / LDAP 同步
- **RoleControllerTest**: 角色 CRUD / 角色绑定用户 / 角色解绑 / 自定义角色权限
- **TenantDataIsolationTest**: SQL 自动注入 tenant_id 过滤 / 跨租户数据不可见

### model-service (现有 1 → 目标 10+)
- **ApiKeyControllerTest**: API Key 创建(加密存储) / 列表(脱敏展示) / 吊销 / 轮换
- **QuotaControllerTest**: 创建配额(按租户/用户/Agent) / 超配额拒绝 / 配额调整
- **BudgetControllerTest**: 月预算设置 / 超额告警 / 超额阻断 / 超额降级策略
- **ModelRouterTest**: 按 task 类型路由 / 按用户级别路由 / Fallback 链执行(GPT-4→Claude→Qwen→DeepSeek)
- **ModelProviderTest**: vLLM 自部署模型注册 / Ollama 本地模型注册

### rag-service (现有 1 → 目标 10+)
- **DocumentControllerTest**: 文档上传(PDF/Word/PPT/Excel/MD/HTML) / 文档状态更新 / 文档删除 + 关联 chunk 级联删除
- **ChunkServiceTest**: 分块策略验证(固定大小/语义分块/递归分块/Markdown分块) / Chunk 元数据保留 / Parent Chunk 关联
- **HybridSearchTest**: BM25 + Vector 混合检索 / Metadata Filter 过滤 / 权限感知过滤(tenant+department+role)
- **RerankerConfigTest**: BGE Reranker 配置 / Jina Reranker / CrossEncoder / Cohere Reranker / Rerank 前后分数对比
- **EmbeddingConfigTest**: Embedding 模型选择 / 维度配置 / batch size 配置

### tool-service (现有 1 → 目标 10+)
- **ToolAuthTest**: API Key 鉴权 / OAuth 鉴权 / BasicAuth 鉴权 / 鉴权失败返回 401
- **RateLimiterConfigTest**: Token Bucket 限流 / Sliding Window 限流 / 限流计数器正确
- **CircuitBreakerTest**: 失败阈值触发熔断(CLOSED→OPEN) / 半开探测(HALF_OPEN) / 成功恢复(→CLOSED) / 熔断期间快速失败
- **ToolCallLogTest**: 入参记录 / 出参记录 / latency 记录 / error 记录 / 日志查询
- **MCPToolTest**: MCP Tool 注册 / MCP 协议握手 / JSON schema 校验

### agent-service (现有 2 → 目标 8+)
- **AgentConfigTest**: System Prompt 绑定 / Tool 绑定 / Skill 绑定 / Memory 策略绑定 / 模型绑定
- **AgentVersionTest**: Draft→Test→Published 状态流转 / Rollback 回滚 / 版本历史查询
- **AgentDebugTest**: 提交测试消息 / 返回完整 trace / trace 包含所有 span 类型
- **AgentPermissionTest**: 角色授权 / 用户授权 / 未授权拒绝访问

### trace-service (现有 1 → 目标 8+)
- **SpanIngestionTest**: 批量写入 span / 单 span 写入 / Span 必填字段校验
- **SessionQueryTest**: 按用户/Agent/模型/时间范围/异常状态筛选 / 分页查询
- **CostAnalysisTest**: 按用户/Agent/模型/时间聚合 / Token 成本计算正确性

### 前端组件测试 (现有 tsc → 目标 Vitest + Playwright)
- **chat-ui**: 消息列表渲染 / SSE 流式接收 / Markdown 代码块高亮 / Mermaid 图表渲染 / LaTeX 公式渲染 / 文件上传预览 / 模型切换下拉 / Agent 切换
- **admin-console**: Dashboard 指标卡片 / Token 趋势图(ECharts) / 模型调用排行 / 时间范围选择器
- **trace-console**: 会话列表多条件筛选 / 对话流分层展示 / Span 树展开折叠 / Prompt 查看弹窗 / Token 成本图表
- **agent-studio**: Agent 创建表单 / Prompt 编辑器 / Tool/Skill 绑定界面 / Agent 调试输入输出
- **rag-studio**: 知识库列表 / 文档上传进度 / Chunk 查看分页 / Recall 调试输入输出
- **model-center**: 模型注册表单 / API Key 配置 / Quota 配置 / 路由规则配置

---

## Phase 2 待补充测试 (扩展覆盖 → 目标 50+ 新用例)

### skill-service (现有 1 → 目标 8+)
- **SkillMarketplaceTest**: Skill 发布到市场 / 搜索(按分类/标签/热度) / 安装到当前租户 / 评价(评分+评论)
- **SkillRouterTest**: Intent→Skill 匹配 / 多 Skill 链式调用 / Skill 执行顺序正确
- **SkillHotReloadTest**: 新版本发布后缓存刷新 / Kafka skill.published 事件消费 / 热更新后 Agent 使用新版本
- **SkillVersionTest**: 版本历史 / 版本对比 / Rollback 回滚

### prompt-service (现有 2 → 目标 10+)
- **PromptABTestTest**: AB Test 配置 / 流量分配(百分比) / 多模型对比测试 / 效果统计(满意度/成本/Tool调用率)
- **PromptGrayscaleTest**: 灰度发布配置 / 按百分比逐步发布 / 按用户群发布 / 灰度回滚
- **PromptTemplateTest**: 变量定义 / Memory injection 占位符 / RAG injection 占位符 / 渲染后 Prompt 正确性
- **TokenEstimationTest**: 输入 Prompt 自动计算 Token 消耗 / 不同模型 Token 计算差异 / 预估与实际偏差 < 10%

### memory-service (现有 2 → 目标 8+)
- **EpisodicMemoryTest**: 事件记忆写入 / 按时间线检索 / 时间范围筛选 / 关联事件聚类
- **SemanticMemoryTest**: 语义记忆写入 / 向量相似检索 / 相似度阈值过滤
- **ProceduralMemoryTest**: 过程记忆写入 / 操作流程复用 / 成功经验检索
- **MemoryConsolidationTest**: 短期→长期记忆压缩 / 摘要自动生成 / 重复记忆去重
- **MemoryDecayTest**: 按时间衰减 / 按重要性衰减 / 衰减后检索排序变化
- **MemoryPermissionTest**: tenant_id + user_id 双重过滤 / 跨用户不可见 / 跨租户不可见

### agent-python Multi-Agent (补充 → 目标 8+)
- **CoordinatorAgentTest**: 任务分解正确性 / 子任务分发 / 结果汇总 / 异常子任务处理
- **SearchAgentTest**: RAG 检索执行 / Tool 查询执行 / 结果格式化返回
- **AnalysisAgentTest**: 数据分析 / 推理链路 / 中间结果传递
- **ReportAgentTest**: 多源结果汇总 / 报告格式生成 / Markdown/PDF 输出
- **MultiAgentTraceTest**: 父 span 关联所有子 Agent span / 全链路 span 树完整
- **AgentCommunicationTest**: Agent 间消息传递 / 超时处理 / 重试机制

### billing-service (现有 2 → 目标 6+)
- **KafkaConsumerTest**: 消费 llm.call 事件 / cost 计算(token × 单价) / 多 provider 单价差异
- **BudgetAlertTest**: 接近 budget 阈值触发告警 / 告警渠道(Slack/Email/Webhook) / 告警频率限制
- **InvoiceTest**: 月度账单生成 / PDF 导出 / CSV 导出 / 账单包含按模型/Agent 分项
- **CostAggregationTest**: 实时成本聚合 / 按日/周/月粒度 / 多维度交叉聚合

### trace-service 增强 (Phase 2 补充)
- **LangfuseIntegrationTest**: LLM call 自动记录 / prompt/token/cost/latency 字段完整
- **OpenTelemetryTest**: Java Agent 自动采集 / 微服务间调用链关联 / OTEL Collector 导出正确
- **GrafanaDashboardTest**: QPS/Token/错误率/延迟面板数据验证
- **AlertRuleTest**: token > threshold 触发 / error_rate > threshold 触发 / tool_failure > threshold 触发 / 通知渠道验证

---

## Phase 3 功能与流程测试 (目标 60+ 新用例)

### AI Workflow (Temporal) 流程测试
- **DataCollectionWorkflowTest**: 数据采集→RAG分析→报告生成 完整流程 / 各 Activity 执行顺序 / Activity 失败重试
- **AgentPipelineWorkflowTest**: 多步骤 Agent 流水线 / 步骤间数据传递 / 步骤超时处理
- **ApprovalWorkflowTest**: 人工审批节点 / 审批超时自动处理 / 审批通过/拒绝后续分支
- **SagaCompensationTest**: 工作流失败触发补偿 / 补偿操作逆序执行 / 补偿失败告警 / 最终一致性验证
- **WorkflowMonitoringTest**: Temporal UI 工作流状态 / 历史事件 / 重试记录 / 堆栈跟踪
- **WorkflowLifecycleTest**: 创建→运行→暂停→恢复→取消 全生命周期

### Agent 市场 功能测试
- **AgentPublishTest**: 发布到市场(含Prompt/Tool/Skill/Memory完整配置) / 版本更新 / 下架
- **AgentDiscoverTest**: 按分类筛选 / 按标签搜索 / 按热度排序 / 全文搜索
- **AgentInstallTest**: 一键复制到当前租户 / 安装后配置可修改 / 安装记录统计
- **AgentReviewTest**: 评分(1-5星) / 评论 / 使用次数统计 / 评分聚合

### 智能路由 流程测试
- **TaskClassifierTest**: 自动识别 10 种任务类型(code/translation/analysis/customer_service等) / 复杂度评估(low/medium/high) / 置信度分数
- **CostOptimizationTest**: 低复杂度→便宜模型 / 高复杂度→强模型 / 成本对比(优化前 vs 优化后)
- **AutoFallbackTest**: GPT-4 失败→Claude→Qwen→DeepSeek 多级降级 / 每级超时 / 全部失败后错误处理
- **CostPredictionTest**: 输入 Prompt→预估不同模型 Token 和 Cost / 预测与实际偏差 / 预算超限预警
- **RoutingStatsTest**: 各路由策略 latency/cost/成功率对比 / 路由决策审计

### AI 运营分析 功能测试
- **UsageAnalyticsTest**: 活跃用户统计 / 会话数统计 / Token 趋势(日/周/月) / 同比环比
- **BehaviorAnalyticsTest**: 热门问题 Top N / 热门 Skill Top N / 热门 Tool Top N / 模型偏好分布
- **QualityAnalyticsTest**: 满意度评分趋势 / Tool 成功率 / RAG 命中率 / Hallucination 率估算
- **CostBreakdownTest**: 按模型/部门/用户成本拆解 / 浪费识别(高价模型处理简单任务)
- **ReportGenerationTest**: 周报/月报自动生成 / PDF 导出 / 飞书消息推送
- **OperationScreenTest**: 实时数据刷新 / 大屏适配 / 核心指标轮播

### AI 安全治理 流程测试
- **PromptInjectionFlowTest**: 用户输入→PromptGuard检测→标记可疑→返回警告或阻断 / 误报率验证 / 漏报率验证
- **DataMaskingFlowTest**: 用户输入→DataMasker脱敏→LLM调用→还原脱敏数据→返回用户 / 脱敏后 LLM 不感知 PII
- **OverPermissionDetectionTest**: Tool 调用权限校验 / RAG 检索权限校验 / 越权行为告警
- **IPBlockingTest**: IP 黑名单添加 / 黑名单 IP 请求被拒 / IP 白名单优先 / 黑名单过期自动清理
- **AuditLogTest**: 敏感操作记录(登录/Prompt/Tool/数据访问) / 日志不可篡改 / 日志保留期 / 日志导出

### admin-service 平台运营 功能测试
- **GlobalRateLimitTest**: 按 Provider 全局速率限制 / 超限排队 / 排队超时处理
- **ProviderCircuitBreakerTest**: Provider 异常自动熔断 / 熔断后流量路由到备用 Provider / 半开恢复
- **PlatformAnnouncementTest**: 公告创建 / 按范围推送(all/tenant/user) / 公告过期自动下架
- **TenantManagementTest**: 全局租户列表 / 租户配额管理 / 租户启停

### SDK 测试
- **Python SDK**:
  - ChatClient: 流式聊天 SSE / 非流式聊天 / 异常处理
  - ToolClient: 工具列表 / 工具执行 / 执行超时
  - AgentClient: Agent CRUD / Agent 调试
  - TraceClient: 会话查询 / Span 查询 / 成本分析
- **Java SDK**:
  - ChatClient: 流式聊天 SSE / 回调处理 / 异常处理
  - ToolClient: 工具执行 / 超时 / 重试
  - AgentClient: Agent CRUD / Agent 市场查询
  - TraceClient: 会话查询 / 成本查询

### K8s 部署 + CI/CD 验证
- **HelmChartTest**: values.yaml 语法校验 / 模板渲染验证 / 所有微服务 Deployment 生成
- **HealthCheckTest**: 所有服务 readiness probe / liveness probe / 启动探针
- **HPATest**: Agent Runtime 自动扩缩 / CPU/Memory 阈值触发 / 最小/最大副本数验证
- **CICDPipelineTest**: Java/Python/Frontend Build / Trivy 安全扫描 / Docker 镜像构建推送 / Staging Smoke Test
- **CanaryDeployTest**: 金丝雀 10% 流量 / 监控指标对比 / 自动回滚条件
- **BlueGreenDeployTest**: 蓝绿切换 / 流量一键切换 / 回滚速度

---

## 端到端 (E2E) 集成流程测试 (11 条核心流程)

### E2E-1: 完整对话链 (Full Chat Flow)
```
User Login → Chat API → Gateway Auth → Intent Router → Skill Engine
  → RAG Retrieve → Memory Inject → LLM Call → Tool Execute
  → Response SSE Stream → Trace Record → Billing Record
```
**验证点**: trace_id 全链路一致 / tenant_id 隔离 / 每个 Span type/latency/cost 完整 / SSE 流式输出连续

### E2E-2: 多租户隔离验证
```
Tenant-A 创建 Agent + KB + Tool → Tenant-B 创建 Agent + KB + Tool
  → Tenant-A 用户查询(只能看到 Tenant-A 的资源)
  → Tenant-B 用户查询(只能看到 Tenant-B 的资源)
  → Tenant-A 用户调 RAG(只能检索 Tenant-A 的 Chunk)
```
**验证点**: DB 级隔离 / 向量索引隔离 / Tool 调用隔离 / Memory 隔离 / Trace 隔离

### E2E-3: Agent 生命周期 (创建→发布→市场→安装)
```
创建 Agent → 绑定 Prompt/Tool/Skill/Memory → Draft → Test → Published
  → 调试(提交测试消息,返回完整 Trace) → 发布到市场
  → 其他租户浏览市场 → 搜索 → 安装 → 使用
```
**验证点**: 版本状态流转正确 / 市场数据隔离 / 安装复制完整配置

### E2E-4: Model Fallback 全链路
```
用户提问 → Model Router 选择 GPT-4 → GPT-4 超时 → Fallback Claude
  → Claude 正常返回 → Trace 记录 Fallback 路径 → 成本按 Claude 计费
```
**验证点**: Fallback 链执行顺序 / 超时时间准确 / Trace 记录 Fallback 事件 / 成本按实际使用模型计算

### E2E-5: RAG 文档全生命周期
```
上传 PDF → 解析 → 分块 → Embedding → 索引
  → 用户提问 → Hybrid Search(BM25+Vector) → Rerank → 注入 Prompt
  → LLM 回答(含引用) → 删除文档 → Chunk 级联删除 → 索引清理
```
**验证点**: 文档解析完整性 / Chunk 数量合理 / 检索召回率 / Rerank 提升效果 / 级联删除正确

### E2E-6: Skill 热更新流程
```
创建 Skill v1 → Agent 使用 v1 → 发布 Skill v2
  → Kafka 事件通知 → Agent 缓存刷新 → 新请求使用 v2
```
**验证点**: 热更新延迟(目标 < 5s) / 旧版本请求不受影响 / 回滚同样生效

### E2E-7: 安全攻击→检测→告警 全链路
```
用户输入恶意 Prompt → PromptGuard 检测 → 标记 CRITICAL → 阻断请求
  → 记录审计日志 → 触发 Slack 告警 → IP 加入黑名单(累计多次)
```
**验证点**: 检测延迟 / 阻断生效(LLM 未收到恶意输入) / 审计日志完整 / 告警及时

### E2E-8: 成本计费完整链路
```
用户请求 → LLM Call → Kafka 事件(llm.call) → Billing Service 消费
  → 计算 cost(token × 单价) → 聚合到租户/用户/Agent
  → 接近 Budget 阈值 → 触发告警 → 超出 Budget → 阻断或降级
```
**验证点**: cost 计算正确性 / 聚合实时性 / 告警准确性 / 阻断生效

### E2E-9: Multi-Agent 协作流程
```
Coordinator 接收复杂任务 → 分解为 3 个子任务
  → SearchAgent 执行 RAG 检索 → AnalysisAgent 分析数据
  → ReportAgent 汇总生成报告 → Coordinator 返回最终结果
```
**验证点**: 子任务并行/串行执行正确 / 中间结果传递完整 / 单 Agent 失败不阻塞整体 / Trace 树完整

### E2E-10: Temporal Workflow 容错流程
```
创建 DataCollectionWorkflow → Activity 执行中 Temporal Server 重启
  → Workflow 自动恢复 → 从断点继续执行 → 最终完成
  → 模拟 Activity 持续失败 → 达重试上限 → 触发 Saga 补偿 → 补偿执行成功
```
**验证点**: Workflow 持久化 / 断点续传 / 重试策略 / Saga 补偿完整性

### E2E-11: K8s 金丝雀发布流程
```
部署 v1.0(100%流量) → 部署 v1.1(10%流量)
  → 监控指标(v1.1 error_rate, latency vs v1.0)
  → 指标正常 → 扩大至 50% → 全量切换
  → 指标异常 → 自动回滚至 v1.0
```
**验证点**: 流量分配准确 / 指标对比实时 / 自动回滚触发 / 回滚后服务正常

---

## 跨切面测试 (Cross-Cutting Concerns)

### 多租户数据隔离 (每层验证)
| 层 | 测试项 |
|----|--------|
| DB | 所有 SQL 查询自动注入 tenant_id，跨租户不可见 |
| 向量 | Qdrant 索引按 tenant_id 过滤，跨租户检索不返回 |
| 缓存 | Redis key 包含 tenant_id 前缀，跨租户不可访问 |
| 文件 | MinIO 对象路径包含 tenant_id，跨租户不可访问 |
| Kafka | 消息体必须包含 tenant_id，消费者校验 |
| Trace | 所有 Span 包含 tenant_id，查询时强制过滤 |
| RAG | 检索时注入 tenant_id + department + role 过滤条件 |

### 全链路 Trace 完整性
- 每个请求生成唯一 trace_id
- 所有微服务间调用透传 trace_id
- 所有 AI Span 包含: trace_id, span_id, parent_id, type(llm/tool/rag/memory), latency, cost, tenant_id
- Span 树父子关系正确，可重建完整调用链
- 异常 Span 标记 error=true + error_message

### API 网关规则
- 所有请求经过 Gateway (除 /health 和 /metrics)
- 未认证请求返回 401
- 未授权请求返回 403
- 限流超限返回 429
- 响应头包含 trace_id

### 健康检查
- 每个微服务 /actuator/health 或 /health 端点
- Docker Compose 所有容器健康状态
- K8s readiness/liveness probe 均正常
- 依赖服务不可用时健康检查正确反映状态

---

## 测试覆盖度总结

| Phase | 模块 | 现有测试 | 目标测试 | 缺口 |
|-------|------|---------|---------|------|
| P1 | common-lib | 32 | 32 | 0 |
| P1 | auth-service | 3 | 8+ | agent-service controller/SSO |
| P1 | user-service | 9 | 12+ | UserController/RoleController/隔离 |
| P1 | model-service | 6 | 14+ | ApiKey/Quota/Budget/Fallback |
| P1 | rag-service | 4 | 14+ | Document/Chunk/HybridSearch/Reranker |
| P1 | tool-service | 3 | 12+ | Auth/RateLimit/CircuitBreaker/MCP |
| P1 | agent-service | 4 | 10+ | Config/Version/Debug/Permission |
| P1 | gateway-service | 0 | 12+ | **全部新建** |
| P1 | trace-service | 4 | 10+ | Span/Session/Cost |
| P1 | agent-python | 59 | 70+ | Multi-Agent/Workflow/Security |
| P1 | frontend | tsc | 30+ | 6 子应用 Vitest + Playwright |
| P2 | skill-service | 4 | 10+ | Marketplace/Router/HotReload |
| P2 | prompt-service | 4 | 12+ | ABTest/Grayscale/Template/Token |
| P2 | memory-service | 7 | 12+ | Episodic/Semantic/Procedural/Decay |
| P2 | billing-service | 4 | 8+ | Kafka/Budget/Invoice/Aggregation |
| P2 | trace-service | - | 6+ | Langfuse/OTEL/Grafana/Alert |
| P3 | workflow | 3 | 10+ | Saga/Monitor/Lifecycle |
| P3 | agent-market | 2 | 8+ | Publish/Discover/Install/Review |
| P3 | smart-router | - | 8+ | Classifier/Optimization/Prediction |
| P3 | admin-service | 2 | 8+ | RateLimit/CircuitBreaker/Announcement |
| P3 | audit-service | 1 | 8+ | Injection/Mask/OverPermission/IP/Audit |
| P3 | sdk | 0 | 10+ | **Java + Python SDK 全部新建** |
| P3 | deploy/ci | 0 | 8+ | **Helm/HPA/Canary/CI/CD 全部新建** |
| E2E | 跨服务 | 0 | 11 | **11 条核心流程全部新建** |
| | **合计** | **143** | **280+** | **~140 待补充** |
