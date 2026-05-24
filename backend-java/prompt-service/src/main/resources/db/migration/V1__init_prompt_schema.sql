CREATE TABLE IF NOT EXISTS prompts (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    description   TEXT,
    category      VARCHAR(64)  DEFAULT 'general',
    tags          TEXT,
    current_version INTEGER   DEFAULT 1,
    status        VARCHAR(16)  NOT NULL DEFAULT 'draft',
    created_by    VARCHAR(36),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE TABLE IF NOT EXISTS prompt_versions (
    id            VARCHAR(36)  PRIMARY KEY,
    prompt_id     VARCHAR(36)  NOT NULL REFERENCES prompts(id),
    version       INTEGER      NOT NULL,
    content       TEXT         NOT NULL,
    variables_json TEXT,
    token_estimate INTEGER,
    change_log    TEXT,
    created_by    VARCHAR(36),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(prompt_id, version)
);
CREATE INDEX idx_prompt_versions_prompt ON prompt_versions(prompt_id);

CREATE TABLE IF NOT EXISTS prompt_ab_tests (
    id            VARCHAR(36)  PRIMARY KEY,
    tenant_id     VARCHAR(36)  NOT NULL,
    name          VARCHAR(128) NOT NULL,
    prompt_a_id   VARCHAR(36)  NOT NULL REFERENCES prompts(id),
    prompt_b_id   VARCHAR(36)  NOT NULL REFERENCES prompts(id),
    traffic_split DECIMAL(3,2) DEFAULT 0.5,
    metric        VARCHAR(32)  DEFAULT 'satisfaction',
    status        VARCHAR(16)  DEFAULT 'running',
    started_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at      TIMESTAMP
);
