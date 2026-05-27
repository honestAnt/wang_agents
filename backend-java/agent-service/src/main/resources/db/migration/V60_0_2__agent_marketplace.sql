CREATE TABLE IF NOT EXISTS agent_marketplace (
    id              VARCHAR(36)  PRIMARY KEY,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    publisher_id    VARCHAR(36)  NOT NULL,
    tenant_id       VARCHAR(36)  NOT NULL,
    category        VARCHAR(64)  NOT NULL DEFAULT 'general',
    tags            VARCHAR(512) DEFAULT '',  -- comma-separated
    icon_url        VARCHAR(512),
    banner_url      VARCHAR(512),
    readme          TEXT,
    version         INTEGER      NOT NULL DEFAULT 1,
    install_count   INTEGER      DEFAULT 0,
    rating_avg      DECIMAL(2,1) DEFAULT 0.0,
    rating_count    INTEGER      DEFAULT 0,
    status          VARCHAR(16)  NOT NULL DEFAULT 'published',  -- published, unpublished, suspended
    published_at    TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_marketplace_category ON agent_marketplace(category);
CREATE INDEX idx_marketplace_tenant ON agent_marketplace(tenant_id);
CREATE INDEX idx_marketplace_status ON agent_marketplace(status);

CREATE TABLE IF NOT EXISTS agent_reviews (
    id              VARCHAR(36)  PRIMARY KEY,
    marketplace_id  VARCHAR(36)  NOT NULL REFERENCES agent_marketplace(id),
    user_id         VARCHAR(36)  NOT NULL,
    rating          INTEGER      NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(marketplace_id, user_id)
);

CREATE INDEX idx_reviews_marketplace ON agent_reviews(marketplace_id);

CREATE TABLE IF NOT EXISTS agent_installs (
    id              VARCHAR(36)  PRIMARY KEY,
    marketplace_id  VARCHAR(36)  NOT NULL REFERENCES agent_marketplace(id),
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    source_agent_id VARCHAR(36)  NOT NULL,
    cloned_agent_id VARCHAR(36),
    status          VARCHAR(16)  NOT NULL DEFAULT 'installed',
    installed_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_installs_tenant ON agent_installs(tenant_id);
CREATE INDEX idx_installs_marketplace ON agent_installs(marketplace_id);
