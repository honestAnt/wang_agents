# Kubernetes Production Deployment

## Prerequisites

- Kubernetes 1.28+
- Helm 3.14+
- cert-manager (for TLS)
- nginx-ingress-controller
- kubectl configured

## Quick Start

```bash
# Add required Helm repos
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add temporal https://go.temporal.io/helm-charts

# Install the platform
helm upgrade --install enterprise-ai ./helm/enterprise-ai-platform \
  --namespace enterprise-ai --create-namespace \
  --set global.imageRegistry=your-registry \
  --set gateway.ingress.host=api.your-domain.com

# Check status
kubectl get pods -n enterprise-ai
kubectl get ingress -n enterprise-ai
```

## Deployment Strategies

- **Blue/Green**: Set `deployment.strategy: blueGreen` in values
- **Canary**: Use `--set canary.enabled=true --set canary.weight=10`

## Rolling Back

```bash
helm rollback enterprise-ai -n enterprise-ai
```

## Monitoring

Access Grafana at `http://grafana.enterprise-ai.local` with admin credentials from
`kubectl get secret grafana-admin -n enterprise-ai -o jsonpath="{.data.admin-password}" | base64 -d`
