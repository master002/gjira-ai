-- GJIRA.AI SHADOW DB SCHEMA
-- Multi-tenant, dual-stream vector + metadata storage
-- Target: PostgreSQL 15+ with pgvector / TimescaleDB optional for time-series

CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- TENANT ISOLATION
-- =============================================================================
CREATE SCHEMA IF NOT EXISTS gjira;

CREATE TABLE gjira.tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_code        VARCHAR(3) NOT NULL UNIQUE,
    org_name        VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now(),
    config          JSONB DEFAULT '{}'
);

CREATE INDEX idx_tenants_org_code ON gjira.tenants(org_code);

-- =============================================================================
-- STREAM A: DYNAMIC VECTORS (6-month rolling context)
-- =============================================================================
CREATE TABLE gjira.dynamic_vectors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,  -- slack, teams, zoom
    source_id       VARCHAR(255) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       VECTOR(1536),          -- Ada-002 dimension; adjust for model
    ingested_at     TIMESTAMPTZ DEFAULT now(),
    event_ts        TIMESTAMPTZ NOT NULL,
    metadata        JSONB DEFAULT '{}'
);

CREATE INDEX idx_dynamic_tenant_ts ON gjira.dynamic_vectors(tenant_id, event_ts DESC);
CREATE INDEX idx_dynamic_source ON gjira.dynamic_vectors(tenant_id, source_type, source_id);
CREATE INDEX idx_dynamic_embedding ON gjira.dynamic_vectors USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- =============================================================================
-- STREAM B: STATIC LIBRARY VECTORS
-- =============================================================================
CREATE TABLE gjira.static_library_vectors (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,  -- confluence, sharepoint, upload
    source_id       VARCHAR(255) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    chunk_text      TEXT NOT NULL,
    embedding       VECTOR(1536),
    file_path      VARCHAR(512) NOT NULL,  -- e.g. 2025-02-27_ACM_STATIC.docx
    ingested_at     TIMESTAMPTZ DEFAULT now(),
    metadata        JSONB DEFAULT '{}'
);

CREATE UNIQUE INDEX idx_static_tenant_source ON gjira.static_library_vectors(tenant_id, source_type, source_id);
CREATE INDEX idx_static_embedding ON gjira.static_library_vectors USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- =============================================================================
-- STATIC LIBRARY MANIFEST (delta detection, idempotency)
-- =============================================================================
CREATE TABLE gjira.static_library_manifest (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES gjira.tenants(id),
    source_type     VARCHAR(32) NOT NULL,
    source_id       VARCHAR(255) NOT NULL,
    content_hash    VARCHAR(64) NOT NULL,
    file_path       VARCHAR(512) NOT NULL,
    last_modified   TIMESTAMPTZ,
    ingested_at     TIMESTAMPTZ DEFAULT now()
);

CREATE UNIQUE INDEX idx_manifest_tenant_source ON gjira.static_library_manifest(tenant_id, source_type, source_id);

-- =============================================================================
-- ENCRYPTED ARCHIVES (R2 pointer + metadata)
-- =============================================================================
CREATE TABLE gjira.encrypted_archives (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES gjira.tenants(id),
    stream_type     VARCHAR(16) NOT NULL,  -- DYNAMIC | STATIC
    r2_bucket       VARCHAR(128) NOT NULL,
    r2_key          VARCHAR(512) NOT NULL,
    encryption_key_id VARCHAR(128),        -- KMS/key reference
    size_bytes      BIGINT,
    created_at      TIMESTAMPTZ DEFAULT now(),
    expires_at      TIMESTAMPTZ
);

CREATE INDEX idx_archives_tenant ON gjira.encrypted_archives(tenant_id, stream_type);

-- =============================================================================
-- USER SKILL LOGS (Governance & Skill-Evolution)
-- =============================================================================
CREATE TABLE gjira.user_skill_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL REFERENCES gjira.tenants(id),
    user_id         VARCHAR(255) NOT NULL,
    event_type      VARCHAR(64) NOT NULL,  -- query, resolution, hitl_escalation
    confidence      DECIMAL(5,4),          -- 0.0000 - 1.0000
    inference_type  VARCHAR(16),           -- edge | server
    latency_ms      INTEGER,
    created_at      TIMESTAMPTZ DEFAULT now(),
    metadata        JSONB DEFAULT '{}'
);

CREATE INDEX idx_skill_tenant_user_ts ON gjira.user_skill_logs(tenant_id, user_id, created_at DESC);
CREATE INDEX idx_skill_event_type ON gjira.user_skill_logs(tenant_id, event_type);

-- =============================================================================
-- ROW LEVEL SECURITY (Multi-tenant isolation)
-- =============================================================================
ALTER TABLE gjira.dynamic_vectors ENABLE ROW LEVEL SECURITY;
ALTER TABLE gjira.static_library_vectors ENABLE ROW LEVEL SECURITY;
ALTER TABLE gjira.static_library_manifest ENABLE ROW LEVEL SECURITY;
ALTER TABLE gjira.encrypted_archives ENABLE ROW LEVEL SECURITY;
ALTER TABLE gjira.user_skill_logs ENABLE ROW LEVEL SECURITY;

-- Policy example (application uses tenant_id from JWT/context)
CREATE POLICY tenant_isolation ON gjira.dynamic_vectors
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
CREATE POLICY tenant_isolation ON gjira.static_library_vectors
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
CREATE POLICY tenant_isolation ON gjira.static_library_manifest
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
CREATE POLICY tenant_isolation ON gjira.encrypted_archives
    USING (tenant_id = current_setting('app.tenant_id')::uuid);
CREATE POLICY tenant_isolation ON gjira.user_skill_logs
    USING (tenant_id = current_setting('app.tenant_id')::uuid);

-- RLS usage: Set app.tenant_id at the start of each request/session (from JWT or auth context).
-- Example (PostgreSQL session):
--   SET app.tenant_id = '550e8400-e29b-41d4-a716-446655440000';
-- Example (Spring JPA): Use a Hibernate Filter or execute "SET app.tenant_id = ?" before each connection checkout.
