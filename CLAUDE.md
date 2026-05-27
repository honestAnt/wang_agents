# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 必须遵守

- dev-task 和 dev-map 两个目录下的文件坚决不能删除
- 每次工作完都要更新工作进度，工作进度文件放在 dev-task 下面的 dev_state.json 中；编译和测试通过后提交代码到git仓库
- 每次新会话开始都要读取 dev-task/dev_state.json 中的进度，然后继续开始
- 每个阶段任务完成后，新增单元测试和测试用例,然后按照测试用例跑一下流程是否满足需求
- 每次工作完或者对话任务结束后，写个工作总结，记录遇到了哪些问题，怎么解决的，沉淀好，方便后续遇到类似问题时能快速找到解决方法,放在dev-task目录下面

## 文档索引

详细文档在 `docs/` 目录，按主题拆分：

| 文档 | 内容 |
|------|------|
| [docs/README.md](docs/README.md) | 项目总览 + 目录导航 |
| [docs/architecture.md](docs/architecture.md) | 架构分层、请求链路、技术栈、设计原则 |
| [docs/getting-started.md](docs/getting-started.md) | 本地开发环境搭建 |
| [docs/frontend.md](docs/frontend.md) | 前端 6 子应用 + 4 共享包 |
| [docs/backend-java.md](docs/backend-java.md) | 14 个 Java 微服务说明 |
| [docs/agent-python.md](docs/agent-python.md) | Python Agent Runtime 模块说明 |
| [docs/sdk.md](docs/sdk.md) | Java / Python SDK 用法 |
| [docs/infra.md](docs/infra.md) | 基础设施服务 (Temporal/Kafka/Qdrant 等) |
| [docs/deploy.md](docs/deploy.md) | K8s Helm + CI/CD Pipeline |
| [docs/shared.md](docs/shared.md) | 共享协议 (OpenAPI/Proto/Event/Trace) |

PRD / 架构设计文档在 `dev-map/` 目录（.docx 格式）。

## 项目概要

企业级智能体平台（Enterprise Agent Platform）— 基于 AgentScope 的多租户企业 AI Agent 中台，最终形态是企业 AI Operating System (AI OS)。

### 目录结构

```
├── docs/                     # 项目文档 (按主题拆分)
├── dev-map/                  # PRD / 架构 / 页面设计 (.docx)
├── dev-task/                 # 开发任务 + 进度跟踪 (dev_state.json)
├── frontend/                 # Next.js monorepo — 6 apps + 4 packages
├── backend-java/             # Spring Boot — 14 个微服务
├── agent-python/             # Python Agent Runtime (FastAPI + AgentScope)
├── sdk/                      # Java SDK + Python SDK
├── infra/                    # 基础设施 docker-compose 配置
├── deploy/                   # Helm Chart + K8s 部署 + CI/CD
├── shared/                   # OpenAPI / Proto / Event Schema / Trace Model
└── scripts/                  # 初始化脚本
```

### 请求链路

```
User → Chat UI → API Gateway → AgentScope Runtime
  → Intent Router → Skill Engine → Memory → RAG → Tool Mesh
  → Model Gateway (AgentScope) → LLM Provider → Response Stream → Trace
```

### 技术栈

- **前端**: Next.js (TypeScript), Ant Design Pro, Turborepo
- **后端**: Spring Boot 3.3, Java 21, Maven, Flyway, Keycloak
- **AI 运行时**: FastAPI + AgentScope + LlamaIndex
- **数据**: PostgreSQL 15, Redis 7, Kafka, OpenSearch, Qdrant, MinIO
- **工作流**: Temporal
- **可观测性**: OpenTelemetry + Langfuse + Jaeger + Grafana
- **部署**: Kubernetes + Helm + Docker + GitHub Actions

### 核心设计约束

- **多租户**: `tenant_id` 隔离 — DB / 索引 / 向量三级
- **Trace 标准**: 所有 AI Span 必须含 `trace_id, span_id, type(llm|tool|rag|memory), latency, cost, tenant_id`
- **API 网关**: 所有请求经 Gateway（除 `/health` 和 `/metrics`）
- **SQL 过滤**: 所有查询自动注入 `tenant_id`
- **Kafka 消息**: 必须包含 `trace_id` 和 `tenant_id`

## 测试命令

```bash
# Python (agent-python 目录下执行)
.venv/bin/python -m pytest tests/ -v

# Java (仓库根目录，需 JDK 21)
JAVA_HOME=/path/to/jdk-21 mvn -f backend-java/pom.xml test -q
```

## 当前状态

Phase 1 (MVP) + Phase 2 (进阶) + Phase 3 (企业级) 全部完成 — 39/39 任务。
143 个 Python 测试 + Java 后端测试全部通过。
