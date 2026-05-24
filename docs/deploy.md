# Kubernetes 部署 + CI/CD

## 结构

```
deploy/
├── docker-compose.yml   # 本地开发基础设施 (8 个服务)
├── helm/
│   └── enterprise-ai-platform/
│       ├── Chart.yaml
│       ├── values.yaml                    # 14 微服务 + 基础设施配置
│       └── templates/
│           ├── deployment.yaml            # 通用 Deployment 模板
│           └── ingress.yaml               # Ingress + TLS
└── k8s/
    └── README.md                          # K8s 部署文档
```

## Helm Chart

values.yaml 覆盖的配置项：
- **14 个微服务**: 副本数、镜像、资源限制、健康检查
- **Agent Runtime**: 4 副本，HPA 自动扩缩 (2–20)，2–8 Gi 内存
- **基础设施**: PostgreSQL/Redis/Qdrant/OpenSearch/Kafka 持久化存储
- **可观测性**: Langfuse / OTEL Collector / Grafana
- **Ingress**: TLS (cert-manager)、SSE 流式代理

## 部署策略

- **Blue/Green**: 设置 `deployment.strategy: blueGreen`
- **Canary**: `--set canary.enabled=true --set canary.weight=10`

## CI/CD Pipeline (GitHub Actions)

```
.github/workflows/ci.yml
```

流水线阶段：
1. **Java Build**: Maven 编译 + 测试 (JDK 21)
2. **Python Build**: pytest (143 tests)
3. **Frontend Build**: npm ci + lint + build
4. **Security Scan**: Trivy 漏洞扫描 (HIGH/CRITICAL)
5. **Docker Build**: 13 个服务镜像构建 + 推送到 Registry
6. **Deploy Staging**: Helm 自动部署到 Staging 环境，Smoke Test
7. **Deploy Production**: 手动审批后 Canary 部署 (10% 流量)
