#!/usr/bin/env bash
# ============================================================
# Enterprise AI Platform — One-Click Local Startup
# Usage:
#   bash scripts/start-local.sh              # full stack (default)
#   bash scripts/start-local.sh backend      # only Java backend
#   bash scripts/start-local.sh agent        # only Python agent
#   bash scripts/start-local.sh frontend     # only frontend
#   bash scripts/start-local.sh stop         # stop all app processes
#   bash scripts/start-local.sh status       # show running status
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

PID_DIR="/tmp/enterprise-ai-pids"
mkdir -p "$PID_DIR"

# ============================================================
# Start Java backend (single agent-application JAR)
# ============================================================
start_backend() {
    step "Building and starting Java backend"

    local mvn_cmd="mvn"
    if [ -f "backend-java/mvnw" ]; then
        mvn_cmd="./mvnw"
        chmod +x backend-java/mvnw 2>/dev/null || true
    fi

    log "Compiling and packaging (skip tests)..."
    $mvn_cmd -f backend-java/pom.xml clean package -q -DskipTests
    log "Build complete."

    step "Starting Agent Application (single process on port 9090)"

    local jar="backend-java/agent-application/target/agent-application-1.0.0-SNAPSHOT.jar"
    if [ ! -f "$jar" ]; then
        err "JAR not found: $jar — build may have failed."
        exit 1
    fi

    nohup java -jar "$jar" > /tmp/agent-application.log 2>&1 &
    echo $! > "$PID_DIR/agent-application.pid"

    log "Backend started: http://localhost:9090"
    log "Logs: /tmp/agent-application.log"
}

# ============================================================
# Start Python Agent Runtime
# ============================================================
start_agent() {
    step "Setting up Python Agent Runtime"
    cd "$REPO_ROOT/agent-python"

    if [ ! -d ".venv" ]; then
        log "Creating virtual environment..."
        python3 -m venv .venv
    fi

    log "Installing dependencies..."
    .venv/bin/pip install -q -r requirements.txt

    step "Starting Agent Runtime (port 8000)"
    nohup .venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload \
        > /tmp/agent-python.log 2>&1 &
    echo $! > "$PID_DIR/agent-python.pid"

    cd "$REPO_ROOT"
    log "Agent Runtime: http://localhost:8000"
    log "API docs:      http://localhost:8000/docs"
}

# ============================================================
# Start Frontend (Turborepo)
# ============================================================
start_frontend() {
    step "Setting up Frontend"
    cd "$REPO_ROOT/frontend"

    if [ ! -d "node_modules" ]; then
        log "Installing dependencies (pnpm install)..."
        pnpm install --silent
    fi

    step "Starting Frontend dev servers (Turborepo)"
    nohup pnpm dev > /tmp/frontend.log 2>&1 &
    echo $! > "$PID_DIR/frontend.pid"

    cd "$REPO_ROOT"
    log "Chat UI:       http://localhost:3000"
    log "Admin Console:  http://localhost:3001"
    log "Agent Studio:   http://localhost:3002"
    log "Model Center:   http://localhost:3003"
    log "RAG Studio:     http://localhost:3004"
    log "Trace Console:  http://localhost:3005"
}

# ============================================================
# Run tests
# ============================================================
run_tests() {
    step "Running Python tests"
    cd "$REPO_ROOT/agent-python"
    .venv/bin/python -m pytest tests/ -v --tb=short 2>&1 || warn "Some Python tests failed"
    cd "$REPO_ROOT"

    step "Running Java tests"
    local mvn_cmd="mvn"
    [ -f "backend-java/mvnw" ] && mvn_cmd="./mvnw"
    $mvn_cmd -f backend-java/pom.xml test -q 2>&1 || warn "Some Java tests failed"
    log "Tests complete."
}

# ============================================================
# Stop all application processes
# ============================================================
stop_all() {
    step "Stopping all application processes"

    for pidfile in "$PID_DIR"/*.pid; do
        [ -f "$pidfile" ] || continue
        local svc=$(basename "$pidfile" .pid)
        local pid=$(cat "$pidfile")
        if kill "$pid" 2>/dev/null; then
            log "Stopped $svc (pid $pid)"
        fi
        rm -f "$pidfile"
    done

    log "All application processes stopped."
}

# ============================================================
# Show status
# ============================================================
show_status() {
    echo ""
    echo -e "${BLUE}  Application Processes${NC}"
    echo "  ─────────────────────"

    local services=(
        "agent-application"
        "agent-python"
        "frontend"
    )

    local running=0
    for svc in "${services[@]}"; do
        if [ -f "$PID_DIR/${svc}.pid" ] && ps -p "$(cat "$PID_DIR/${svc}.pid")" > /dev/null 2>&1; then
            echo -e "  ${GREEN}[UP]${NC}   $svc (pid $(cat "$PID_DIR/${svc}.pid"))"
            ((running++))
        else
            echo -e "  ${RED}[DOWN]${NC} $svc"
        fi
    done

    echo ""
    echo "  Endpoints:"
    echo "    Backend:        http://localhost:9090"
    echo "    Agent Runtime:  http://localhost:8000/docs"
    echo "    Chat UI:        http://localhost:3000"
    echo ""

    if [ "$running" -eq 0 ]; then
        echo -e "  ${YELLOW}No app processes running. Run: bash scripts/start-local.sh${NC}"
    fi
}

# ============================================================
# Print usage
# ============================================================
usage() {
    echo "Usage: bash scripts/start-local.sh [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  (default)   Start full stack (backend + agent + frontend)"
    echo "  backend     Start Java backend only (single agent-application JAR)"
    echo "  agent       Start Python agent runtime only"
    echo "  frontend    Start frontend dev servers only"
    echo "  test        Run all tests"
    echo "  stop        Stop all app processes"
    echo "  status      Show running status"
}

# ============================================================
# Main
# ============================================================
case "${1:-all}" in
    backend)
        start_backend
        ;;
    agent)
        start_agent
        ;;
    frontend)
        start_frontend
        ;;
    stop)
        stop_all
        ;;
    status)
        show_status
        ;;
    test)
        run_tests
        ;;
    -h|--help|help)
        usage
        ;;
    all|*)
        start_backend
        start_agent
        start_frontend
        ;;
esac

echo ""
echo -e "${GREEN}============================================================${NC}"
echo -e "${GREEN}  Done.${NC}"
echo -e "${GREEN}============================================================${NC}"
