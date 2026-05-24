CREATE TABLE IF NOT EXISTS skills (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(128) NOT NULL,
    display_name    VARCHAR(256),
    description     TEXT,
    category        VARCHAR(64)  DEFAULT 'general',  -- general, finance, legal, engineering, customer_service
    icon_url        VARCHAR(512),
    status          VARCHAR(16)  NOT NULL DEFAULT 'draft',  -- draft, test, published, archived
    version         INTEGER      DEFAULT 1,
    prompt_template TEXT,
    input_schema    TEXT,
    output_schema   TEXT,
    price           DECIMAL(10,2) DEFAULT 0,
    download_count  INTEGER      DEFAULT 0,
    rating          DECIMAL(3,2) DEFAULT 0,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);
CREATE INDEX idx_skills_tenant ON skills(tenant_id);

CREATE TABLE IF NOT EXISTS skill_versions (
    id              VARCHAR(36)  PRIMARY KEY,
    skill_id        VARCHAR(36)  NOT NULL REFERENCES skills(id),
    version         INTEGER      NOT NULL,
    config_json     TEXT         NOT NULL,
    change_log      TEXT,
    created_by      VARCHAR(36),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(skill_id, version)
);

CREATE TABLE IF NOT EXISTS skill_tool_chains (
    id              VARCHAR(36)  PRIMARY KEY,
    skill_id        VARCHAR(36)  NOT NULL REFERENCES skills(id),
    tool_id         VARCHAR(36)  NOT NULL,
    step_order      INTEGER      NOT NULL DEFAULT 0,
    config_json     TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_stc_skill ON skill_tool_chains(skill_id);

CREATE TABLE IF NOT EXISTS skill_marketplace (
    id              VARCHAR(36)  PRIMARY KEY,
    skill_id        VARCHAR(36)  NOT NULL REFERENCES skills(id),
    publisher_id    VARCHAR(36)  NOT NULL,
    visibility      VARCHAR(16)  DEFAULT 'tenant',  -- tenant, global
    approved        BOOLEAN      DEFAULT FALSE,
    review_count    INTEGER      DEFAULT 0,
    avg_rating      DECIMAL(3,2) DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS skill_permissions (
    id              VARCHAR(36)  PRIMARY KEY,
    skill_id        VARCHAR(36)  NOT NULL REFERENCES skills(id),
    principal_type  VARCHAR(16)  NOT NULL,
    principal_id    VARCHAR(36)  NOT NULL,
    permission      VARCHAR(16)  DEFAULT 'use',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
