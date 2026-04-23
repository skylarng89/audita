-- ============================================================
-- Tenant schema — tables created once per organisation.
-- Applied via FlywayTenantMigrator on tenant creation and app startup.
-- ============================================================

-- Roles
CREATE TABLE IF NOT EXISTS roles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Permissions (seeded; immutable)
CREATE TABLE IF NOT EXISTS permissions (
    id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code  VARCHAR(100) UNIQUE NOT NULL,
    label VARCHAR(255) NOT NULL
);

-- Role <-> Permission mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Users
CREATE TABLE IF NOT EXISTS users (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(255) NOT NULL,
    role_id         UUID         REFERENCES roles(id),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    invited_by      UUID         REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED'))
);

-- OAuth linked accounts (Google, Microsoft)
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider     VARCHAR(20)  NOT NULL,
    provider_sub VARCHAR(500) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    linked_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_oauth_provider CHECK (provider IN ('GOOGLE', 'MICROSOFT')),
    CONSTRAINT uq_provider_sub UNIQUE (provider, provider_sub)
);

-- Password reset tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- JWT refresh tokens
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE
);

-- User invite tokens
CREATE TABLE IF NOT EXISTS invite_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

-- Groups
CREATE TABLE IF NOT EXISTS groups (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_by  UUID         REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- User <-> Group membership
CREATE TABLE IF NOT EXISTS user_groups (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

-- Org-wide settings (key-value store)
CREATE TABLE IF NOT EXISTS org_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT         NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Custom field definitions (Admin-configured)
CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    label         VARCHAR(255) NOT NULL,
    field_type    VARCHAR(50)  NOT NULL,
    options       JSONB,                    -- for DROPDOWN type
    is_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'DROPDOWN', 'CHECKBOX'))
);

-- Default approvers (org-level; cloned into each new CR)
CREATE TABLE IF NOT EXISTS default_approvers (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_required BOOLEAN     NOT NULL DEFAULT TRUE,
    position    INT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- SLA Policies
CREATE TABLE IF NOT EXISTS sla_policies (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    priority_trigger     VARCHAR(20),        -- LOW|MEDIUM|HIGH|CRITICAL|ALL; NULL = matches all
    deadline_hours       INT          NOT NULL,
    warning_before_hours INT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- SLA escalation contacts
CREATE TABLE IF NOT EXISTS sla_escalation_contacts (
    sla_policy_id UUID NOT NULL REFERENCES sla_policies(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (sla_policy_id, user_id)
);

-- Change Requests
CREATE TABLE IF NOT EXISTS change_requests (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(500) NOT NULL,
    description      TEXT,
    priority         VARCHAR(20)  NOT NULL,
    risk_level       VARCHAR(20)  NOT NULL,
    category         VARCHAR(255),
    status           VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    approval_type    VARCHAR(20)  NOT NULL,
    approval_locked  BOOLEAN      NOT NULL DEFAULT FALSE,
    scheduled_start  TIMESTAMPTZ,
    scheduled_end    TIMESTAMPTZ,
    affected_systems TEXT[],
    sla_deadline     TIMESTAMPTZ,
    sla_breached     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by       UUID         REFERENCES users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cr_status   CHECK (status IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT chk_cr_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_cr_risk     CHECK (risk_level IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_cr_approval CHECK (approval_type IN ('LINEAR','NON_LINEAR'))
);

-- Custom field values per CR
CREATE TABLE IF NOT EXISTS change_request_custom_fields (
    change_request_id UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    field_id          UUID NOT NULL REFERENCES custom_field_definitions(id),
    value             TEXT,
    PRIMARY KEY (change_request_id, field_id)
);

-- Approvers on a CR
CREATE TABLE IF NOT EXISTS cr_approvers (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID        NOT NULL REFERENCES users(id),
    is_required       BOOLEAN     NOT NULL DEFAULT TRUE,
    position          INT         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    rejection_reason  TEXT,
    decided_at        TIMESTAMPTZ,
    is_ad_hoc         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_approver_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);

-- File attachments (on CRs and comments)
CREATE TABLE IF NOT EXISTS attachments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    comment_id        UUID,                  -- FK added below after comments table
    uploader_id       UUID        REFERENCES users(id),
    file_name         VARCHAR(500) NOT NULL,
    mime_type         VARCHAR(100),
    size_bytes        BIGINT,
    storage_path      TEXT        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Comments
CREATE TABLE IF NOT EXISTS comments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    author_id         UUID        REFERENCES users(id),
    body              TEXT        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Back-reference: attachments.comment_id -> comments
ALTER TABLE attachments
    ADD CONSTRAINT fk_attachment_comment
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE SET NULL;

-- @Mentions in comments
CREATE TABLE IF NOT EXISTS comment_mentions (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

-- Per-CR activity stream (immutable)
CREATE TABLE IF NOT EXISTS activity_stream (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    actor_id          UUID        REFERENCES users(id),
    action_type       VARCHAR(100) NOT NULL,
    payload           JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Global audit log (immutable, org-scoped)
CREATE TABLE IF NOT EXISTS audit_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    actor_email VARCHAR(255),              -- denormalised for immutability
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    payload     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- In-app notifications
CREATE TABLE IF NOT EXISTS notifications (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(100) NOT NULL,
    title        VARCHAR(500),
    body         TEXT,
    link         TEXT,
    is_read      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- Indexes for performance (SRS §7.1)
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_cr_status      ON change_requests(status);
CREATE INDEX IF NOT EXISTS idx_cr_created_by  ON change_requests(created_by);
CREATE INDEX IF NOT EXISTS idx_cr_created_at  ON change_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cr_priority    ON change_requests(priority);
CREATE INDEX IF NOT EXISTS idx_cr_sla         ON change_requests(sla_deadline) WHERE sla_breached = FALSE;
CREATE INDEX IF NOT EXISTS idx_cr_approvers   ON cr_approvers(change_request_id);
CREATE INDEX IF NOT EXISTS idx_activity_cr    ON activity_stream(change_request_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_created  ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_recipient ON notifications(recipient_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_status   ON users(status);
