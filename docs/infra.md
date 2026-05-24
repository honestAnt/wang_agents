# 基础设施 (Infrastructure)

本地开发和 CI/CD 依赖的基础设施服务。

## 结构

```
infra/
├── temporal/         # Temporal Server + UI (docker-compose)
├── lite-llm-proxy/   # LiteLLM 代理配置
├── mcp-servers/      # MCP Server 配置
├── message-queue/    # Kafka 配置
├── observability/    # OpenTelemetry Collector / Grafana
├── vector-db/        # Qdrant 配置
└── search/           # OpenSearch 配置
```

## Docker Compose 服务 (deploy/docker-compose.yml)

| 服务 | 镜像 | 端口 | 用途 |
|------|------|------|------|
| PostgreSQL 15 | postgres:15-alpine | 5432 | 主数据库 |
| Redis 7 | redis:7-alpine | 6379 | 缓存/短期记忆/限流 |
| MinIO | minio/minio | 9000, 9001 | S3 对象存储 |
| Qdrant | qdrant/qdrant | 6333, 6334 | 向量数据库 |
| OpenSearch | opensearch:2 | 9200, 9600 | 全文搜索+日志 |
| Temporal | temporalio/auto-setup | 7233, 5433 | 工作流引擎 |
| Temporal UI | temporalio/ui | 8080 | 工作流监控界面 |
