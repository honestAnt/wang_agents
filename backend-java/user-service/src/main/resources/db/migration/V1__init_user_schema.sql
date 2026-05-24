-- ============================================================
-- User Service Schema v1 — tenants, users, departments, roles
-- ============================================================

CREATE TABLE IF NOT EXISTS tenants (
    id          VARCHAR(36)  PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    domain      VARCHAR(256),
    plan        VARCHAR(32)  NOT NULL DEFAULT 'free',
    status      VARCHAR(16)  NOT NULL DEFAULT 'active',
    max_users   INTEGER      DEFAULT 50,
    max_agents  INTEGER      DEFAULT 10,
    quota_limit BIGINT       DEFAULT 1000000,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id          VARCHAR(36)  PRIMARY KEY,
    tenant_id   VARCHAR(36)  NOT NULL REFERENCES tenants(id),
    parent_id   VARCHAR(36)  REFERENCES departments(id),
    name        VARCHAR(128) NOT NULL,
    path        VARCHAR(512),
    sort_order  INTEGER      DEFAULT 0,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_departments_tenant ON departments(tenant_id);
CREATE INDEX idx_departments_parent ON departments(parent_id);

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  PRIMARY KEY,
    tenant_id   VARCHAR(36)  NOT NULL REFERENCES tenants(id),
    department_id VARCHAR(36) REFERENCES departments(id),
    username    VARCHAR(128) NOT NULL,
    email       VARCHAR(256),
    display_name VARCHAR(128),
    phone       VARCHAR(32),
    avatar_url  VARCHAR(512),
    status      VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, username)
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_users_department ON users(department_id);

CREATE TABLE IF NOT EXISTS user_roles (
    id          VARCHAR(36)  PRIMARY KEY,
    user_id     VARCHAR(36)  NOT NULL REFERENCES users(id),
    tenant_id   VARCHAR(36)  NOT NULL REFERENCES tenants(id),
    role        VARCHAR(64)  NOT NULL,
    granted_by  VARCHAR(36),
    granted_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, tenant_id, role)
);

CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
