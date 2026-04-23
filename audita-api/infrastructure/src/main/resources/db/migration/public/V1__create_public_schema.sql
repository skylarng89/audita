-- ============================================================
-- Public schema — platform-level tables shared across all tenants
-- ============================================================

-- Tenants (organizations)
CREATE TABLE IF NOT EXISTS tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT  chk_tenant_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

-- Allowed email domains per tenant (domain whitelisting)
CREATE TABLE IF NOT EXISTS tenant_allowed_domains (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    domain      VARCHAR(255) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT  uq_tenant_domain UNIQUE (tenant_id, domain)
);

-- SSO provider configuration per tenant
CREATE TABLE IF NOT EXISTS tenant_sso_configs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    provider      VARCHAR(20)  NOT NULL,
    client_id     VARCHAR(500) NOT NULL,
    client_secret TEXT         NOT NULL,   -- AES-256 encrypted
    ms_tenant_id  VARCHAR(255),            -- Azure AD tenant ID (Microsoft only)
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT    chk_sso_provider CHECK (provider IN ('GOOGLE', 'MICROSOFT')),
    CONSTRAINT    uq_tenant_provider UNIQUE (tenant_id, provider)
);

-- Super admins (platform-level, not scoped to a tenant)
CREATE TABLE IF NOT EXISTS super_admins (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Platform-level audit log (tenant provisioning, domain changes, SSO changes)
CREATE TABLE IF NOT EXISTS platform_audit_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID,                      -- NULL if system action
    actor_email VARCHAR(255),              -- denormalised for immutability
    action_type VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   UUID,
    payload     JSONB,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index: tenant lookups by slug (login, request routing)
CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_platform_audit_created ON platform_audit_log(created_at DESC);
