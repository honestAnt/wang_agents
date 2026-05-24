# 本地开发环境

## 前置依赖

- Java 21+ (Temurin)
- Python 3.12+
- Node.js 20+
- Docker + Docker Compose
- Maven 3.9+

## 一键启动

```bash
# 1. 启动基础设施（PostgreSQL / Redis / MinIO / Qdrant / OpenSearch / Temporal）
./scripts/init-local.sh

# 2. 启动 Java 后端（Gateway 端口 8080）
cd backend-java && mvn spring-boot:run -pl gateway-service

# 3. 启动 Python Agent Runtime（端口 8000）
cd agent-python && pip install -e ".[dev]" && uvicorn app.main:app --reload

# 4. 启动前端（端口 3000）
cd frontend && npm install && npm run dev
```

## 基础设施端口

| 服务 | 端口 | 管理界面 |
|------|------|----------|
| PostgreSQL | 5432 | — |
| Redis | 6379 | — |
| MinIO | 9000 | :9001 (Console) |
| Qdrant | 6333 | — |
| OpenSearch | 9200 | — |
| Temporal | 7233 | :8080 (UI) |

## 运行测试

```bash
# Java
cd backend-java && mvn test

# Python
cd agent-python && pytest tests/ -v

# Frontend
cd frontend && npm test
```
