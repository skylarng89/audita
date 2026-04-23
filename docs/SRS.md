# Audita — Software Requirements Specification (SRS)

**Version:** 1.0.0
**Status:** Draft
**Last Updated:** 2026-04-19

---

## 1. Introduction

### 1.1 Purpose
This document defines the technical and functional software requirements for Audita — a multi-tenant ITIL/ITSM Change Management platform. It is intended for engineering teams responsible for design, development, and testing.

### 1.2 Scope
Audita consists of:
- A **Nuxt 3** (Vue 3) single-page/server-side rendered frontend
- A **Java 25 + Spring Boot 4** RESTful backend
- A **PostgreSQL** relational database (multi-tenant, schema-per-tenant)
- An **S3-compatible or local disk** file storage layer
- An email delivery system via configurable SMTP

### 1.3 Definitions

| Term | Definition |
|---|---|
| CR | Change Request |
| SLA | Service Level Agreement |
| Tenant | An isolated organization within the Audita platform |
| Approver | User assigned to review and act on a CR |
| Requester | User who raises a CR |
| Linear | Sequential approval flow |
| Non-linear | Concurrent approval flow |

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌──────────────────────────────────────────────────────┐
│                     Client Browser                    │
│              Nuxt 3 (SSR + SPA hydration)            │
└─────────────────────────┬────────────────────────────┘
                          │ HTTPS / REST + SSE
┌─────────────────────────▼────────────────────────────┐
│              Spring Boot 4 API (Java 25)              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │  Auth    │ │  Change  │ │  Users   │ │ Audit   │ │
│  │  Module  │ │  Request │ │  &Groups │ │ Module  │ │
│  └──────────┘ │  Module  │ └──────────┘ └─────────┘ │
│               └──────────┘                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐             │
│  │ Notific. │ │  File    │ │  Admin   │             │
│  │  Module  │ │  Module  │ │  Module  │             │
│  └──────────┘ └──────────┘ └──────────┘             │
└────────┬────────────┬──────────────┬─────────────────┘
         │            │              │
    ┌────▼───┐  ┌─────▼──────┐  ┌───▼─────────┐
    │  PgSQL │  │ File Store │  │ SMTP Server │
    │ (per   │  │ (S3/Local) │  │             │
    │ schema)│  └────────────┘  └─────────────┘
    └────────┘
```

### 2.2 Multi-Tenancy Strategy

- **Schema-per-tenant** isolation in PostgreSQL.
- A shared `public` schema holds the `tenants` and `super_admin` tables.
- Each tenant gets a dedicated schema (e.g., `tenant_abc`) containing all org-scoped tables.
- The `X-Tenant-ID` header (or subdomain resolution) identifies the tenant on each request.
- Spring Boot resolves the active schema via a `TenantContext` thread-local + Hibernate `CurrentTenantIdentifierResolver`.

### 2.3 Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Nuxt 3, Vue 3, Tailwind CSS, Pinia, TipTap (rich editor) |
| Backend | Java 25, Spring Boot 4, Spring Security, Spring Data JPA |
| ORM | Hibernate 7 (Jakarta Persistence) |
| Database | PostgreSQL 16+ |
| Auth | JWT (access token) + Refresh Token (HttpOnly cookie); Spring Security OAuth2 Client (OIDC) for Google & Microsoft |
| File Storage | Spring's `Resource` abstraction — LocalFileSystemStorage / S3Client (AWS SDK v2) |
| Email | Spring Mail + Thymeleaf templates |
| Real-time | Server-Sent Events (SSE) for in-app notifications |
| Build | Gradle (backend), pnpm (frontend) |
| Containerisation | Docker + Docker Compose (dev), Helm chart (production K8s) |
| Migration | Flyway (per-tenant schema migrations) |

---

## 3. Database Schema

### 3.1 Public Schema (Platform-level)

```sql
-- Tenants table
tenants (
  id            UUID PRIMARY KEY,
  name          VARCHAR(255) NOT NULL,
  slug          VARCHAR(100) UNIQUE NOT NULL,   -- used as schema name
  status        VARCHAR(20) DEFAULT 'ACTIVE',   -- ACTIVE | SUSPENDED
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  updated_at    TIMESTAMPTZ DEFAULT NOW()
)

-- Tenant allowed email domains (Super Admin configured)
tenant_allowed_domains (
  id         UUID PRIMARY KEY,
  tenant_id  UUID REFERENCES tenants(id) ON DELETE CASCADE,
  domain     VARCHAR(255) NOT NULL,               -- lowercase, e.g. "acme.com"
  created_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (tenant_id, domain)
)

-- Tenant SSO provider configuration
tenant_sso_configs (
  id            UUID PRIMARY KEY,
  tenant_id     UUID REFERENCES tenants(id) ON DELETE CASCADE,
  provider      VARCHAR(20) NOT NULL,             -- GOOGLE | MICROSOFT
  client_id     VARCHAR(500) NOT NULL,
  client_secret TEXT NOT NULL,                    -- AES-256 encrypted at rest
  ms_tenant_id  VARCHAR(255),                     -- Microsoft Azure AD tenant ID only
  enabled       BOOLEAN DEFAULT TRUE,
  created_at    TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (tenant_id, provider)
)

-- Super admins
super_admins (
  id            UUID PRIMARY KEY,
  email         VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,
  created_at    TIMESTAMPTZ DEFAULT NOW()
)
```

### 3.2 Tenant Schema (per org)

```sql
-- Users
users (
  id              UUID PRIMARY KEY,
  email           VARCHAR(255) UNIQUE NOT NULL,
  password_hash   VARCHAR(255),
  full_name       VARCHAR(255) NOT NULL,
  role_id         UUID REFERENCES roles(id),
  status          VARCHAR(20) DEFAULT 'PENDING',  -- PENDING | ACTIVE | SUSPENDED
  invited_by      UUID REFERENCES users(id),
  created_at      TIMESTAMPTZ DEFAULT NOW(),
  updated_at      TIMESTAMPTZ DEFAULT NOW()
)

-- Roles
roles (
  id           UUID PRIMARY KEY,
  name         VARCHAR(100) NOT NULL,
  description  TEXT,
  is_system    BOOLEAN DEFAULT FALSE,  -- TRUE for built-in roles
  created_at   TIMESTAMPTZ DEFAULT NOW()
)

-- Permissions
permissions (
  id     UUID PRIMARY KEY,
  code   VARCHAR(100) UNIQUE NOT NULL,  -- e.g. "cr.create", "users.manage"
  label  VARCHAR(255) NOT NULL
)

-- Role <-> Permission mapping
role_permissions (
  role_id        UUID REFERENCES roles(id) ON DELETE CASCADE,
  permission_id  UUID REFERENCES permissions(id) ON DELETE CASCADE,
  PRIMARY KEY (role_id, permission_id)
)

-- Groups
groups (
  id          UUID PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  description TEXT,
  created_by  UUID REFERENCES users(id),
  created_at  TIMESTAMPTZ DEFAULT NOW()
)

-- User <-> Group membership
user_groups (
  user_id   UUID REFERENCES users(id) ON DELETE CASCADE,
  group_id  UUID REFERENCES groups(id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, group_id)
)

-- Default approver configuration
default_approvers (
  id           UUID PRIMARY KEY,
  user_id      UUID REFERENCES users(id) ON DELETE CASCADE,
  is_required  BOOLEAN DEFAULT TRUE,
  position     INT NOT NULL,             -- order for Linear workflows
  created_at   TIMESTAMPTZ DEFAULT NOW()
)

-- Custom fields definition
custom_field_definitions (
  id            UUID PRIMARY KEY,
  label         VARCHAR(255) NOT NULL,
  field_type    VARCHAR(50) NOT NULL,    -- TEXT | NUMBER | DATE | DROPDOWN | CHECKBOX
  options       JSONB,                   -- for DROPDOWN type
  is_required   BOOLEAN DEFAULT FALSE,
  display_order INT NOT NULL,
  created_at    TIMESTAMPTZ DEFAULT NOW()
)

-- Org-wide settings
org_settings (
  key        VARCHAR(100) PRIMARY KEY,
  value      TEXT NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT NOW()
)
-- Example keys: approval_type, max_upload_size_mb, storage_backend, smtp_host, etc.

-- SLA Policies
sla_policies (
  id                    UUID PRIMARY KEY,
  name                  VARCHAR(255) NOT NULL,
  priority_trigger      VARCHAR(20),            -- LOW|MEDIUM|HIGH|CRITICAL|ALL
  deadline_hours        INT NOT NULL,
  warning_before_hours  INT,                    -- notify X hours before breach
  created_at            TIMESTAMPTZ DEFAULT NOW()
)

-- SLA escalation contacts
sla_escalation_contacts (
  sla_policy_id UUID REFERENCES sla_policies(id) ON DELETE CASCADE,
  user_id       UUID REFERENCES users(id) ON DELETE CASCADE,
  PRIMARY KEY (sla_policy_id, user_id)
)

-- Change Requests
change_requests (
  id               UUID PRIMARY KEY,
  title            VARCHAR(500) NOT NULL,
  description      TEXT,                        -- rich text HTML
  priority         VARCHAR(20) NOT NULL,        -- LOW|MEDIUM|HIGH|CRITICAL
  risk_level       VARCHAR(20) NOT NULL,        -- LOW|MEDIUM|HIGH|CRITICAL
  category         VARCHAR(255),
  status           VARCHAR(30) DEFAULT 'DRAFT', -- DRAFT|PENDING_APPROVAL|APPROVED|REJECTED|CANCELLED
  approval_type    VARCHAR(20) NOT NULL,        -- LINEAR|NON_LINEAR
  approval_locked  BOOLEAN DEFAULT FALSE,       -- TRUE once first decision is made
  scheduled_start  TIMESTAMPTZ,
  scheduled_end    TIMESTAMPTZ,
  affected_systems TEXT[],
  sla_deadline     TIMESTAMPTZ,
  sla_breached     BOOLEAN DEFAULT FALSE,
  created_by       UUID REFERENCES users(id),
  created_at       TIMESTAMPTZ DEFAULT NOW(),
  updated_at       TIMESTAMPTZ DEFAULT NOW()
)

-- Custom field values per CR
change_request_custom_fields (
  change_request_id UUID REFERENCES change_requests(id) ON DELETE CASCADE,
  field_id          UUID REFERENCES custom_field_definitions(id),
  value             TEXT,
  PRIMARY KEY (change_request_id, field_id)
)

-- Approvers on a CR
cr_approvers (
  id                UUID PRIMARY KEY,
  change_request_id UUID REFERENCES change_requests(id) ON DELETE CASCADE,
  user_id           UUID REFERENCES users(id),
  is_required       BOOLEAN DEFAULT TRUE,
  position          INT NOT NULL,
  status            VARCHAR(20) DEFAULT 'PENDING', -- PENDING|APPROVED|REJECTED
  rejection_reason  TEXT,
  decided_at        TIMESTAMPTZ,
  is_ad_hoc         BOOLEAN DEFAULT FALSE,
  created_at        TIMESTAMPTZ DEFAULT NOW()
)

-- File attachments
attachments (
  id                UUID PRIMARY KEY,
  change_request_id UUID REFERENCES change_requests(id) ON DELETE CASCADE,
  comment_id        UUID,                        -- nullable; FK set below
  uploader_id       UUID REFERENCES users(id),
  file_name         VARCHAR(500) NOT NULL,
  mime_type         VARCHAR(100),
  size_bytes        BIGINT,
  storage_path      TEXT NOT NULL,               -- relative path or S3 key
  created_at        TIMESTAMPTZ DEFAULT NOW()
)

-- Comments
comments (
  id                UUID PRIMARY KEY,
  change_request_id UUID REFERENCES change_requests(id) ON DELETE CASCADE,
  author_id         UUID REFERENCES users(id),
  body              TEXT NOT NULL,               -- rich text HTML
  created_at        TIMESTAMPTZ DEFAULT NOW(),
  updated_at        TIMESTAMPTZ DEFAULT NOW()
)

-- comment_id FK in attachments
ALTER TABLE attachments ADD CONSTRAINT fk_comment
  FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE SET NULL;

-- @Mentions in comments
comment_mentions (
  comment_id UUID REFERENCES comments(id) ON DELETE CASCADE,
  user_id    UUID REFERENCES users(id) ON DELETE CASCADE,
  PRIMARY KEY (comment_id, user_id)
)

-- Activity stream (audit trail per CR)
activity_stream (
  id                UUID PRIMARY KEY,
  change_request_id UUID REFERENCES change_requests(id) ON DELETE CASCADE,
  actor_id          UUID REFERENCES users(id),
  action_type       VARCHAR(100) NOT NULL,       -- see Section 3.3
  payload           JSONB,                        -- before/after values, metadata
  created_at        TIMESTAMPTZ DEFAULT NOW()
)

-- Global audit trail
audit_log (
  id          UUID PRIMARY KEY,
  actor_id    UUID,                              -- NULL if system action
  actor_email VARCHAR(255),                      -- denormalised for immutability
  action_type VARCHAR(100) NOT NULL,
  entity_type VARCHAR(100),                      -- change_request | user | role | ...
  entity_id   UUID,
  payload     JSONB,
  ip_address  VARCHAR(45),
  created_at  TIMESTAMPTZ DEFAULT NOW()
)

-- In-app notifications
notifications (
  id          UUID PRIMARY KEY,
  recipient_id UUID REFERENCES users(id) ON DELETE CASCADE,
  type        VARCHAR(100) NOT NULL,
  title       VARCHAR(500),
  body        TEXT,
  link        TEXT,                              -- deep link to CR
  is_read     BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMPTZ DEFAULT NOW()
)

-- Password reset tokens
password_reset_tokens (
  id          UUID PRIMARY KEY,
  user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL,
  used        BOOLEAN DEFAULT FALSE
)

-- Refresh tokens
refresh_tokens (
  id          UUID PRIMARY KEY,
  user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash  VARCHAR(255) NOT NULL,
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked     BOOLEAN DEFAULT FALSE
)

-- OAuth account links
oauth_accounts (
  id           UUID PRIMARY KEY,
  user_id      UUID REFERENCES users(id) ON DELETE CASCADE,
  provider     VARCHAR(20) NOT NULL,              -- GOOGLE | MICROSOFT
  provider_sub VARCHAR(500) NOT NULL,             -- provider's unique subject identifier
  email        VARCHAR(255) NOT NULL,
  linked_at    TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE (provider, provider_sub)
)
```

### 3.3 Activity Stream Action Types

| Action Type | Description |
|---|---|
| `CR_CREATED` | Change request created |
| `CR_SUBMITTED` | Draft submitted for approval |
| `CR_FIELD_UPDATED` | A CR field was edited |
| `CR_STATUS_CHANGED` | Status transition occurred |
| `CR_CANCELLED` | CR cancelled by requester/admin |
| `APPROVER_ADDED` | Approver added to CR |
| `APPROVER_REMOVED` | Approver removed from CR |
| `APPROVER_REORDERED` | Approver order changed |
| `APPROVER_REQUIRED_CHANGED` | Required/Optional toggle changed |
| `APPROVAL_DECIDED` | Approver approved or rejected |
| `APPROVAL_DECISION_CHANGED` | Approver changed their decision |
| `APPROVAL_TYPE_CHANGED` | Linear ↔ Non-linear toggle |
| `COMMENT_ADDED` | New comment posted |
| `ATTACHMENT_UPLOADED` | File attached to CR or comment |
| `SLA_WARNING` | SLA warning threshold reached |
| `SLA_BREACHED` | SLA deadline passed |

---

## 4. Functional Requirements

### 4.1 Authentication

| ID | Requirement |
|---|---|
| AUTH-01 | Passwords must be hashed using bcrypt (min cost factor 12) |
| AUTH-02 | Auth uses short-lived JWT access tokens (15 min) + long-lived refresh tokens (7 days) in HttpOnly cookies |
| AUTH-03 | Refresh tokens are rotated on each use |
| AUTH-04 | Password reset links expire after 1 hour and are single-use |
| AUTH-05 | Failed login attempts are rate-limited (5 attempts per 15 min per IP) |
| AUTH-06 | Super Admin and tenant users share the same auth endpoint; tenant resolved from request context |
| AUTH-07 | Google SSO implemented via Spring Security OAuth2 Client using OIDC (`openid`, `email`, `profile` scopes) |
| AUTH-08 | Microsoft SSO implemented via Azure AD OIDC endpoint (`https://login.microsoftonline.com/{tenantId}/v2.0`); supports both single-tenant and common (multi-tenant) Azure AD configurations |
| AUTH-09 | SSO callback flow: provider returns to `/api/v1/auth/oauth/{provider}/callback` → validate ID token → domain check → JIT provision or match existing user → issue JWT + refresh token → redirect to frontend |
| AUTH-10 | JIT provisioning assigns a configurable default role (Admin-configurable, defaults to `Requester`) |
| AUTH-11 | A user can link multiple SSO providers to the same account (matched by email) |
| AUTH-12 | SSO credentials (client ID, client secret) are stored encrypted (AES-256) in `tenant_sso_configs` |

### 4.1.1 Domain Whitelisting

| ID | Requirement |
|---|---|
| DW-01 | Super Admin manages allowed domains in `tenant_allowed_domains` (public schema) |
| DW-02 | Whitelisting is active for a tenant when one or more domain entries exist; inactive when empty |
| DW-03 | Domain check applies to: local login, SSO login, and user invite |
| DW-04 | Domain extracted from email is lowercased and compared case-insensitively against allowed list |
| DW-05 | Blocked access returns HTTP 403 with error code `DOMAIN_NOT_PERMITTED` |
| DW-06 | Domain add/remove events are written to the platform-level audit log |
| DW-07 | Super Admins are exempt from domain whitelisting |

### 4.2 Multi-Tenancy

| ID | Requirement |
|---|---|
| MT-01 | Each tenant has an isolated PostgreSQL schema |
| MT-02 | Tenant is resolved from subdomain (e.g., `acme.audita.io`) or `X-Tenant-ID` header |
| MT-03 | Cross-tenant data access is strictly prohibited at the API layer |
| MT-04 | Flyway runs per-tenant schema migrations at tenant creation and on application startup |
| MT-05 | Super Admin API endpoints are protected by a separate authority scope |

### 4.3 User & Role Management

| ID | Requirement |
|---|---|
| USR-01 | A user holds exactly one role at a time |
| USR-02 | Built-in roles (`Admin`, `Requester`, `Approver`, `Auditor`) are immutable |
| USR-03 | Custom roles are created by Admins with a subset of the system permission codes |
| USR-04 | Role-based access is enforced via Spring Security method-level `@PreAuthorize` annotations |
| USR-05 | Auditor role users have read-only access enforced at the API level (no mutation endpoints accessible) |
| USR-06 | User invitation emails include a secure, time-limited acceptance link (48 hours) |
| USR-07 | Admins can deactivate/reactivate users; deactivated users cannot log in |

### 4.4 Groups

| ID | Requirement |
|---|---|
| GRP-01 | Groups are freeform; no hierarchy |
| GRP-02 | A user can belong to zero or more groups |
| GRP-03 | Group membership changes are logged in the audit trail |

### 4.5 Change Requests

| ID | Requirement |
|---|---|
| CR-01 | A CR can be saved as Draft before submission |
| CR-02 | Submitting a CR transitions status to `PENDING_APPROVAL` and triggers approver notifications |
| CR-03 | A CR in `DRAFT` state can be fully edited by the Requester |
| CR-04 | Fields can be edited in `PENDING_APPROVAL` state; all changes are logged in the activity stream |
| CR-05 | Closed CRs (`APPROVED`, `REJECTED`, `CANCELLED`) are read-only |
| CR-06 | Only the Requester or Admin can cancel a CR (in `DRAFT` or `PENDING_APPROVAL` state) |
| CR-07 | Custom fields defined by Admin appear on the CR form and are stored per-CR |
| CR-08 | SLA deadline is computed at submission time based on the matching SLA policy and priority |
| CR-09 | The system evaluates SLA deadlines via a scheduled job every 5 minutes |

### 4.6 Approval Workflow

| ID | Requirement |
|---|---|
| WF-01 | Default approvers from org settings are cloned into each new CR at creation |
| WF-02 | Requester can add ad-hoc approvers at creation or after (while CR is not closed) |
| WF-03 | Requester can override approval type (Linear/Non-linear) before `approval_locked = TRUE` |
| WF-04 | `approval_locked` is set to `TRUE` when the first approver records any decision |
| WF-05 | In Linear mode, only Approver #1 (lowest `position`) is notified on submission; each subsequent approver is notified after the preceding one acts |
| WF-06 | In Non-linear mode, all approvers are notified simultaneously on submission |
| WF-07 | Approvers can change their decision at any time while CR is not closed |
| WF-08 | Rejection requires a non-empty reason string |
| WF-09 | Rejection closure rule: if one required approver exists and rejects → `REJECTED`; if all required approvers have rejected → `REJECTED`; otherwise CR remains `PENDING_APPROVAL` |
| WF-10 | Approval closure rule: all required approvers have status `APPROVED` → CR transitions to `APPROVED` |
| WF-11 | Admins can change Required/Optional status of approvers at any time while CR is not closed |
| WF-12 | Re-evaluating closure state is triggered on every approver decision change |

### 4.7 Comments & Collaboration

| ID | Requirement |
|---|---|
| COM-01 | Comments use TipTap rich-text editor with extensions: Bold, Italic, Lists, Code, Link, Image, Mention |
| COM-02 | `@mention` autocomplete queries active users in the tenant |
| COM-03 | Mentioned users receive in-app + email notifications |
| COM-04 | Comments are immutable after posting (no edit/delete in v1) |
| COM-05 | File attachments in comments are stored via the same file storage module as CR attachments |

### 4.8 File Storage

| ID | Requirement |
|---|---|
| FILE-01 | Storage backend is configurable via `org_settings`: `LOCAL` or `S3` |
| FILE-02 | For LOCAL: files stored under a configurable base path on the server filesystem |
| FILE-03 | For S3: AWS SDK v2 is used; bucket, region, access key, secret configured via org settings (or env vars) |
| FILE-04 | Max file size and allowed MIME types are configurable per org |
| FILE-05 | File download requires authenticated request; pre-signed URLs used for S3 |
| FILE-06 | Virus scanning hook is provided (pluggable; no scanner bundled in v1) |

### 4.9 Notifications

| ID | Requirement |
|---|---|
| NOTIF-01 | In-app notifications delivered via Server-Sent Events (SSE) per authenticated user session |
| NOTIF-02 | Notifications are persisted in the `notifications` table for replay on reconnect |
| NOTIF-03 | Unread notification count is returned on every authenticated API response header (`X-Unread-Count`) |
| NOTIF-04 | Email notifications use Thymeleaf HTML templates with a plaintext fallback |
| NOTIF-05 | SMTP settings (host, port, username, password, TLS) are configurable per org via Admin settings |
| NOTIF-06 | Notification dispatch is asynchronous (Spring `@Async` thread pool or a lightweight internal queue) |

### 4.10 Audit Trail

| ID | Requirement |
|---|---|
| AUDIT-01 | Every state-changing operation writes to `activity_stream` (scoped to CR) and `audit_log` (global) |
| AUDIT-02 | Audit entries are never deleted or modified |
| AUDIT-03 | `payload` JSONB column stores before/after field values for diff display |
| AUDIT-04 | Global audit trail is accessible to `Admin` and `Auditor` roles only |
| AUDIT-05 | Audit trail supports filtering by actor, action type, entity type, entity ID, date range |
| AUDIT-06 | Admins can export filtered audit log to CSV |
| AUDIT-07 | Actor email is denormalised into `audit_log` to preserve historical accuracy |

---

## 5. API Design

### 5.1 Conventions

- RESTful JSON API under `/api/v1/`
- Tenant resolved from `X-Tenant-Slug` request header or subdomain
- Super Admin routes: `/api/platform/v1/`
- Auth: `Authorization: Bearer <access_token>` header
- Pagination: `?page=0&size=20&sort=createdAt,desc`
- Errors follow RFC 7807 Problem Detail format

### 5.2 Core Endpoint Groups

#### Authentication
```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password

# SSO — initiates OIDC redirect
GET    /api/v1/auth/oauth/google
GET    /api/v1/auth/oauth/microsoft
# SSO callbacks (provider redirects here)
GET    /api/v1/auth/oauth/google/callback
GET    /api/v1/auth/oauth/microsoft/callback
```

#### Platform (Super Admin)
```
GET    /api/platform/v1/tenants
POST   /api/platform/v1/tenants
GET    /api/platform/v1/tenants/{id}
PATCH  /api/platform/v1/tenants/{id}
DELETE /api/platform/v1/tenants/{id}

# Domain whitelisting
GET    /api/platform/v1/tenants/{id}/domains
POST   /api/platform/v1/tenants/{id}/domains
DELETE /api/platform/v1/tenants/{id}/domains/{domainId}

# SSO configuration
GET    /api/platform/v1/tenants/{id}/sso
PUT    /api/platform/v1/tenants/{id}/sso/{provider}
DELETE /api/platform/v1/tenants/{id}/sso/{provider}
```

#### Users
```
GET    /api/v1/users
POST   /api/v1/users/invite
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}
DELETE /api/v1/users/{id}
POST   /api/v1/users/accept-invite
```

#### Roles
```
GET    /api/v1/roles
POST   /api/v1/roles
GET    /api/v1/roles/{id}
PUT    /api/v1/roles/{id}
DELETE /api/v1/roles/{id}
GET    /api/v1/permissions
```

#### Groups
```
GET    /api/v1/groups
POST   /api/v1/groups
GET    /api/v1/groups/{id}
PUT    /api/v1/groups/{id}
DELETE /api/v1/groups/{id}
POST   /api/v1/groups/{id}/members
DELETE /api/v1/groups/{id}/members/{userId}
```

#### Change Requests
```
GET    /api/v1/change-requests
POST   /api/v1/change-requests
GET    /api/v1/change-requests/{id}
PATCH  /api/v1/change-requests/{id}
POST   /api/v1/change-requests/{id}/submit
POST   /api/v1/change-requests/{id}/cancel

GET    /api/v1/change-requests/{id}/approvers
POST   /api/v1/change-requests/{id}/approvers
PATCH  /api/v1/change-requests/{id}/approvers/{approverId}
DELETE /api/v1/change-requests/{id}/approvers/{approverId}
POST   /api/v1/change-requests/{id}/approvers/{approverId}/reorder

POST   /api/v1/change-requests/{id}/approve
POST   /api/v1/change-requests/{id}/reject

GET    /api/v1/change-requests/{id}/comments
POST   /api/v1/change-requests/{id}/comments

GET    /api/v1/change-requests/{id}/attachments
POST   /api/v1/change-requests/{id}/attachments
DELETE /api/v1/change-requests/{id}/attachments/{attachmentId}

GET    /api/v1/change-requests/{id}/activity
```

#### Admin Settings
```
GET    /api/v1/settings
PUT    /api/v1/settings

GET    /api/v1/settings/default-approvers
POST   /api/v1/settings/default-approvers
DELETE /api/v1/settings/default-approvers/{id}
PATCH  /api/v1/settings/default-approvers/{id}/reorder

GET    /api/v1/settings/custom-fields
POST   /api/v1/settings/custom-fields
PUT    /api/v1/settings/custom-fields/{id}
DELETE /api/v1/settings/custom-fields/{id}

GET    /api/v1/settings/sla-policies
POST   /api/v1/settings/sla-policies
PUT    /api/v1/settings/sla-policies/{id}
DELETE /api/v1/settings/sla-policies/{id}
```

#### Audit Trail
```
GET    /api/v1/audit-log
GET    /api/v1/audit-log/export        (CSV)
```

#### Notifications
```
GET    /api/v1/notifications
PATCH  /api/v1/notifications/{id}/read
POST   /api/v1/notifications/read-all
GET    /api/v1/notifications/stream    (SSE endpoint)
```

---

## 6. Security Requirements

| ID | Requirement |
|---|---|
| SEC-01 | All API endpoints require authentication except: login, forgot-password, reset-password, accept-invite |
| SEC-02 | HTTPS enforced in all environments; HTTP redirects to HTTPS |
| SEC-03 | CORS configured to allow only the frontend origin |
| SEC-04 | CSRF protection applied via `SameSite=Strict` on refresh token cookie |
| SEC-05 | SQL injection prevented by exclusive use of parameterised queries (JPA/Hibernate) |
| SEC-06 | XSS prevention: rich-text HTML is sanitised server-side using OWASP Java HTML Sanitizer before storage |
| SEC-07 | File uploads: MIME type validated server-side; filename sanitised; path traversal prevention enforced |
| SEC-08 | Sensitive settings (SMTP password, S3 secret key) encrypted at rest using AES-256 before DB storage |
| SEC-09 | Rate limiting applied to: login (5/15min), forgot-password (3/hour), invite-accept (10/hour) |
| SEC-10 | Tenant isolation: every DB query includes tenant schema context; no cross-tenant query possible at the ORM layer |
| SEC-11 | Audit log entries include actor IP address |
| SEC-12 | SSO ID tokens validated against provider's JWKS endpoint before trusting claims |
| SEC-13 | OAuth2 state parameter used on all SSO flows to prevent CSRF; verified on callback |
| SEC-14 | SSO client secrets stored AES-256 encrypted; never returned in API responses |

---

## 7. Non-Functional Requirements

### 7.1 Performance

| Metric | Target |
|---|---|
| API response time (p95) | < 300ms for list endpoints (10k records) |
| File upload throughput | Support up to 50 MB files |
| SSE notification delivery | < 2 seconds from event to client receipt |
| CR list query | Must be paginated; indexed on `status`, `created_by`, `created_at`, `priority` |

### 7.2 Scalability

- Stateless API (sessions are JWT-based; no server-side session state).
- Horizontal scaling behind a load balancer with sticky sessions only required for SSE connections.
- Database connection pooling via HikariCP.

### 7.3 Reliability

- Flyway schema migrations are idempotent and version-controlled.
- Notification dispatch failures are retried up to 3 times with exponential backoff.
- Scheduled SLA evaluation job uses `@Scheduled` with configurable interval (default 5 min).

### 7.4 Maintainability

- Backend follows hexagonal (ports and adapters) architecture: `domain`, `application`, `infrastructure`, `api` packages.
- Frontend follows Nuxt's file-system routing with composables for API calls and Pinia stores for state.
- All environment-specific configuration via environment variables (12-factor app).

### 7.5 Observability

- Structured JSON logging via SLF4J + Logback.
- Spring Boot Actuator health endpoints exposed for container health checks.
- All request logs include `tenant_id`, `user_id`, `request_id` (MDC).

---

## 8. Environment Variables (Key)

```env
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/audita
DATABASE_USERNAME=audita
DATABASE_PASSWORD=secret

# JWT
JWT_SECRET=<256-bit-secret>
JWT_EXPIRY_SECONDS=900
REFRESH_TOKEN_EXPIRY_DAYS=7

# File Storage
STORAGE_BACKEND=LOCAL           # LOCAL | S3
STORAGE_LOCAL_BASE_PATH=/data/uploads
STORAGE_S3_BUCKET=audita-uploads
STORAGE_S3_REGION=us-east-1
STORAGE_S3_ACCESS_KEY=
STORAGE_S3_SECRET_KEY=

# Email (default SMTP — org-level overrides stored in DB)
DEFAULT_SMTP_HOST=smtp.example.com
DEFAULT_SMTP_PORT=587
DEFAULT_SMTP_USERNAME=
DEFAULT_SMTP_PASSWORD=
DEFAULT_SMTP_TLS=true

# SSO (platform-level fallback; per-tenant overrides stored in DB)
OAUTH_GOOGLE_CLIENT_ID=
OAUTH_GOOGLE_CLIENT_SECRET=
OAUTH_MICROSOFT_CLIENT_ID=
OAUTH_MICROSOFT_CLIENT_SECRET=
OAUTH_MICROSOFT_TENANT_ID=common           # "common" for multi-tenant Azure AD
OAUTH_REDIRECT_BASE_URL=https://app.audita.io

# Encryption key for sensitive stored values (SSO secrets, SMTP passwords)
APP_ENCRYPTION_KEY=<32-byte-hex>
```

---

## 9. Frontend Architecture (Nuxt 3)

### 9.1 Directory Structure

```
audita-web/
├── assets/
├── components/
│   ├── cr/                  # Change request components
│   ├── approvers/           # Approver list, decision components
│   ├── comments/            # Rich text comment thread
│   ├── notifications/       # Notification bell, feed
│   └── shared/              # Buttons, modals, tables
├── composables/
│   ├── useAuth.ts
│   ├── useChangeRequests.ts
│   ├── useNotifications.ts  # SSE connection management
│   └── useToast.ts
├── layouts/
│   ├── default.vue          # Authenticated layout (sidebar + header)
│   ├── auth.vue             # Login / password reset layout
│   └── platform.vue         # Super Admin layout
├── pages/
│   ├── auth/
│   ├── dashboard/
│   ├── change-requests/
│   ├── admin/               # Settings pages
│   ├── audit-trail/
│   └── platform/            # Super Admin pages
├── stores/
│   ├── auth.ts
│   ├── notifications.ts
│   └── settings.ts
├── middleware/
│   ├── auth.ts              # Redirect unauthenticated users
│   ├── role.ts              # Role-based route guards
│   └── tenant.ts            # Set tenant context from subdomain
└── plugins/
    ├── api.ts               # $fetch wrapper with auth headers
    └── sse.ts               # SSE client plugin
```

### 9.2 State Management (Pinia)

- `useAuthStore` — current user, token management, login/logout.
- `useNotificationStore` — unread count, notification list, SSE stream.
- `useSettingsStore` — org settings cache (approval type, custom fields, etc.).

### 9.3 Rich Text Editor

- **TipTap** used for both CR descriptions and comments.
- Extensions: `StarterKit`, `Link`, `Image`, `Mention` (custom user mention with backend autocomplete), `Placeholder`.
- HTML output stored and rendered with XSS sanitisation on the backend.

---

## 10. Deployment

### 10.1 Docker Compose (Development/Self-hosted)

```yaml
services:
  api:
    build: ./audita-api
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/audita
    depends_on: [db]
    ports: ["8080:8080"]

  web:
    build: ./audita-web
    ports: ["3000:3000"]

  db:
    image: postgres:16-alpine
    volumes: [pgdata:/var/lib/postgresql/data]
    environment:
      POSTGRES_DB: audita
      POSTGRES_PASSWORD: secret

volumes:
  pgdata:
```

### 10.2 Production (Kubernetes / Helm)

- API Deployment + Service + HPA
- Nuxt Web Deployment + Service + Ingress (with TLS via cert-manager)
- PostgreSQL via managed service (RDS, Cloud SQL) or StatefulSet
- Persistent Volume for local file storage (or S3 bucket)
- ConfigMap for non-secret env vars; Sealed Secrets / External Secrets for credentials
