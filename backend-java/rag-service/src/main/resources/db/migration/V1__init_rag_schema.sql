-- ============================================================
-- RAG Service Schema v1 — knowledge bases, documents, chunks
-- ============================================================

CREATE TABLE IF NOT EXISTS knowledge_bases (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    name            VARCHAR(256) NOT NULL,
    description     TEXT,
    embedding_model VARCHAR(128) DEFAULT 'text-embedding-3-small',
    embedding_dim   INTEGER      DEFAULT 1536,
    chunk_size      INTEGER      DEFAULT 512,
    chunk_overlap   INTEGER      DEFAULT 50,
    chunk_strategy  VARCHAR(32)  DEFAULT 'recursive',  -- fixed, semantic, recursive, markdown
    index_type      VARCHAR(32)  DEFAULT 'hybrid',      -- bm25, vector, hybrid
    status          VARCHAR(16)  NOT NULL DEFAULT 'active',
    doc_count       INTEGER      DEFAULT 0,
    chunk_count     BIGINT       DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kb_tenant ON knowledge_bases(tenant_id);

CREATE TABLE IF NOT EXISTS documents (
    id              VARCHAR(36)  PRIMARY KEY,
    kb_id           VARCHAR(36)  NOT NULL REFERENCES knowledge_bases(id),
    tenant_id       VARCHAR(36)  NOT NULL,
    title           VARCHAR(512) NOT NULL,
    file_type       VARCHAR(16)  NOT NULL,  -- pdf, docx, pptx, xlsx, md, html, txt
    file_url        VARCHAR(1024),
    file_size       BIGINT,
    status          VARCHAR(16)  NOT NULL DEFAULT 'uploading',  -- uploading, parsing, chunking, embedding, ready, error
    metadata_json   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_docs_kb ON documents(kb_id);
CREATE INDEX idx_docs_tenant ON documents(tenant_id);

CREATE TABLE IF NOT EXISTS chunks (
    id              VARCHAR(36)  PRIMARY KEY,
    document_id     VARCHAR(36)  NOT NULL REFERENCES documents(id),
    kb_id           VARCHAR(36)  NOT NULL REFERENCES knowledge_bases(id),
    tenant_id       VARCHAR(36)  NOT NULL,
    chunk_index     INTEGER      NOT NULL,
    content         TEXT         NOT NULL,
    token_count     INTEGER,
    embedding_id    VARCHAR(128),
    metadata_json   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chunks_doc ON chunks(document_id);
CREATE INDEX idx_chunks_kb ON chunks(kb_id);
CREATE INDEX idx_chunks_tenant ON chunks(tenant_id);

CREATE TABLE IF NOT EXISTS embedding_configs (
    id              VARCHAR(36)  PRIMARY KEY,
    tenant_id       VARCHAR(36)  NOT NULL,
    kb_id           VARCHAR(36)  NOT NULL REFERENCES knowledge_bases(id),
    model           VARCHAR(128) NOT NULL,
    dimensions      INTEGER      DEFAULT 1536,
    batch_size      INTEGER      DEFAULT 20,
    provider        VARCHAR(64)  DEFAULT 'openai',
    api_key_ref     VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_embed_config_kb ON embedding_configs(kb_id);
