-- ============================================================
-- Tenant baseline schema (unified V1)
--
-- This is the complete schema for a new tenant, consolidated from
-- the original V1-V10 incremental migrations. For fresh deployments
-- (public launch), this single script provisions the entire schema.
--
-- Future schema changes follow as V2, V3, etc.
-- ============================================================

-- ============================================================
-- Core tables
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_system   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS permissions (
    id    UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code  VARCHAR(100) UNIQUE NOT NULL,
    label VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

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

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent_hash VARCHAR(255),
    ip_hash         VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS invite_tokens (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS groups (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_by  UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_group_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_groups (
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE TABLE IF NOT EXISTS group_members (
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id  UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (group_id, user_id)
);

CREATE TABLE IF NOT EXISTS org_settings (
    key        VARCHAR(100) PRIMARY KEY,
    value      TEXT         NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS custom_field_definitions (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    label         VARCHAR(255) NOT NULL,
    field_type    VARCHAR(50)  NOT NULL,
    options       JSONB,
    is_required   BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INT          NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_field_type CHECK (field_type IN ('TEXT', 'NUMBER', 'DATE', 'DROPDOWN', 'CHECKBOX'))
);

CREATE TABLE IF NOT EXISTS default_approvers (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_required BOOLEAN     NOT NULL DEFAULT TRUE,
    position    INT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sla_policies (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name                 VARCHAR(255) NOT NULL,
    priority_trigger     VARCHAR(20),
    deadline_hours       INT          NOT NULL,
    warning_before_hours INT,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS sla_escalation_contacts (
    sla_policy_id UUID NOT NULL REFERENCES sla_policies(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (sla_policy_id, user_id)
);

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

CREATE TABLE IF NOT EXISTS change_request_custom_fields (
    change_request_id UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    field_id          UUID NOT NULL REFERENCES custom_field_definitions(id),
    value             TEXT,
    PRIMARY KEY (change_request_id, field_id)
);

CREATE TABLE IF NOT EXISTS cr_approvers (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID        NOT NULL REFERENCES users(id),
    position          INT         NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason  TEXT,
    decided_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cr_approver UNIQUE (change_request_id, user_id),
    CONSTRAINT chk_approver_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE TABLE IF NOT EXISTS cr_watchers (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID         NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_sample         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cr_watcher UNIQUE (change_request_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_cr_watchers_cr ON cr_watchers(change_request_id);
CREATE INDEX IF NOT EXISTS idx_cr_watchers_user ON cr_watchers(user_id);

CREATE TABLE IF NOT EXISTS attachments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    comment_id        UUID,
    uploader_id       UUID        REFERENCES users(id),
    file_name         VARCHAR(500) NOT NULL,
    mime_type         VARCHAR(100),
    size_bytes        BIGINT,
    storage_path      TEXT        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS comments (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    author_id         UUID        REFERENCES users(id),
    body              TEXT        NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

ALTER TABLE attachments
    ADD CONSTRAINT fk_attachment_comment
    FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE SET NULL;

CREATE TABLE IF NOT EXISTS comment_mentions (
    comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS activity_stream (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    actor_id          UUID        REFERENCES users(id),
    action_type       VARCHAR(100) NOT NULL,
    payload           JSONB,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,
    actor_email VARCHAR(255),
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    payload     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

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

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    operation VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    resource_id UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_user_operation_key UNIQUE (user_id, operation, idempotency_key)
);

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

-- ============================================================
-- Seed data: permissions, built-in roles, role-permission mappings
-- ============================================================

INSERT INTO permissions (code, label) VALUES
    ('cr.create',              'Create Change Requests'),
    ('cr.view',                'View Change Requests'),
    ('cr.view.all',            'View All Change Requests (global)'),
    ('cr.edit',                'Edit Change Requests'),
    ('cr.cancel',              'Cancel Change Requests'),
    ('cr.submit',              'Submit Change Requests for Approval'),
    ('cr.approve',             'Approve / Reject Change Requests'),
    ('cr.manage_participants', 'Add/Remove Approvers, Watchers, Assignees'),
    ('uat.signoff',            'UAT Sign-Off (requester + approver)'),
    ('deployment.execute',     'Mark Deployment Completed'),
    ('users.view',             'View Users'),
    ('users.manage',           'Invite, Edit and Deactivate Users'),
    ('roles.view',             'View Roles'),
    ('roles.manage',           'Create and Manage Custom Roles'),
    ('groups.view',            'View Groups'),
    ('groups.manage',          'Create and Manage Groups'),
    ('settings.view',          'View Organisation Settings'),
    ('settings.manage',        'Manage Organisation Settings'),
    ('sla.view',               'View SLA Policies'),
    ('sla.manage',             'Create and Manage SLA Policies'),
    ('audit.view',             'View Audit Trail'),
    ('audit.export',           'Export Audit Trail to CSV')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles (id, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Admin',     'Full organisation management',              TRUE),
    ('00000000-0000-0000-0000-000000000002', 'Requester', 'Can create and manage change requests',     TRUE),
    ('00000000-0000-0000-0000-000000000004', 'Auditor',   'Read-only access across the organisation',  TRUE)
ON CONFLICT (id) DO NOTHING;

-- Admin: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Requester: create, view, edit, cancel, submit, approve, manage_participants, uat.signoff, deployment.execute, users.view, groups.view, settings.view, sla.view
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions
WHERE code IN ('cr.create', 'cr.view', 'cr.edit', 'cr.cancel', 'cr.submit', 'cr.approve',
               'cr.manage_participants', 'uat.signoff', 'deployment.execute',
               'users.view', 'groups.view', 'settings.view', 'sla.view')
ON CONFLICT DO NOTHING;

-- Auditor: view all, users.view, groups.view, roles.view, settings.view, sla.view, audit.view, audit.export
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permissions
WHERE code IN ('cr.view', 'cr.view.all', 'users.view', 'groups.view', 'roles.view',
               'settings.view', 'sla.view', 'audit.view', 'audit.export')
ON CONFLICT DO NOTHING;

-- ============================================================
-- Indexes
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

-- V5 additional audit indexes
CREATE INDEX IF NOT EXISTS idx_audit_log_created     ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id    ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity      ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_custom_field_defs_order ON custom_field_definitions(display_order);

-- V6 user_roles index
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- V7 idempotency_keys index
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_keys(expires_at);

-- V8 audit_export_requests indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_audit_export_requests_token ON audit_export_requests(download_token)
    WHERE download_token IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_audit_export_requests_created_at ON audit_export_requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_export_requests_token_expiry ON audit_export_requests(token_expires_at);

-- V9/V10 refresh_tokens indexes
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- V3 group_members index
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
