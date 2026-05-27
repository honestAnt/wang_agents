CREATE TABLE IF NOT EXISTS security_audit_logs (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36),
    event_type      VARCHAR(32)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL DEFAULT 'info',
    description     TEXT,
    source_ip       VARCHAR(64),
    user_agent      VARCHAR(512),
    resource        VARCHAR(256),
    action          VARCHAR(16),
    details         TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_tenant ON security_audit_logs(tenant_id);
CREATE INDEX idx_audit_event_type ON security_audit_logs(event_type);
CREATE INDEX idx_audit_created ON security_audit_logs(created_at);
CREATE INDEX idx_audit_user ON security_audit_logs(user_id);

CREATE TABLE IF NOT EXISTS security_alerts (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    alert_type      VARCHAR(32)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL DEFAULT 'medium',
    message         TEXT         NOT NULL,
    source_user_id  VARCHAR(36),
    raw_prompt      TEXT,
    matched_patterns TEXT,
    resolved        BOOLEAN      DEFAULT FALSE,
    resolved_by     VARCHAR(36),
    resolved_at     TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_alerts_tenant ON security_alerts(tenant_id);
CREATE INDEX idx_alerts_resolved ON security_alerts(resolved);
CREATE INDEX idx_alerts_severity ON security_alerts(severity);

CREATE TABLE IF NOT EXISTS ip_block_rules (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    ip_pattern      VARCHAR(64)  NOT NULL,
    block_type      VARCHAR(32)  DEFAULT 'temporary',
    reason          TEXT,
    expires_at      TIMESTAMP,
    created_by      VARCHAR(36),
    active          BOOLEAN      DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ip_block_tenant ON ip_block_rules(tenant_id);
CREATE INDEX idx_ip_block_active ON ip_block_rules(active);
