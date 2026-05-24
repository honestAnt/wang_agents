CREATE TABLE IF NOT EXISTS billing_records (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36),
    agent_id        VARCHAR(36),
    model           VARCHAR(128) NOT NULL,
    trace_id        VARCHAR(36),
    prompt_tokens   INTEGER      DEFAULT 0,
    completion_tokens INTEGER    DEFAULT 0,
    cost_usd        DECIMAL(12,6) DEFAULT 0,
    cost_currency   VARCHAR(8)   DEFAULT 'USD',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_billing_tenant ON billing_records(tenant_id);
CREATE INDEX idx_billing_time ON billing_records(created_at);

CREATE TABLE IF NOT EXISTS cost_summaries (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    period          VARCHAR(16)  NOT NULL,  -- daily, weekly, monthly
    period_start    DATE         NOT NULL,
    total_cost_usd  DECIMAL(12,4) DEFAULT 0,
    total_tokens    BIGINT       DEFAULT 0,
    request_count   INTEGER      DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, period, period_start)
);

CREATE TABLE IF NOT EXISTS invoices (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    invoice_number  VARCHAR(64)  NOT NULL UNIQUE,
    period_start    DATE         NOT NULL,
    period_end      DATE         NOT NULL,
    total_cost_usd  DECIMAL(12,4) NOT NULL,
    status          VARCHAR(16)  DEFAULT 'draft',  -- draft, sent, paid, overdue
    pdf_url         VARCHAR(1024),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
