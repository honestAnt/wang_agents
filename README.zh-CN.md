[English](./README.md) | [中文](./README.zh-CN.md)

# 企业级智能体平台（Enterprise Agent Platform）

基于 [AgentScope](https://github.com/modelscope/agentscope) 构建的企业级多租户 AI Agent 平台。它不仅是聊天机器人——而是**企业 AI 操作系统（AI OS）**，统一了知识库、多模型治理、Agent Runtime、Skills、Tool Registry、Memory、Workflow、多租户权限和 AI 可观测性。

## 目标用户

- **企业内部**：AI 平台团队、IT 部门、数字化部门、数据平台团队
- **SaaS 场景**：智能客服、企业知识助手、数字员工、企业 Copilot、实验室 AI 助手、工作流自动化

## 核心能力

| 模块 | 说明 |
|------|------|
| **企业知识库（RAG）** | 文档导入 → 解析 → 分块 → Embedding → Hybrid Search（BM25 + 向量 + 元数据过滤），支持权限感知检索 |
| **智能聊天** | 多轮对话、流式输出、多模态输入、Agent 协同、模型切换 |
| **Agent Runtime** | 基于 AgentScope 的执行循环（reason → act → observe），Planner / Router / Executor |
| **Skills 系统** | `Prompt + Tool + Workflow` 能力封装，支持动态加载、热更新、版本管理和技能市场 |
| **Tool Registry** | 统一企业工具接入中心，支持 HTTP/MCP/SDK/Workflow 工具，含限流/熔断/审计 |
| **模型网关** | 基于 LiteLLM 的多模型治理（GPT、Claude、Gemini、Qwen、DeepSeek、vLLM、Ollama），支持 Quota、Budget、路由、Fallback |
| **Memory 系统** | 短期记忆（Redis + 滑动上下文窗口）+ 长期记忆（PostgreSQL + 向量记忆）— 情景/语义/过程三类记忆 |
| **多租户** | tenant_id 级别的 DB/索引/向量三级数据隔离；RBAC + ABAC 权限模型 |
| **Trace & Audit** | 全链路追踪（用户 → Agent → Skill → RAG → Tool → LLM），OpenTelemetry + Langfuse，成本统计，AI 回放，Prompt 注入检测 |

## 架构总览

```
前端 (Next.js)
  → API Gateway (Spring Cloud Gateway)
    → Java 微服务 (Spring Boot — auth, user, agent, model, rag, tool 等)
      → Python Agent Runtime (AgentScope + FastAPI)
        → RAG 引擎 (LlamaIndex + Qdrant/Milvus)
        → 工具网格 (MCP + 内部 API)
        → 模型网关 (LiteLLM → LLM 厂商)
        → 链路追踪 (OpenTelemetry + Langfuse)

数据层: PostgreSQL / Redis / OpenSearch / Qdrant|Milvus / Kafka / MinIO
```

## 技术栈

### 前端
- **Next.js** (TypeScript) — 主站 + 管理后台
- **Ant Design Pro** — 企业后台 UI
- **Turborepo** — monorepo 管理

### 后端服务（Java）
- **Spring Boot** 微服务
- **Spring Cloud Gateway** — API 网关
- **Keycloak** — IAM 认证（RBAC + ABAC）
- **PostgreSQL** — 核心业务数据库
- **Redis** — 会话缓存、短期记忆、限流
- **Kafka** — 事件总线
- **OpenSearch** — 全文搜索、Trace 日志

### AI Runtime (Python)
- **AgentScope** — Agent 执行引擎
- **LiteLLM** — 模型网关（统一 API / Fallback / 路由 / 成本追踪）
- **LlamaIndex** — RAG 引擎
- **Qdrant**（开发/测试）/ **Milvus**（生产）— 向量数据库
- **Langfuse** — LLM 可观测性
- **OpenTelemetry** — 分布式追踪
- **Temporal** / **LangGraph** — 工作流编排（Phase 3）

### 模型支持
GPT-4.1、Claude、Gemini、Qwen、DeepSeek、vLLM、Ollama

### 基础设施
- **Kubernetes** + **Docker** + **Helm**
- **MinIO / S3** — 对象存储
- **MCP** (Model Context Protocol) — 工具网格标准协议

## 项目结构（规划中）

```
enterprise-ai-platform/
├── docs/                  # PRD / 架构 / API 文档
├── deploy/                # k8s / helm / docker-compose
├── scripts/               # 初始化脚本
├── frontend/              # Next.js 前端 monorepo
│   ├── apps/
│   │   ├── chat-ui/       # 用户聊天工作台
│   │   ├── admin-console/ # 企业管理后台
│   │   ├── agent-studio/  # Agent 配置中心
│   │   ├── model-center/  # 模型治理中心
│   │   ├── rag-studio/    # RAG 调试与管理
│   │   └── trace-console/ # Trace & Audit 控制台
│   └── packages/
│       ├── ui-components/ # 共享 UI 组件
│       ├── api-client/    # 共享 API 客户端
│       ├── auth/          # 共享认证模块
│       └── utils/         # 工具函数
├── backend-java/          # Spring Boot 微服务
│   ├── common-lib/        # 公共依赖库
│   ├── gateway-service/   # API 网关
│   ├── auth-service/      # Keycloak IAM 集成
│   ├── user-service/      # 用户/租户/组织
│   ├── agent-service/     # Agent 配置管理
│   ├── model-service/     # 模型治理
│   ├── rag-service/       # 知识库 / RAG
│   ├── tool-service/      # Tool Registry
│   ├── skill-service/     # Skills 管理
│   ├── memory-service/    # 长期记忆
│   ├── trace-service/     # Trace & Audit
│   ├── billing-service/   # 成本计费
│   ├── prompt-service/    # Prompt 工程平台
│   ├── audit-service/     # 安全审计
│   └── admin-service/     # 管理后台 API 聚合
├── agent-python/          # AgentScope Runtime (FastAPI)
│   └── app/
│       ├── core/          # Agent 引擎、规划器、路由器、执行器、记忆、上下文
│       ├── agents/        # chat_agent, rag_agent, workflow_agent, multi_agent
│       ├── skills/        # data_analysis, customer_service, research
│       ├── tools/         # tool_client, mcp_client, internal_tools
│       ├── rag/           # retriever, reranker, embedding
│       ├── memory/        # short_term, long_term, vector_memory
│       ├── llm/           # litellm_client, model_router
│       ├── trace/         # tracer, exporter
│       └── api/           # chat, agent, debug
├── sdk/                   # 内部 SDK
│   ├── java-sdk/
│   ├── python-sdk/
│   └── openapi/
├── infra/                 # 基础设施配置
│   ├── lite-llm-proxy/
│   ├── mcp-servers/
│   ├── vector-db/
│   ├── search/
│   ├── message-queue/
│   └── observability/
└── shared/                # 共享协议层
    ├── proto/             # gRPC 定义
    ├── openapi/           # REST API Schema
    ├── event-schema/      # Kafka 事件定义
    └── trace-model/       # 统一 Trace Span 模型
```

## 开发路线图

### Phase 1 — MVP
**目标**：多租户 AI 聊天平台，支持基础 RAG、Tool 调用、多模型切换
- Monorepo 搭建 + Docker Compose 本地开发环境
- 核心 Java 微服务（auth, user, model, rag, tool, agent, gateway, trace）
- Python Agent Runtime（AgentScope + FastAPI + LiteLLM + LlamaIndex）
- 前端：登录页、聊天工作台、Dashboard、Agent/模型/KB 管理、Trace 控制台

### Phase 2 — 进阶
**目标**：Skills 技能市场、长期记忆、多 Agent 协作、Prompt 工程平台、全链路可观测
- Skill 服务 + Skill 引擎 + 技能市场
- 长期记忆服务（情景/语义/过程记忆）
- Multi-Agent 协作框架
- Prompt Center（版本管理、灰度发布、AB Test）
- Langfuse + OpenTelemetry 完整集成
- 计费服务

### Phase 3 — 企业级
**目标**：AI OS — AI Workflow、Agent 市场、智能路由、AI 运营分析、安全治理
- Temporal AI Workflow 引擎
- Agent 市场
- 智能模型路由 + 自动降级
- AI 运营分析中心
- 安全治理（Prompt Injection 检测、数据脱敏、越权检测）
- SDK 发布 + K8s 生产部署 + CI/CD

## 本地开发前置条件

### 本地依赖服务

`.env` 文件期望以下服务在本地运行：

| 服务 | 端口 | 凭证 |
|------|------|------|
| PostgreSQL | 5432 | `opc_compliance` / `opc_123` |
| MinIO | 9000 | `minioadmin` / `minioadmin123` |
| Redis | 6379 | — |
| Qdrant | 6333 | — |
| OpenSearch | 9200 | — |

### 快速启动（即将就绪）

```bash
# 1. 启动基础设施
docker compose -f deploy/docker-compose.yml up -d

# 2. 初始化数据库
bash scripts/init-local.sh

# 3. 启动后端服务
cd backend-java && ./mvnw spring-boot:run

# 4. 启动 Agent Runtime
cd agent-python && uvicorn app.main:app --reload

# 5. 启动前端
cd frontend && pnpm dev
```

## 核心设计决策

- **统一 Trace 标准**：所有 Span 必须包含 `trace_id`, `span_id`, `type` (llm|tool|rag|memory), `latency`, `cost`, `tenant_id`
- **权限感知 RAG**：不同用户只能检索到有权限的数据，按 `tenant_id`、`department`、`role`、`project` 过滤
- **数据隔离**：所有 SQL 查询自动注入 `tenant_id` 过滤；向量库和搜索引擎按租户隔离
- **MCP 优先**：Model Context Protocol 是企业工具接入的推荐标准
- **Skill 一等公民**：`Prompt + Tool + Workflow` 封装使 AI 能力可跨租户复用

## 当前状态

**规划阶段** — 架构文档已完成，尚未开始代码实现。

设计文档见 `dev-map/` 目录：
- `企业智能体prd.docx` — 完整产品需求文档
- `企业智能体技术架构.docx` — 技术架构设计
- `企业智能体页面设计.docx` — UI/UX 页面设计
- `企业智能体工程目录实现.docx` — 代码仓库目录结构

## 版本

当前版本：**v0.1.0-alpha**（规划阶段）

详见 [CHANGELOG](./CHANGELOG.md) 版本历史。

## 许可证

版权所有 (c) 2026 [fengbao.wang](mailto:wangfengbaowfb@gmail.com)。保留所有权利。

- **个人及学术使用**：免费 — 可用于个人项目和非商业性学术研究，自由使用、复制、修改。
- **商业使用**：须事先取得作者的书面授权。商业授权请联系 [wangfengbaowfb@gmail.com](mailto:wangfengbaowfb@gmail.com)。

完整条款见 [LICENSE](./LICENSE)。

## 免责声明

本软件按"原样"提供，不提供任何明示或暗示的保证。作者不对因使用本软件而产生的任何损害承担责任。使用风险自负。
