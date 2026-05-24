#!/usr/bin/env bash
# ============================================================
# Enterprise AI Platform — One-Click Local Init Script
# Usage: bash scripts/init-local.sh
# ============================================================
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }
step() { echo -e "\n${BLUE}=== $* ===${NC}"; }

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

step "Checking prerequisites"

# Check Docker
if ! command -v docker &>/dev/null; then
    err "Docker is not installed. Please install Docker Desktop: https://docs.docker.com/get-docker/"
    exit 1
fi
log "Docker: $(docker --version)"

# Check Docker Compose
if docker compose version &>/dev/null; then
    log "Docker Compose: $(docker compose version)"
elif command -v docker-compose &>/dev/null; then
    log "docker-compose: $(docker-compose --version)"
else
    err "Docker Compose is not available. Please install Docker Desktop."
    exit 1
fi

compose() {
    if docker compose version &>/dev/null 2>&1; then
        docker compose "$@"
    else
        docker-compose "$@"
    fi
}

# Check .env file
step "Checking environment configuration"
if [ ! -f ".env" ]; then
    warn ".env file not found — copying from .env.example"
    cp .env.example .env
    warn "Please edit .env with your real configuration before starting services."
    log "You can continue with defaults for local development."
fi

# Source .env safely
set -a
source .env 2>/dev/null || true
set +a

step "Starting infrastructure services"
compose -f deploy/docker-compose.yml up -d

step "Waiting for services to be healthy"

wait_for() {
    local name=$1
    local timeout=${2:-60}
    local cmd=$3
    local elapsed=0
    log "Waiting for ${name} (timeout: ${timeout}s)..."
    while ! eval "$cmd" &>/dev/null; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [ "$elapsed" -ge "$timeout" ]; then
            err "${name} did not become healthy within ${timeout}s"
            return 1
        fi
    done
    log "${name} is healthy (${elapsed}s)"
}

wait_for "PostgreSQL" 60 "docker exec enterprise-ai-postgres pg_isready -U ${POSTGRES_USER:-wang_agent}"
wait_for "Redis" 30 "docker exec enterprise-ai-redis redis-cli ping"
wait_for "MinIO" 30 "curl -sf http://localhost:${S3_PORT:-9000}/minio/health/live"
wait_for "Qdrant" 30 "curl -sf http://localhost:${QDRANT_PORT:-6333}/health"
wait_for "OpenSearch" 60 "curl -sf http://localhost:${OPENSEARCH_PORT:-9200}"

step "Initializing MinIO bucket"
docker exec enterprise-ai-minio sh -c "
  mc alias set local http://localhost:9000 \${MINIO_ROOT_USER} \${MINIO_ROOT_PASSWORD} && \
  mc mb --ignore-existing local/${S3_BUCKET_NAME:-enterprise-ai-files}
" && log "MinIO bucket '${S3_BUCKET_NAME:-enterprise-ai-files}' ready" || warn "MinIO bucket setup skipped (mc not available or already exists)"

step "Service status"
compose -f deploy/docker-compose.yml ps

echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  All infrastructure services are running!${NC}"
echo -e "${GREEN}============================================================${NC}"
echo ""
echo "  PostgreSQL:   http://localhost:${POSTGRES_PORT:-5432}"
echo "  Redis:         redis://localhost:${REDIS_PORT:-6379}"
echo "  MinIO API:     http://localhost:${S3_PORT:-9000}"
echo "  MinIO Console: http://localhost:${S3_CONSOLE_PORT:-9001}"
echo "  Qdrant:        http://localhost:${QDRANT_PORT:-6333}"
echo "  OpenSearch:    http://localhost:${OPENSEARCH_PORT:-9200}"
echo ""
echo -e "${YELLOW}  Next steps:${NC}"
echo "  1. Edit .env with your LLM API keys"
echo "  2. Start backend services:  cd backend-java && mvn spring-boot:run"
echo "  3. Start agent runtime:     cd agent-python && uvicorn app.main:app --reload"
echo "  4. Start frontend:          cd frontend && pnpm dev"
echo ""
