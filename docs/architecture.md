# 技术架构

## 架构分层

```
Frontend (Next.js)
  → API Gateway (Spring Cloud Gateway)
    → Java Microservices (Spring Boot)
      → Python Agent Runtime (AgentScope + FastAPI)
        → RAG Engine (LlamaIndex + Qdrant)
        → Tool Mesh (MCP + internal APIs)
        → Model Gateway (LiteLLM → LLM providers)
        → Trace (OpenTelemetry + Langfuse)
```

## 请求链路

```
User → Chat UI → API Gateway → AgentScope Runtime
  → Intent Router → Skill Engine
  → Memory Service (Redis + PostgreSQL)
  → RAG Service (LlamaIndex Hybrid Search)
  → Tool Mesh (MCP Server → Enterprise APIs)
  → Model Gateway (LiteLLM → LLM Provider)
  → Response Stream → Trace Service
```

## 技术栈

| 层 | 技术 |
|----|------|
| **前端** | Next.js (TypeScript), Ant Design Pro, Turborepo |
| **网关** | Spring Cloud Gateway, JWT Auth, Rate Limiting, CORS |
| **后端** | Spring Boot 3.3, Java 21, Maven, Flyway |
| **认证** | Keycloak (RBAC + ABAC), OAuth2 Resource Server |
| **数据库** | PostgreSQL 15, Redis 7 |
| **消息** | Kafka |
| **搜索** | OpenSearch (全文搜索) |
| **向量** | Qdrant |
| **存储** | MinIO (S3-compatible) |
| **AI 运行时** | AgentScope, FastAPI, LiteLLM, LlamaIndex |
| **工作流** | Temporal |
| **可观测性** | OpenTelemetry, Langfuse, Jaeger, Grafana |
| **部署** | Kubernetes, Docker, Helm, GitHub Actions |

## 模型支持

GPT-4.1, Claude Sonnet/Haiku, Gemini, DeepSeek V3, Qwen, vLLM, Ollama

## 核心设计原则

- **多租户**: `tenant_id` 隔离 — DB、索引、向量三级
- **RAG**: Hybrid Search (BM25 + Dense Vector + Metadata Filter)，BGE Reranker，权限感知检索
- **Memory**: 短期 (Redis, Sliding Window) + 长期 (PostgreSQL, 向量记忆) + 情景/语义/过程三类
- **Skills**: `Prompt + Tool + Workflow` 能力封装，动态加载、热更新、版本管理
- **Tools**: HTTP Tool / MCP Tool / SDK Tool / Workflow Tool 四种类型，统一 Tool Router
- **Trace 标准**: `trace_id, span_id, type(llm|tool|rag|memory), latency, cost, tenant_id`
