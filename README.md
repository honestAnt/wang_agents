[English](./README.md) | [中文](./README.zh-CN.md)

# Enterprise Agent Platform (EAP)

An enterprise-grade, multi-tenant AI Agent platform built on [AgentScope](https://github.com/modelscope/agentscope). It is not just a chatbot — it is an **Enterprise AI Operating System (AI OS)** that unifies knowledge bases, model governance, agent runtime, skills, tools, memory, workflows, multi-tenancy, and AI observability into a single platform.

## Target Users

- **Internal enterprise**: AI platform teams, IT departments, digital transformation teams, data platform teams
- **SaaS scenarios**: Intelligent customer service, enterprise knowledge assistant, digital employee, enterprise Copilot, lab AI assistant, workflow automation

## Core Capabilities

| Module | Description |
|---------|-------------|
| **Enterprise RAG** | Document ingestion → parsing → chunking → embedding → hybrid search (BM25 + vector + metadata filter) with permission-aware retrieval |
| **Intelligent Chat** | Multi-turn conversation, streaming output, multimodal input, agent collaboration, model switching |
| **Agent Runtime** | AgentScope-powered execution loop (reason → act → observe), planner, router, executor |
| **Skills System** | `Prompt + Tool + Workflow` capability encapsulation with dynamic loading, hot reload, and versioning |
| **Tool Registry** | Unified enterprise tool hub supporting HTTP/MCP/SDK/Workflow tools with rate limiting, circuit breaking, and auditing |
| **Model Gateway** | LiteLLM-based multi-model governance (GPT, Claude, Gemini, Qwen, DeepSeek, vLLM, Ollama) with quota, budget, routing, and fallback |
| **Memory System** | Short-term (Redis + sliding context) + Long-term (PostgreSQL + vector memory) — episodic, semantic, procedural |
| **Multi-Tenancy** | tenant_id–based isolation at DB, index, and vector levels; RBAC + ABAC permission model |
| **Trace & Audit** | Full-chain trace (User → Agent → Skill → RAG → Tool → LLM), OpenTelemetry + Langfuse, cost tracking, AI replay, prompt injection detection |

## Architecture

```
Frontend (Next.js)
  → API Gateway (Spring Cloud Gateway)
    → Java Microservices (Spring Boot — auth, user, agent, model, rag, tool, etc.)
      → Python Agent Runtime (AgentScope + FastAPI)
        → RAG Engine (LlamaIndex + Qdrant/Milvus)
        → Tool Mesh (MCP + internal APIs)
        → Model Gateway (LiteLLM → LLM providers)
        → Trace (OpenTelemetry + Langfuse)

Data Layer: PostgreSQL / Redis / OpenSearch / Qdrant|Milvus / Kafka / MinIO
```

## Tech Stack

### Frontend
- **Next.js** (TypeScript) — main site + admin console
- **Ant Design Pro** — enterprise admin UI
- **Turborepo** — monorepo management

### Backend Services (Java)
- **Spring Boot** microservices
- **Spring Cloud Gateway** — API gateway
- **Keycloak** — IAM (RBAC + ABAC)
- **PostgreSQL** — primary OLTP database
- **Redis** — session cache, short-term memory, rate limiting
- **Kafka** — event bus
- **OpenSearch** — full-text search, trace logs

### AI Runtime (Python)
- **AgentScope** — agent execution engine
- **LiteLLM** — model gateway (unified API, fallback, routing, cost tracking)
- **LlamaIndex** — RAG engine
- **Qdrant** (dev/test) / **Milvus** (production) — vector database
- **Langfuse** — LLM observability
- **OpenTelemetry** — distributed tracing
- **Temporal** / **LangGraph** — workflow orchestration (Phase 3)

### Model Support
GPT-4.1, Claude, Gemini, Qwen, DeepSeek, vLLM, Ollama

### Infrastructure
- **Kubernetes** + **Docker** + **Helm**
- **MinIO / S3** — object storage
- **MCP** (Model Context Protocol) — tool mesh standard

## Project Structure (Planned)

```
enterprise-ai-platform/
├── docs/                  # PRD / architecture / API docs
├── deploy/                # k8s / helm / docker-compose
├── scripts/               # init scripts
├── frontend/              # Next.js monorepo
│   ├── apps/
│   │   ├── chat-ui/       # User chat workspace
│   │   ├── admin-console/ # Admin management
│   │   ├── agent-studio/  # Agent configuration studio
│   │   ├── model-center/  # Model governance
│   │   ├── rag-studio/    # RAG debugging & management
│   │   └── trace-console/ # Trace & audit console
│   └── packages/
│       ├── ui-components/ # Shared UI components
│       ├── api-client/    # Shared API client
│       ├── auth/          # Shared auth module
│       └── utils/         # Utility functions
├── backend-java/          # Spring Boot microservices
│   ├── common-lib/        # Shared library
│   ├── gateway-service/   # API Gateway
│   ├── auth-service/      # Keycloak IAM integration
│   ├── user-service/      # User/tenant/organization
│   ├── agent-service/     # Agent configuration
│   ├── model-service/     # Model governance
│   ├── rag-service/       # Knowledge base / RAG
│   ├── tool-service/      # Tool registry
│   ├── skill-service/     # Skills management
│   ├── memory-service/    # Long-term memory
│   ├── trace-service/     # Trace & audit
│   ├── billing-service/   # Cost billing
│   ├── prompt-service/    # Prompt engineering platform
│   ├── audit-service/     # Security audit
│   └── admin-service/     # Admin API aggregation
├── agent-python/          # AgentScope runtime (FastAPI)
│   └── app/
│       ├── core/          # Agent engine, planner, router, executor, memory, context
│       ├── agents/        # chat_agent, rag_agent, workflow_agent, multi_agent
│       ├── skills/        # data_analysis, customer_service, research
│       ├── tools/         # tool_client, mcp_client, internal_tools
│       ├── rag/           # retriever, reranker, embedding
│       ├── memory/        # short_term, long_term, vector_memory
│       ├── llm/           # litellm_client, model_router
│       ├── trace/         # tracer, exporter
│       └── api/           # chat, agent, debug
├── sdk/                   # Internal SDKs
│   ├── java-sdk/
│   ├── python-sdk/
│   └── openapi/
├── infra/                 # Infrastructure configs
│   ├── lite-llm-proxy/
│   ├── mcp-servers/
│   ├── vector-db/
│   ├── search/
│   ├── message-queue/
│   └── observability/
└── shared/                # Shared protocols
    ├── proto/             # gRPC definitions
    ├── openapi/           # REST API schemas
    ├── event-schema/      # Kafka event definitions
    └── trace-model/       # Unified trace span schema
```

## Development Roadmap

### Phase 1 — MVP
**Goal**: Multi-tenant AI chat platform with basic RAG, tool calling, and multi-model switching
- Monorepo setup + Docker Compose local infrastructure
- Core Java microservices (auth, user, model, rag, tool, agent, gateway, trace)
- Python Agent Runtime (AgentScope + FastAPI + LiteLLM + LlamaIndex)
- Frontend: Login, Chat UI, Dashboard, Agent/Model/KB management, Trace Console

### Phase 2 — Advanced
**Goal**: Skills marketplace, long-term memory, multi-agent, prompt platform, full observability
- Skill service + skill engine + skill marketplace
- Long-term memory service (episodic/semantic/procedural)
- Multi-Agent collaboration framework
- Prompt Center (versioning, AB test, canary release)
- Langfuse + OpenTelemetry full integration
- Billing service

### Phase 3 — Enterprise
**Goal**: AI OS — workflows, agent marketplace, intelligent routing, operations analytics, security governance
- Temporal AI workflow engine
- Agent marketplace
- Intelligent model routing + auto fallback
- AI operations analytics center
- Security governance (prompt injection detection, data masking, privilege escalation detection)
- SDK release + K8s production deployment + CI/CD

## Getting Started (Prerequisites)

### Local Development Dependencies

The `.env` file expects these services running locally:

| Service | Port | Credentials |
|---------|------|-------------|
| PostgreSQL | 5432 | `opc_compliance` / `opc_123` |
| MinIO | 9000 | `minioadmin` / `minioadmin123` |
| Redis | 6379 | — |
| Qdrant | 6333 | — |
| OpenSearch | 9200 | — |

### Quick Start (Coming Soon)

```bash
# 1. Start infrastructure
docker compose -f deploy/docker-compose.yml up -d

# 2. Initialize databases
bash scripts/init-local.sh

# 3. Start backend services
cd backend-java && ./mvnw spring-boot:run

# 4. Start agent runtime
cd agent-python && uvicorn app.main:app --reload

# 5. Start frontend
cd frontend && pnpm dev
```

## Key Design Decisions

- **Trace standard**: Every span MUST emit `trace_id`, `span_id`, `type` (llm|tool|rag|memory), `latency`, `cost`, `tenant_id`
- **Permission-aware RAG**: Different users can only retrieve data they have access to, filtered by `tenant_id`, `department`, `role`, `project`
- **Data isolation**: All SQL queries auto-inject `tenant_id` filter; vector and search indices are tenant-isolated
- **MCP-first tools**: Model Context Protocol is the recommended standard for enterprise tool integration
- **Skills as first-class citizens**: `Prompt + Tool + Workflow` encapsulation enables reusable AI capabilities across tenants

## Current Status

**Planning phase** — architecture documents complete, code implementation not started.

See `dev-map/` for design documents:
- `企业智能体prd.docx` — Full PRD
- `企业智能体技术架构.docx` — Technical architecture
- `企业智能体页面设计.docx` — UI/UX page design
- `企业智能体工程目录实现.docx` — Monorepo directory structure

## License

TBD
