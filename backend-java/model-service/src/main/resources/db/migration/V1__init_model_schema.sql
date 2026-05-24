-- ============================================================
-- Model Service Schema v1 — models, providers, keys, quotas
-- ============================================================

CREATE TABLE IF NOT EXISTS model_providers (
    id           VARCHAR(36)  PRIMARY KEY,
    tenant_id    VARCHAR(36)  NOT NULL,
    name         VARCHAR(64)  NOT NULL,  -- openai, anthropic, google, deepseek, qwen, vllm, ollama
    display_name VARCHAR(128),
    base_url     VARCHAR(512),
    status       VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE TABLE IF NOT EXISTS models (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    provider_id   VARCHAR(36)  NOT NULL REFERENCES model_providers(id),
    name          VARCHAR(128) NOT NULL,  -- gpt-4.1, claude-sonnet-4-6, deepseek-v3
    display_name  VARCHAR(256),
    type          VARCHAR(32)  NOT NULL DEFAULT 'chat',  -- chat, embedding, image, audio
    max_tokens    INTEGER      DEFAULT 4096,
    input_price   DECIMAL(10,6) DEFAULT 0,   -- USD per 1K input tokens
    output_price  DECIMAL(10,6) DEFAULT 0,   -- USD per 1K output tokens
    capabilities  TEXT,                        -- JSON array: ["function_calling", "vision", "streaming"]
    status        VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, provider_id, name)
);

CREATE TABLE IF NOT EXISTS api_keys (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    provider_id   VARCHAR(36)  NOT NULL REFERENCES model_providers(id),
    key_name      VARCHAR(128) NOT NULL,
    encrypted_key VARCHAR(512) NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'active',
    last_used_at  TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);

CREATE TABLE IF NOT EXISTS quotas (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),
    agent_id      VARCHAR(36),
    model_id      VARCHAR(36)  REFERENCES models(id),
    max_tokens    BIGINT       NOT NULL DEFAULT 0,  -- 0 = unlimited
    max_requests  BIGINT       NOT NULL DEFAULT 0,
    period        VARCHAR(16)  NOT NULL DEFAULT 'monthly',  -- daily, weekly, monthly
    current_tokens BIGINT      DEFAULT 0,
    current_requests BIGINT    DEFAULT 0,
    reset_at      TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_quotas_tenant ON quotas(tenant_id);

CREATE TABLE IF NOT EXISTS budgets (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    user_id       VARCHAR(36),
    agent_id      VARCHAR(36),
    monthly_limit_usd DECIMAL(12,4) NOT NULL DEFAULT 0,
    current_cost_usd  DECIMAL(12,4) DEFAULT 0,
    alert_threshold   DECIMAL(3,2) DEFAULT 0.8,  -- alert at 80%
    action       VARCHAR(16)  NOT NULL DEFAULT 'alert',  -- alert, block, downgrade
    period       VARCHAR(16)  NOT NULL DEFAULT 'monthly',
    reset_at     TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_budgets_tenant ON budgets(tenant_id);

CREATE TABLE IF NOT EXISTS model_routes (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    condition_json TEXT,                       -- {"task_type": "code", "min_tokens": 0, "max_tokens": 4096}
    primary_model_id VARCHAR(36) NOT NULL REFERENCES models(id),
    fallback_chain_json TEXT,                  -- ["model_id_2", "model_id_3"]
    priority      INTEGER      DEFAULT 0,
    status        VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_model_routes_tenant ON model_routes(tenant_id);
