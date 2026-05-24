# 企业级智能体平台 (Enterprise Agent Platform)

基于 AgentScope 构建的多租户企业 AI Agent 中台，最终形态是 **企业 AI Operating System (AI OS)**。

## 文档导航

| 文档 | 说明 |
|------|------|
| [architecture.md](architecture.md) | 整体架构、请求链路、技术选型 |
| [getting-started.md](getting-started.md) | 本地开发环境搭建 |
| [frontend.md](frontend.md) | 前端 6 个子应用 + 4 个共享包 |
| [backend-java.md](backend-java.md) | 14 个 Java 微服务 |
| [agent-python.md](agent-python.md) | Python Agent Runtime |
| [sdk.md](sdk.md) | Java / Python SDK |
| [infra.md](infra.md) | 基础设施（Temporal、MQ、Vector DB 等） |
| [deploy.md](deploy.md) | Kubernetes 部署 + CI/CD |
| [shared.md](shared.md) | 共享协议（OpenAPI、Proto、Event Schema、Trace Model） |

## 目录结构

```
wang_agents/
├── docs/                     # 项目文档
├── dev-map/                  # PRD / 架构 / 页面设计
├── dev-task/                 # 开发任务跟踪
├── frontend/                 # Next.js 前端 (monorepo)
│   ├── apps/                 # 6 个子应用
│   └── packages/             # 4 个共享包
├── backend-java/             # Spring Boot 微服务 (14 个)
├── agent-python/             # Python Agent Runtime
├── sdk/                      # Java SDK + Python SDK
├── infra/                    # 基础设施 Docker Compose
├── deploy/                   # Helm Chart + K8s 部署文档
├── shared/                   # 共享协议定义
├── scripts/                  # 初始化脚本
└── .github/workflows/        # CI/CD (GitHub Actions)
```
