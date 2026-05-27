CREATE TABLE IF NOT EXISTS tools (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    display_name  VARCHAR(256),
    description   TEXT,
    tool_type     VARCHAR(16)  NOT NULL,  -- http, mcp, sdk
    schema_json   TEXT,                   -- OpenAPI / JSON Schema
    endpoint_url  VARCHAR(1024),
    method        VARCHAR(8)   DEFAULT 'POST',
    timeout_ms    INTEGER      DEFAULT 30000,
    retry_count   INTEGER      DEFAULT 0,
    retry_backoff VARCHAR(16)  DEFAULT 'fixed',  -- fixed, exponential
    status        VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE TABLE IF NOT EXISTS tool_auth_configs (
    id            VARCHAR(36)  PRIMARY KEY,
    tool_id       VARCHAR(36)  NOT NULL REFERENCES tools(id),
    auth_type     VARCHAR(16)  NOT NULL,  -- api_key, oauth2, basic, none
    credential_json TEXT,                 -- encrypted credentials
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tool_rate_limits (
    id            VARCHAR(36)  PRIMARY KEY,
    tool_id       VARCHAR(36)  NOT NULL REFERENCES tools(id),
    tenant_id     VARCHAR(36)  NOT NULL,
    strategy      VARCHAR(16)  NOT NULL DEFAULT 'token_bucket',  -- token_bucket, sliding_window
    max_requests  INTEGER      NOT NULL DEFAULT 100,
    window_seconds INTEGER     NOT NULL DEFAULT 60,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tool_call_logs (
    id            VARCHAR(36)  PRIMARY KEY,
    tool_id       VARCHAR(36)  NOT NULL REFERENCES tools(id),
    tenant_id     VARCHAR(36)  NOT NULL,
    trace_id      VARCHAR(36),
    input_json    TEXT,
    output_json   TEXT,
    status_code   INTEGER,
    latency_ms    INTEGER,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tool_logs_tool ON tool_call_logs(tool_id);
CREATE INDEX idx_tool_logs_tenant ON tool_call_logs(tenant_id);
