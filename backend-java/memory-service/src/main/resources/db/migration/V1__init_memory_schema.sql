CREATE TABLE IF NOT EXISTS memory_entries (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    session_id      VARCHAR(36),
    type            VARCHAR(16)  NOT NULL,  -- episodic, semantic, procedural
    content         TEXT         NOT NULL,
    importance      DECIMAL(3,2) DEFAULT 0.5,
    decay_rate      DECIMAL(3,2) DEFAULT 0.01,
    access_count    INTEGER      DEFAULT 0,
    last_accessed   TIMESTAMP,
    metadata_json   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_memory_tenant ON memory_entries(tenant_id);
CREATE INDEX idx_memory_user ON memory_entries(user_id);
CREATE INDEX idx_memory_type ON memory_entries(type);

CREATE TABLE IF NOT EXISTS user_preferences (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    pref_key        VARCHAR(128) NOT NULL,
    pref_value      TEXT,
    confidence      DECIMAL(3,2) DEFAULT 0.5,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id, pref_key)
);

CREATE TABLE IF NOT EXISTS user_behaviors (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    action          VARCHAR(64)  NOT NULL,
    context_json    TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_behaviors_user ON user_behaviors(user_id);
