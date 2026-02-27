-- H2 (MODE=PostgreSQL) / PostgreSQL compatible schema
-- Uses TEXT and BYTEA for cross-DB compatibility
CREATE SCHEMA IF NOT EXISTS gjira;

CREATE TABLE gjira.tenants (
    id              VARCHAR(36) PRIMARY KEY,
    org_code        VARCHAR(3) NOT NULL UNIQUE,
    org_name        VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    config          TEXT
);

CREATE INDEX idx_tenants_org_code ON gjira.tenants(org_code);

CREATE TABLE gjira.dynamic_vectors (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,
    source_id       VARCHAR(255) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       BYTEA,
    ingested_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    event_ts        TIMESTAMPTZ NOT NULL,
    metadata        TEXT
);

CREATE INDEX idx_dynamic_tenant_ts ON gjira.dynamic_vectors(tenant_id, event_ts DESC);
CREATE INDEX idx_dynamic_source ON gjira.dynamic_vectors(tenant_id, source_type, source_id);

CREATE TABLE gjira.static_library_vectors (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,
    source_id       VARCHAR(255) NOT NULL,
    chunk_index     INTEGER NOT NULL DEFAULT 0,
    content_hash    VARCHAR(64) NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       BYTEA,
    file_path       VARCHAR(512) NOT NULL,
    ingested_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata        TEXT
);

CREATE INDEX idx_static_tenant_source ON gjira.static_library_vectors(tenant_id, source_type, source_id);

CREATE TABLE gjira.static_library_manifest (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,
    source_id       VARCHAR(255) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    last_modified   TIMESTAMPTZ,
    ingested_at     TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_manifest_tenant_source ON gjira.static_library_manifest(tenant_id, source_type, source_id);

CREATE TABLE gjira.encrypted_archives (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL REFERENCES gjira.tenants(id),
    stream_type     VARCHAR(16) NOT NULL,
    r2_bucket       VARCHAR(128) NOT NULL,
    r2_key          VARCHAR(512) NOT NULL,
    encryption_key_id VARCHAR(128),
    size_bytes      BIGINT,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ
);

CREATE INDEX idx_archives_tenant ON gjira.encrypted_archives(tenant_id, stream_type);

CREATE TABLE gjira.user_skill_logs (
    id              VARCHAR(36) PRIMARY KEY,
    tenant_id       VARCHAR(36) NOT NULL REFERENCES gjira.tenants(id),
    user_id         VARCHAR(255) NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    confidence      DECIMAL(5,4),
    inference_type  VARCHAR(16),
    latency_ms      INTEGER,
    created_at      TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    metadata        TEXT
);

CREATE INDEX idx_skill_tenant_user_ts ON gjira.user_skill_logs(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_skill_event_type ON gjira.user_skill_logs(tenant_id, event_type);
