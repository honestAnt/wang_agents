# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 必须遵守

- dev-task 和 dev-map 两个目录下的文件坚决不能删除
- 每次工作完都要更新工作进度，工作进度文件放在 dev-task 下面的 dev_state.json 中
- 每次新会话开始都要读取 dev-task/dev_state.json 中的进度，然后继续开始

## Project Overview

企业级智能体平台（Enterprise Agent Platform）— 基于 AgentScope 构建的多租户企业 AI Agent 中台。最终形态是企业 AI Operating System（AI OS），不仅是聊天机器人。

Core capabilities: 企业知识库、多模型治理、Agent Runtime、Skills 管理、Tool Registry、Memory、Workflow、多租户权限、AI 可观测性。

Architecture documents live in `dev-map/`:
- `企业智能体prd.docx` — full PRD (product requirements)
- `企业智能体技术架构.docx` — production-grade technical architecture
- `企业智能体页面设计.docx` — UI/UX page design PRD
- `企业智能体工程目录实现.docx` — monorepo directory structure and service boundaries

## Monorepo Structure (planned)

```
enterprise-ai-platform/
├── docs/              # PRD / architecture / API docs
├── deploy/            # k8s / helm / docker-compose
├── scripts/           # init scripts
├── frontend/          # Next.js (chat-ui, admin-console, agent-studio, model-center, rag-studio, trace-console)
├── backend-java/      # Spring Boot microservices (gateway, auth, user, agent, model, rag, tool, skill, memory, trace, billing, admin)
├── agent-python/      # AgentScope runtime (FastAPI + AgentScope + LiteLLM + LlamaIndex)
├── sdk/               # Internal SDKs (java-sdk, python-sdk, openapi)
├── infra/             # Infrastructure (lite-llm-proxy, mcp-servers, vector-db, search, message-queue, observability)
└── shared/            # Shared protocols (proto, openapi, event-schema, trace-model)
```

## Tech Stack

### Frontend
- **Next.js** (TypeScript) — main site + admin console
- **React Admin** / **Ant Design Pro** — enterprise admin

### Backend Services (Java)
- **Spring Boot** microservices
- **Keycloak** — auth / IAM (RBAC + ABAC)
- **PostgreSQL** — primary database (users, tenants, agents, skills, tools, sessions, messages)
- **Redis** — session cache, short-term memory, rate limiting
- **Kafka** — event bus
- **OpenSearch** — conversation logs, trace logs, full-text search

### AI Runtime (Python)
- **AgentScope** — agent execution engine (reason → act → observe loop)
- **LiteLLM** — model gateway (unified API, fallback, routing, retry, cost tracking)
- **LlamaIndex** — RAG engine
- **Qdrant** / **Qdrant** — vector database
- **Langfuse** — LLM observability
- **OpenTelemetry** — distributed tracing

### Model Support
GPT-4.1, Claude, Gemini, Qwen, DeepSeek, vLLM, Ollama

### Infrastructure
- **Kubernetes** + **Docker** + **Helm**
- **MinIO / S3** — object storage
- **Temporal** / **LangGraph** — workflow orchestration
- **MCP** (Model Context Protocol) — tool mesh standard

## Architecture Layers

```
Frontend (Next.js)
  → API Gateway (Spring Cloud Gateway / Kong)
    → Java Microservices (Spring Boot — auth, user, agent, model, rag, tool, etc.)
      → Python Agent Runtime (AgentScope + FastAPI)
        → RAG Engine (LlamaIndex + Qdrant)
        → Tool Mesh (MCP + internal APIs)
        → Model Gateway (LiteLLM → LLM providers)
        → Trace (OpenTelemetry + Langfuse)
```

Request flow: User → Chat UI → API Gateway → Conversation Service → AgentScope Runtime → Intent Router → Skill Engine → Memory Service → RAG Service → Tool Mesh → Model Gateway → LLM Provider → Response Stream → Trace Service

## Key Design Points

- **Multitenancy**: `tenant_id`–based data isolation at DB, index, and vector levels
- **RAG**: hybrid search (BM25 + dense vector + metadata filter), reranker (BGE), permission-aware retrieval
- **Memory**: short-term (Redis, sliding context) + long-term (PostgreSQL, vector memory) + episodic/semantic/procedural
- **Skills**: `Prompt + Tool + Workflow` capability encapsulation with dynamic loading, hot reload, versioning
- **Tools**: HTTP Tool, MCP Tool, SDK Tool, Workflow Tool — unified via Tool Router → MCP Server → Enterprise APIs
- **Trace standard** (all services must emit): `trace_id`, `span_id`, `type` (llm|tool|rag|memory), `latency`, `cost`, `tenant_id`

## Current State

Project is in planning phase. No source code exists yet — only architecture documents in `dev-map/` and dotfiles (`.env`, `.gitignore`, `.gitattributes`, `.dockerignore`). The `.env` file points to local PostgreSQL (`opc_compliance` database) and MinIO (`opc-files` bucket).
