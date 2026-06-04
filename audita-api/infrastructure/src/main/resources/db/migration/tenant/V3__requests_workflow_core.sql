-- V3: Request workflow core — display ID, dual-status, workflow mode.
-- Introduces the conditional workflow scaffolding: approval_status mirrors the
-- legacy status column, completion_status tracks delivery pipeline progress,
-- and workflow_mode selects which lifecycle a request follows.
-- The legacy status column is retained for backward compatibility during the
-- migration window and will be deprecated in a future release.

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS display_id       VARCHAR(32);

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS approval_status  VARCHAR(32)  NOT NULL DEFAULT 'DRAFT';

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS completion_status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS';

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS workflow_mode    VARCHAR(32)  NOT NULL DEFAULT 'APPROVAL_ONLY';

-- Backfill approval_status from the existing status column so that historical
-- rows reflect their true approval state immediately after migration.
UPDATE change_requests
SET approval_status = status
WHERE approval_status = 'DRAFT' AND status <> 'DRAFT';

-- CHECK constraints enforce enum boundaries at the database level.
ALTER TABLE change_requests
    ADD CONSTRAINT chk_approval_status
    CHECK (approval_status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'));

ALTER TABLE change_requests
    ADD CONSTRAINT chk_completion_status
    CHECK (completion_status IN ('IN_PROGRESS', 'COMPLETED'));

ALTER TABLE change_requests
    ADD CONSTRAINT chk_workflow_mode
    CHECK (workflow_mode IN ('APPROVAL_ONLY', 'DELIVERY_PIPELINE'));

-- Index on display_id for lookup by human-readable reference.
CREATE UNIQUE INDEX IF NOT EXISTS idx_change_requests_display_id
    ON change_requests (display_id) WHERE display_id IS NOT NULL;

-- Department master data — lookup table for request routing and reporting.
CREATE TABLE IF NOT EXISTS departments (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    code          VARCHAR(32),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_departments_name UNIQUE (name)
);

-- FK references from change_requests to departments.
ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS request_department_id UUID REFERENCES departments(id);

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS destination_department_id UUID REFERENCES departments(id);

-- Bidirectional request links — canonical pair (a < b) prevents duplicates.
CREATE TABLE IF NOT EXISTS request_links (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id_a  UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    request_id_b  UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    linked_by     UUID        REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_request_links_no_self CHECK (request_id_a <> request_id_b),
    CONSTRAINT uq_request_links_pair    UNIQUE (request_id_a, request_id_b)
);

CREATE INDEX IF NOT EXISTS idx_request_links_a ON request_links (request_id_a);
CREATE INDEX IF NOT EXISTS idx_request_links_b ON request_links (request_id_b);

-- ── UAT stage ─────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS request_uat (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id  UUID         NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    details     TEXT,
    status      VARCHAR(32)  NOT NULL DEFAULT 'IN_PROGRESS',
    read_only   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by  UUID         REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_uat_status CHECK (status IN ('IN_PROGRESS', 'APPROVED', 'REJECTED', 'PROMOTED'))
);

CREATE TABLE IF NOT EXISTS request_uat_approvers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uat_id           UUID        NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
    user_id          UUID        NOT NULL REFERENCES users(id),
    is_required      BOOLEAN     NOT NULL DEFAULT TRUE,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    position         INT         NOT NULL,
    decided_at       TIMESTAMPTZ,
    rejection_reason TEXT,
    CONSTRAINT uq_uat_approver        UNIQUE (uat_id, user_id),
    CONSTRAINT chk_uat_approver_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS request_uat_comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uat_id     UUID        NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
    author_id  UUID        REFERENCES users(id),
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Deployment stage ──────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS request_deployments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id   UUID        NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
    uat_id       UUID        NOT NULL UNIQUE REFERENCES request_uat(id),
    status       VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
    created_by   UUID        REFERENCES users(id),
    promoted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_deployment_status CHECK (status IN ('PENDING_APPROVAL', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS request_deployment_approvers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id    UUID        NOT NULL REFERENCES request_deployments(id) ON DELETE CASCADE,
    user_id          UUID        NOT NULL REFERENCES users(id),
    is_required      BOOLEAN     NOT NULL DEFAULT TRUE,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    position         INT         NOT NULL,
    decided_at       TIMESTAMPTZ,
    rejection_reason TEXT,
    CONSTRAINT uq_deployment_approver        UNIQUE (deployment_id, user_id),
    CONSTRAINT chk_deployment_approver_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE TABLE IF NOT EXISTS request_deployment_comments (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    deployment_id UUID        NOT NULL REFERENCES request_deployments(id) ON DELETE CASCADE,
    author_id     UUID        REFERENCES users(id),
    body          TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
