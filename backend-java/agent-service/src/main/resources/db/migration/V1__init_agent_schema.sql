CREATE TABLE IF NOT EXISTS agents (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    avatar_url      VARCHAR(512),
    system_prompt   TEXT,
    model_id        VARCHAR(36),
    temperature     DECIMAL(3,2) DEFAULT 0.7,
    max_tokens      INTEGER      DEFAULT 4096,
    memory_strategy VARCHAR(16)  DEFAULT 'short_term',  -- short_term, long_term, both, none
    visibility      VARCHAR(16)  DEFAULT 'tenant',       -- private, tenant, public
    status          VARCHAR(16)  NOT NULL DEFAULT 'draft',
    version         INTEGER      DEFAULT 1,
    published_at    TIMESTAMP,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_agents_tenant ON agents(tenant_id);

CREATE TABLE IF NOT EXISTS agent_configs (
    id              VARCHAR(36)  PRIMARY KEY,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    version         INTEGER      NOT NULL,
    config_json     TEXT         NOT NULL,  -- full snapshot: prompt, tools, skills, model, memory, kb, permissions
    change_log      TEXT,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, version)
);

CREATE TABLE IF NOT EXISTS agent_tool_bindings (
    id              VARCHAR(36)  PRIMARY KEY,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    tool_id         VARCHAR(36)  NOT NULL,
    priority        INTEGER      DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, tool_id)
);

CREATE TABLE IF NOT EXISTS agent_skill_bindings (
    id              VARCHAR(36)  PRIMARY KEY,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    skill_id        VARCHAR(36)  NOT NULL,
    priority        INTEGER      DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, skill_id)
);

CREATE TABLE IF NOT EXISTS agent_permissions (
    id              VARCHAR(36)  PRIMARY KEY,
    agent_id        VARCHAR(36)  NOT NULL REFERENCES agents(id),
    principal_type  VARCHAR(16)  NOT NULL,  -- role, user, department
    principal_id    VARCHAR(36)  NOT NULL,
    permission      VARCHAR(16)  NOT NULL DEFAULT 'use',  -- use, edit, admin
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_perm_agent ON agent_permissions(agent_id);
