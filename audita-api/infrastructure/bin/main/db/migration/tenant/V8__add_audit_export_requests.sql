CREATE TABLE IF NOT EXISTS audit_export_requests (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by_user_id   UUID REFERENCES users(id),
    requested_by_email     VARCHAR(255),
    actor_email_filter     VARCHAR(255),
    action_type_filter     VARCHAR(100),
    entity_type_filter     VARCHAR(100),
    date_from              DATE,
    date_to                DATE,
    status                 VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    file_name              VARCHAR(255),
    file_storage_path      TEXT,
    download_token         VARCHAR(128),
    token_expires_at       TIMESTAMPTZ,
    completed_at           TIMESTAMPTZ,
    failure_reason         TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_audit_export_status CHECK (status IN ('PENDING', 'READY', 'FAILED', 'EXPIRED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_export_requests_token ON audit_export_requests(download_token)
    WHERE download_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_export_requests_created_at ON audit_export_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_export_requests_token_expiry ON audit_export_requests(token_expires_at);
