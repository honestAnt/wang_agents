CREATE TABLE IF NOT EXISTS trace_sessions (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),
    agent_id      VARCHAR(36),
    model         VARCHAR(128),
    message_count INTEGER      DEFAULT 0,
    total_latency_ms DOUBLE,
    total_cost_usd   DECIMAL(12,6) DEFAULT 0,
    status        VARCHAR(16)  DEFAULT 'ok',
    tags          TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trace_sessions_tenant ON trace_sessions(tenant_id);
CREATE INDEX idx_trace_sessions_user ON trace_sessions(user_id);
CREATE INDEX idx_trace_sessions_time ON trace_sessions(created_at);

CREATE TABLE IF NOT EXISTS trace_spans (
    id            VARCHAR(36)  PRIMARY KEY,
    trace_id      VARCHAR(36)  NOT NULL,
    span_id       VARCHAR(36)  NOT NULL,
    parent_id     VARCHAR(36),
    session_id    VARCHAR(36)  REFERENCES trace_sessions(id),
    tenant_id     VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),
    agent_id      VARCHAR(36),
    type          VARCHAR(16)  NOT NULL,  -- llm, tool, rag, memory, agent, skill, api, workflow
    name          VARCHAR(256),
    input_json    TEXT,
    output_json   TEXT,
    latency_ms    DOUBLE,
    status        VARCHAR(16)  DEFAULT 'ok',
    error_message TEXT,
    metadata_json TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_spans_trace ON trace_spans(trace_id);
CREATE INDEX idx_spans_session ON trace_spans(session_id);
CREATE INDEX idx_spans_tenant ON trace_spans(tenant_id);
CREATE INDEX idx_spans_type ON trace_spans(type);
