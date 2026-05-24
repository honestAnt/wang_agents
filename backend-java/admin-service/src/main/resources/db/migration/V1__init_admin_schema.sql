CREATE TABLE IF NOT EXISTS platform_stats (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    stat_date       TIMESTAMP    NOT NULL,
    active_users    BIGINT       DEFAULT 0,
    total_sessions  BIGINT       DEFAULT 0,
    total_requests  BIGINT       DEFAULT 0,
    total_tokens    BIGINT       DEFAULT 0,
    total_cost      DECIMAL(12,4) DEFAULT 0,
    tool_calls      BIGINT       DEFAULT 0,
    tool_failures   BIGINT       DEFAULT 0,
    rag_queries     BIGINT       DEFAULT 0,
    rag_hits        BIGINT       DEFAULT 0,
    avg_latency_ms  DOUBLE PRECISION,
    avg_satisfaction DOUBLE PRECISION,
    top_model       VARCHAR(128),
    top_skill       VARCHAR(128),
    top_tool        VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, stat_date)
);

CREATE INDEX idx_platform_stats_tenant ON platform_stats(tenant_id);
CREATE INDEX idx_platform_stats_date ON platform_stats(stat_date);

CREATE TABLE IF NOT EXISTS operational_alerts (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    alert_type      VARCHAR(32)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL DEFAULT 'info',
    message         TEXT         NOT NULL,
    resolved        BOOLEAN      DEFAULT FALSE,
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_tenant ON operational_alerts(tenant_id);
CREATE INDEX idx_alerts_resolved ON operational_alerts(resolved);
