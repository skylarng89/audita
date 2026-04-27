# Audita — Developer Task List

**Project:** Audita — Multi-Tenant ITIL/ITSM Change Management Platform
**Version:** 0.1.0
**Last Updated:** 2026-04-27
**Team Size:** 2–3 Developers

---

## Task Status Legend

- 🔴 **Not Started** — Task has not been started
- 🟡 **In Progress** — Task is currently being worked on
- ✅ **Completed** — Task is finished and tested

---

## Sprint 0: Foundation & Scaffolding (Week 1–2)

> **Goal:** Both repositories are runnable locally. Dev environment is fully reproducible via Docker Compose. CI pipeline is in place. No application logic yet.

### Backend Scaffold (`audita-api`)

| Task ID  | Task                                                                                      | Priority | Status       | Assigned To | Notes                                                                                                 |
| -------- | ----------------------------------------------------------------------------------------- | -------- | ------------ | ----------- | ----------------------------------------------------------------------------------------------------- |
| INIT-001 | Initialise Gradle multi-module Spring Boot 4 project (Java 25)                            | High     | ✅ Completed | Developer 1 | `settings.gradle.kts` + root `build.gradle.kts`; Spring Boot 4.0.6 BOM; Java 25 toolchain             |
| INIT-002 | Configure HikariCP + Hibernate 7 + PostgreSQL 16 connection                               | High     | ✅ Completed | Developer 1 | `application.yml` — HikariCP pool; `spring-boot-starter-data-jpa`; `postgresql` driver                |
| INIT-003 | Configure Flyway for `public` schema baseline migration                                   | High     | ✅ Completed | Developer 1 | `db/migration/public/V1__create_public_schema.sql`; `baseline-on-migrate: true`                       |
| INIT-004 | Implement `TenantContext` thread-local + Hibernate `CurrentTenantIdentifierResolver`      | High     | ✅ Completed | Developer 1 | `TenantContext`, `AuditaTenantIdentifierResolver`, `AuditaMultiTenantConnectionProvider`, `JpaConfig` |
| INIT-005 | Implement per-tenant schema Flyway migration runner                                       | High     | ✅ Completed | Developer 1 | `FlywayTenantMigrator` + `db/migration/tenant/V1__create_tenant_schema.sql`                           |
| INIT-006 | Configure Spring Boot Actuator health endpoints                                           | Medium   | ✅ Completed | Developer 1 | `spring-boot-starter-actuator`; `health,info` exposed in `application.yml`                            |
| INIT-007 | Configure Logback structured JSON logging with MDC (`tenant_id`, `user_id`, `request_id`) | Medium   | ✅ Completed | Developer 1 | `logback-spring.xml`: JSON (prod) + coloured (dev); `logstash-logback-encoder:8.1`                    |
| INIT-008 | Configure CORS (allow frontend origin only)                                               | High     | ✅ Completed | Developer 1 | `SecurityConfig.java` — `CorsConfiguration` + `UrlBasedCorsConfigurationSource`                       |
| INIT-009 | Set up RFC 7807 Problem Detail global exception handler                                   | High     | ✅ Completed | Developer 1 | `GlobalExceptionHandler.java`; `mvc.problem-details.enabled: true`                                    |
| INIT-010 | Write Docker Compose: `api`, `web`, `db` services                                         | High     | ✅ Completed | Developer 1 | `docker-compose.yml` — PostgreSQL, MailHog, API, Nuxt with healthchecks                               |

### Frontend Scaffold (`audita-web`)

| Task ID  | Task                                                                                                  | Priority | Status       | Assigned To | Notes                                                                                                           |
| -------- | ----------------------------------------------------------------------------------------------------- | -------- | ------------ | ----------- | --------------------------------------------------------------------------------------------------------------- |
| INIT-011 | Initialise Nuxt 3 project with pnpm, TypeScript, Tailwind CSS                                         | High     | ✅ Completed | Developer 2 | `nuxt.config.ts`, `pnpm-lock.yaml`; dark mode via `class` strategy                                              |
| INIT-012 | Configure Tailwind design tokens (colours, typography) per design.md                                  | High     | ✅ Completed | Developer 2 | `tailwind.config.ts` — primary, surface, danger, warning, success, info palette                                 |
| INIT-013 | Create `default.vue`, `auth.vue`, `platform.vue` layouts                                              | High     | ✅ Completed | Developer 2 | All three layout files present in `layouts/`                                                                    |
| INIT-014 | Create `plugins/api.ts` — `$fetch` wrapper with auth header injection and 401 handling                | High     | ✅ Completed | Developer 2 | `plugins/api.ts`; injects Authorization + X-Tenant-Slug; silent refresh on 401                                  |
| INIT-015 | Create shared component library baseline: Button, Input, Badge, Card, Modal, Toast, Table, Pagination | High     | ✅ Completed | Developer 2 | `AppButton`, `AppInput`, `AppBadge`, `AppCard`, `AppModal`, `AppTable`, `AppPagination` in `components/shared/` |
| INIT-016 | Create `middleware/auth.ts` — redirect unauthenticated to login                                       | High     | ✅ Completed | Developer 2 | `middleware/auth.ts` present                                                                                    |
| INIT-017 | Create `middleware/role.ts` — role-based route guard                                                  | High     | ✅ Completed | Developer 2 | `middleware/role.ts` — uses `to.meta.requiredRole`                                                              |
| INIT-018 | Create `middleware/tenant.ts` — resolve tenant slug from subdomain                                    | Medium   | ✅ Completed | Developer 2 | `middleware/tenant.ts` — subdomain → slug; ?tenant= query param in dev; writes to `auth.tenantSlug`             |
| INIT-019 | Configure runtime config for API base URL                                                             | Medium   | ✅ Completed | Developer 2 | `nuxt.config.ts` — `runtimeConfig.public.apiBase` from `NUXT_PUBLIC_API_BASE`                                   |

---

## Progress Tracking

### Overall Progress by Sprint

| Sprint    | Total Tasks | Not Started | In Progress | Completed | Progress % |
| --------- | ----------- | ----------- | ----------- | --------- | ---------- |
| Sprint 0  | 19          | 0           | 0           | 19        | 100%       |
| Sprint 1  | 22          | 0           | 0           | 22        | 100%       |
| **TOTAL** | **41**      | **0**       | **0**       | **41**    | **100%**   |

---

## Recent Implementations

### Sprint 0 — Foundation & Scaffolding (Completed 2026-04-27)

**Overview**: Both repositories are fully scaffolded, runnable via Docker Compose, with structured logging, tenant middleware, and a shared UI component library.

**Files Created/Modified**:

- `audita-api/api/build.gradle.kts` — added `logstash-logback-encoder:8.1` dependency
- `audita-api/api/src/main/resources/logback-spring.xml` — JSON appender (prod) + coloured console (dev); MDC: `tenant_id`, `user_id`, `request_id`
- `audita-web/components/shared/AppButton.vue` — variants: primary, secondary, ghost, danger; sizes: sm/md/lg; loading state
- `audita-web/components/shared/AppInput.vue` — label, error, hint, prefix/suffix slots; v-model
- `audita-web/components/shared/AppBadge.vue` — success/warning/danger/info/neutral/primary variants; optional dot
- `audita-web/components/shared/AppCard.vue` — header/body/footer slots; dark-mode aware
- `audita-web/components/shared/AppModal.vue` — Teleport, Transition, Escape-to-close, backdrop-click, ARIA
- `audita-web/components/shared/AppTable.vue` — typed columns, loading skeleton, empty state, named cell slots
- `audita-web/components/shared/AppPagination.vue` — ellipsis-aware page numbers, range summary, ARIA
- `audita-web/middleware/tenant.ts` — subdomain → slug extraction; `?tenant=` dev fallback; writes `auth.tenantSlug`

**Key Changes**:

- Logback uses Spring profile switching: `!dev` → LogstashEncoder JSON; `dev` → coloured pattern
- Tenant middleware writes to `auth.tenantSlug` (already consumed by `plugins/api.ts` for `X-Tenant-Slug` injection)
- `?tenant=` query param only active when `import.meta.dev` is true — cannot be abused in production

**Test Coverage**: Sprint 0 is scaffold-only; no business logic tests required at this stage.

---

## Sprint 1: Authentication & Platform Bootstrap (Week 3–4)

> **Goal:** Super Admin can be registered on first run. Org users can log in via email/password. Password reset works. JWT + refresh token cycle works end-to-end. This unlocks all subsequent work.

### Backend — Auth Module

| Task ID  | Task                                                                       | Priority | Status       | Assigned To | Notes                                                                                     |
| -------- | -------------------------------------------------------------------------- | -------- | ------------ | ----------- | ----------------------------------------------------------------------------------------- |
| AUTH-001 | Implement platform bootstrap: first-run Super Admin registration endpoint  | High     | ✅ Completed | Developer 1 | `PlatformBootstrapController` + `AuthService.bootstrapPlatform()`; idempotent             |
| AUTH-002 | Implement local login endpoint with bcrypt verification                    | High     | ✅ Completed | Developer 1 | Rate limited 5/15min/IP+email; domain whitelist check; `AuthController.login()`           |
| AUTH-003 | Implement JWT access token generation (15 min, RS256 or HS256)             | High     | ✅ Completed | Developer 1 | `JwtService.generateToken()`; HS256; 15-min default                                       |
| AUTH-004 | Implement refresh token issuance, rotation, and revocation                 | High     | ✅ Completed | Developer 1 | HttpOnly cookie; SHA-256 hashed; 7-day rotating; `AuthService.refreshToken()`             |
| AUTH-005 | Implement logout (revoke refresh token, clear cookie)                      | High     | ✅ Completed | Developer 1 | `AuthController.logout()`; clears cookie + deletes DB record                              |
| AUTH-006 | Implement forgot password — generate single-use token, send email          | High     | ✅ Completed | Developer 1 | Rate limited 3/hr/email; 1h expiry; `AuthService.forgotPassword()`                        |
| AUTH-007 | Implement reset password — validate token, update hash, mark used          | High     | ✅ Completed | Developer 1 | `AuthService.resetPassword()` marks token used atomically                                 |
| AUTH-008 | Implement Spring Security JWT filter (validate token, set SecurityContext) | High     | ✅ Completed | Developer 1 | `JwtAuthenticationFilter`; `TenantResolutionFilter` sets context first                    |
| AUTH-009 | Implement domain whitelisting check on login (DW-01 → DW-07)               | High     | ✅ Completed | Developer 1 | `AuthService.checkDomainWhitelist()`; 403 `DOMAIN_NOT_PERMITTED`                          |
| AUTH-010 | Write unit + integration tests for all auth flows                          | High     | ✅ Completed | Developer 1 | 18 unit tests; login, logout, refresh, forgot/reset, domain block, rate limits, bootstrap |

### Backend — Google & Microsoft SSO

| Task ID  | Task                                                            | Priority | Status       | Assigned To | Notes                                                                |
| -------- | --------------------------------------------------------------- | -------- | ------------ | ----------- | -------------------------------------------------------------------- |
| AUTH-011 | Implement Google OIDC SSO initiation and callback               | High     | ✅ Completed | Developer 1 | `SsoService` + `SsoController`; manual OAuth2 flow; state param CSRF |
| AUTH-012 | Implement Microsoft Azure AD OIDC SSO initiation and callback   | High     | ✅ Completed | Developer 1 | Same `SsoService`; `OAuthProvider.MICROSOFT`; supports single-tenant |
| AUTH-013 | Implement JIT user provisioning on first SSO login              | High     | ✅ Completed | Developer 1 | `SsoService.resolveOrProvisionUser()`; default role ACTIVE           |
| AUTH-014 | Implement OAuth account linking (same user, multiple providers) | Medium   | ✅ Completed | Developer 1 | Email-match fallback in `resolveOrProvisionUser()`                   |
| AUTH-015 | Encrypt/decrypt SSO client secrets with AES-256                 | High     | ✅ Completed | Developer 1 | `AesEncryptionService` (AES-256-GCM); `APP_ENCRYPTION_KEY` env var   |

### Frontend — Auth Screens

| Task ID  | Task                                                                                                              | Priority | Status       | Assigned To | Notes                                                                 |
| -------- | ----------------------------------------------------------------------------------------------------------------- | -------- | ------------ | ----------- | --------------------------------------------------------------------- |
| AUTH-016 | Build Sign In page (split-panel design per `audita_sign_in/`)                                                     | High     | ✅ Completed | Developer 2 | `pages/auth/sign-in.vue`; Google/Microsoft SSO buttons; tenant-aware  |
| AUTH-017 | Build Forgot Password page (`audita_forgot_password/`)                                                            | High     | ✅ Completed | Developer 2 | `pages/auth/forgot-password.vue`; success state after submit          |
| AUTH-018 | Build Reset Password page                                                                                         | High     | ✅ Completed | Developer 2 | `pages/auth/reset-password.vue`; strength bar; token from query param |
| AUTH-019 | Build Accept Invite / Complete Setup page (`audita_complete_your_setup/`)                                         | High     | ✅ Completed | Developer 2 | `pages/auth/accept-invite.vue`; full name + password + strength bar   |
| AUTH-020 | Build Platform Bootstrap / First Run wizard                                                                       | High     | ✅ Completed | Developer 2 | `pages/platform/bootstrap.vue`; SA name, email, password; standalone  |
| AUTH-021 | Implement `useAuthStore` (Pinia) — login, logout, refresh token cycle, user state                                 | High     | ✅ Completed | Developer 2 | `stores/auth.ts`; access token in memory; refresh via HttpOnly cookie |
| AUTH-022 | Implement role-based redirect after login (Super Admin → platform dashboard; Admin → dashboard; others → CR list) | High     | ✅ Completed | Developer 2 | `composables/useAuth.ts`; SSO callback page also handles role routing |

---

## Sprint 2: Multi-Tenancy, Users & Groups (Week 5–6)

> **Goal:** Super Admin can provision organisations. Admins can invite users, manage roles/groups. A user can accept an invite and set their password. The application is now multi-tenant end-to-end.

### Backend — Tenant Management

| Task ID    | Task                                                                                                | Priority | Status         | Assigned To | Notes                                                                  |
| ---------- | --------------------------------------------------------------------------------------------------- | -------- | -------------- | ----------- | ---------------------------------------------------------------------- |
| TENANT-001 | Implement Super Admin tenant CRUD endpoints                                                         | High     | 🔴 Not Started | Developer 1 | `GET/POST /api/platform/v1/tenants`; `GET/PATCH/DELETE /{id}`          |
| TENANT-002 | Implement tenant provisioning: create schema, run Flyway migrations, create Admin user, send invite | High     | 🔴 Not Started | Developer 1 | MT-01, MT-04; atomic — all or nothing                                  |
| TENANT-003 | Implement domain whitelist management endpoints (Super Admin)                                       | High     | 🔴 Not Started | Developer 1 | `GET/POST /api/platform/v1/tenants/{id}/domains`; `DELETE /{domainId}` |
| TENANT-004 | Implement SSO config CRUD per tenant (Super Admin)                                                  | Medium   | 🔴 Not Started | Developer 1 | `GET/PUT/DELETE /api/platform/v1/tenants/{id}/sso/{provider}`          |
| TENANT-005 | Write integration tests for tenant provisioning and multi-schema isolation                          | High     | 🔴 Not Started | Developer 1 | MT-03: assert no cross-tenant data access                              |

### Backend — Users & Roles

| Task ID | Task                                                                         | Priority | Status         | Assigned To | Notes                                                                  |
| ------- | ---------------------------------------------------------------------------- | -------- | -------------- | ----------- | ---------------------------------------------------------------------- |
| USR-001 | Seed built-in roles on tenant creation (Admin, Requester, Approver, Auditor) | High     | 🔴 Not Started | Developer 1 | USR-02: immutable system roles                                         |
| USR-002 | Implement user invite endpoint                                               | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/users/invite`; domain whitelist check; send invite email |
| USR-003 | Implement accept-invite endpoint (set password, activate user)               | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/users/accept-invite`; 48h token; rate limit 10/hr        |
| USR-004 | Implement user list, get, update (role, status), deactivate                  | High     | 🔴 Not Started | Developer 1 | `GET/PATCH /api/v1/users`; USR-07                                      |
| USR-005 | Implement custom role CRUD with permission assignments                       | Medium   | 🔴 Not Started | Developer 1 | `GET/POST/PUT/DELETE /api/v1/roles`; USR-03                            |
| USR-006 | Implement permission enforcement via `@PreAuthorize`                         | High     | 🔴 Not Started | Developer 1 | USR-04; method-level security                                          |
| USR-007 | Implement Auditor read-only enforcement at API layer                         | High     | 🔴 Not Started | Developer 1 | USR-05: reject all mutation requests for Auditor role                  |
| USR-008 | Write tests for role/permission enforcement                                  | High     | 🔴 Not Started | Developer 1 | Test each role's allowed/denied endpoints                              |

### Backend — Groups

| Task ID | Task                                        | Priority | Status         | Assigned To | Notes                                                              |
| ------- | ------------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------------ |
| GRP-001 | Implement group CRUD endpoints              | Medium   | 🔴 Not Started | Developer 1 | `GET/POST/PUT/DELETE /api/v1/groups`; GRP-01                       |
| GRP-002 | Implement group member add/remove endpoints | Medium   | 🔴 Not Started | Developer 1 | `POST/DELETE /api/v1/groups/{id}/members/{userId}`; GRP-02, GRP-03 |

### Frontend — Super Admin Platform

| Task ID      | Task                                                                | Priority | Status         | Assigned To | Notes                                                                                             |
| ------------ | ------------------------------------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------------------------------------------- |
| PLATFORM-001 | Build Super Admin platform dashboard (`audita_platform_dashboard/`) | High     | 🔴 Not Started | Developer 2 | KPI: tenant count, global users, system health; top orgs table; domain controls panel; SSO status |
| PLATFORM-002 | Build Tenant Management list page (`audita_tenant_management/`)     | High     | 🔴 Not Started | Developer 2 | Table: org name, status, domain, last audit; actions                                              |
| PLATFORM-003 | Build Provision New Org modal/page (`audita_provision_new_org/`)    | High     | 🔴 Not Started | Developer 2 | Org name, slug, initial admin name + email                                                        |
| PLATFORM-004 | Build domain whitelist management UI (per tenant settings)          | High     | 🔴 Not Started | Developer 2 | Add/remove allowed domains; active/inactive status                                                |
| PLATFORM-005 | Build SSO configuration UI per tenant                               | Medium   | 🔴 Not Started | Developer 2 | Google + Microsoft toggles; client ID / secret fields                                             |

### Frontend — Users & Groups

| Task ID | Task                                                         | Priority | Status         | Assigned To | Notes                                                                                   |
| ------- | ------------------------------------------------------------ | -------- | -------------- | ----------- | --------------------------------------------------------------------------------------- |
| USR-009 | Build User Management page (`audita_user_management/`)       | High     | 🔴 Not Started | Developer 2 | Stats cards; role/status filters; user table with edit/deactivate/resend invite actions |
| USR-010 | Build Invite User modal                                      | High     | 🔴 Not Started | Developer 2 | Email, full name, role dropdown, group multi-select                                     |
| USR-011 | Build Roles & Permissions page (`audita_roles_permissions/`) | Medium   | 🔴 Not Started | Developer 2 | Role list (left); permission toggles by module (right); create custom role              |
| USR-012 | Build Group Management page (`audita_group_management/`)     | Medium   | 🔴 Not Started | Developer 2 | Group list; create group; manage members                                                |

---

## Sprint 3: Change Request Core (Week 7–8)

> **Goal:** Requesters can create, save as draft, edit, and submit change requests. CRs are listed and filterable. CR detail page shows all tabs. This is the core MVP deliverable.

### Backend — Change Request CRUD

| Task ID | Task                                                   | Priority | Status         | Assigned To | Notes                                                                                                  |
| ------- | ------------------------------------------------------ | -------- | -------------- | ----------- | ------------------------------------------------------------------------------------------------------ |
| CR-001  | Implement create CR (Draft)                            | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/change-requests`; CR-01; clone default approvers at creation (WF-01)                     |
| CR-002  | Implement update CR (fields, while not closed)         | High     | 🔴 Not Started | Developer 1 | `PATCH /api/v1/change-requests/{id}`; CR-03, CR-04; log `CR_FIELD_UPDATED` to activity stream          |
| CR-003  | Implement submit CR (Draft → Pending Approval)         | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/change-requests/{id}/submit`; CR-02; compute SLA deadline (CR-08); trigger notifications |
| CR-004  | Implement cancel CR                                    | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/change-requests/{id}/cancel`; CR-06: Requester or Admin only; log `CR_CANCELLED`         |
| CR-005  | Implement CR list endpoint with filtering + pagination | High     | 🔴 Not Started | Developer 1 | `GET /api/v1/change-requests`; filter by status, priority, category, created_by; indexed columns       |
| CR-006  | Implement CR detail endpoint                           | High     | 🔴 Not Started | Developer 1 | `GET /api/v1/change-requests/{id}`                                                                     |
| CR-007  | Implement custom field value storage per CR            | Medium   | 🔴 Not Started | Developer 1 | `change_request_custom_fields` table; CR-07                                                            |
| CR-008  | Write CR state machine unit tests                      | High     | 🔴 Not Started | Developer 1 | Draft→Pending, Pending→Approved, Pending→Rejected, cancelled states                                    |

### Backend — Approvers on CR

| Task ID | Task                                                             | Priority | Status         | Assigned To | Notes                                                                                            |
| ------- | ---------------------------------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------------------------------------------ |
| CR-009  | Implement approver add/remove/reorder on CR                      | High     | 🔴 Not Started | Developer 1 | `POST/PATCH/DELETE /api/v1/change-requests/{id}/approvers`; WF-02, WF-11; log to activity stream |
| CR-010  | Implement approval decision (approve / reject)                   | High     | 🔴 Not Started | Developer 1 | `POST /{id}/approve` and `/{id}/reject`; WF-08; mandatory rejection reason                       |
| CR-011  | Implement approval closure rule evaluation                       | High     | 🔴 Not Started | Developer 1 | WF-09, WF-10, WF-12: evaluate on every decision change; set `approval_locked` on first decision  |
| CR-012  | Implement Linear workflow: notify only next approver in sequence | High     | 🔴 Not Started | Developer 1 | WF-05: after each approval, notify approver at position+1                                        |
| CR-013  | Implement approval type lock (no change after first decision)    | High     | 🔴 Not Started | Developer 1 | WF-03, WF-04: `approval_locked = TRUE` after first decision                                      |

### Frontend — Change Requests

| Task ID | Task                                                                             | Priority | Status         | Assigned To | Notes                                                                                                                                                                                                       |
| ------- | -------------------------------------------------------------------------------- | -------- | -------------- | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CR-014  | Build Change Requests list page (`audita_change_requests/`)                      | High     | 🔴 Not Started | Developer 2 | Table with status badge, priority badge, risk bar, scheduled date, requester avatar; filters; pagination                                                                                                    |
| CR-015  | Build Create Change Request multi-section form (`audita_create_change_request/`) | High     | 🔴 Not Started | Developer 2 | Section 1: Foundational Details (title, priority, risk, description via TipTap, category); Section 2: Impact Analysis (affected systems, scheduled dates); Section 3: Technical Documentation (file upload) |
| CR-016  | Build Step 2 of CR creation: Approvers selection                                 | High     | 🔴 Not Started | Developer 2 | Pre-populated default approvers; add ad-hoc approvers; drag to reorder; approval type toggle                                                                                                                |
| CR-017  | Integrate TipTap rich text editor for CR description                             | High     | 🔴 Not Started | Developer 2 | Extensions: StarterKit, Link, Image, Placeholder                                                                                                                                                            |
| CR-018  | Build CR Detail page — Details tab (`audita_cr_detail_1/`)                       | High     | 🔴 Not Started | Developer 2 | Header: title, status badge, priority, risk; Approve/Reject buttons; maintenance window panel; risk assessment panel; affected infrastructure                                                               |
| CR-019  | Build CR Detail page — Approvers tab (`audita_cr_detail_2/`)                     | High     | 🔴 Not Started | Developer 2 | Approver list with status; add/remove approver; reorder drag-and-drop; required/optional toggle                                                                                                             |
| CR-020  | Build CR Detail page — Activity Stream tab                                       | High     | 🔴 Not Started | Developer 2 | Immutable, chronological event list; formatted action type labels                                                                                                                                           |
| CR-021  | Implement file upload component (drag-and-drop + browse)                         | High     | 🔴 Not Started | Developer 2 | Multipart POST to `/attachments`; show upload progress; list uploaded files                                                                                                                                 |

---

## Sprint 4: Comments, Notifications & SLA (Week 9–10)

> **Goal:** Full collaboration loop: comments with mentions, real-time notifications, SLA enforcement. After this sprint the MVP loop is complete.

### Backend — Comments

| Task ID | Task                                                                      | Priority | Status         | Assigned To | Notes                                                        |
| ------- | ------------------------------------------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------ |
| COM-001 | Implement comment creation endpoint                                       | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/change-requests/{id}/comments`; COM-01, COM-04 |
| COM-002 | Implement comment list endpoint                                           | High     | 🔴 Not Started | Developer 1 | `GET /api/v1/change-requests/{id}/comments`                  |
| COM-003 | Implement @mention extraction and storage (`comment_mentions`)            | High     | 🔴 Not Started | Developer 1 | COM-02, COM-03: notify mentioned users                       |
| COM-004 | Implement server-side HTML sanitisation of comment body (OWASP sanitizer) | High     | 🔴 Not Started | Developer 1 | SEC-06; applied before persistence                           |
| COM-005 | Implement file attachment in comments                                     | Medium   | 🔴 Not Started | Developer 1 | COM-05; reuse file storage module                            |

### Backend — Notifications

| Task ID   | Task                                                                           | Priority | Status         | Assigned To | Notes                                                                              |
| --------- | ------------------------------------------------------------------------------ | -------- | -------------- | ----------- | ---------------------------------------------------------------------------------- |
| NOTIF-001 | Implement SSE endpoint for per-user notification stream                        | High     | 🔴 Not Started | Developer 1 | `GET /api/v1/notifications/stream`; NOTIF-01; Java virtual threads for concurrency |
| NOTIF-002 | Implement notification persistence and replay on SSE reconnect                 | High     | 🔴 Not Started | Developer 1 | NOTIF-02: store in `notifications` table; replay unread on connect                 |
| NOTIF-003 | Implement `X-Unread-Count` response header on all authenticated API responses  | Medium   | 🔴 Not Started | Developer 1 | NOTIF-03                                                                           |
| NOTIF-004 | Implement notification CRUD: list, mark read, mark all read                    | High     | 🔴 Not Started | Developer 1 | `GET/PATCH /api/v1/notifications`                                                  |
| NOTIF-005 | Implement Thymeleaf email templates for all notification events                | High     | 🔴 Not Started | Developer 1 | NOTIF-04: all events in USER_FLOW §10 notification matrix                          |
| NOTIF-006 | Implement SMTP email dispatch (async, retry up to 3x with exponential backoff) | High     | 🔴 Not Started | Developer 1 | NOTIF-05, NOTIF-06; SMTP settings from `org_settings`                              |
| NOTIF-007 | Write notification dispatch integration tests                                  | High     | 🔴 Not Started | Developer 1 | Test all notification trigger events                                               |

### Backend — SLA Engine

| Task ID | Task                                                       | Priority | Status         | Assigned To | Notes                                                                                     |
| ------- | ---------------------------------------------------------- | -------- | -------------- | ----------- | ----------------------------------------------------------------------------------------- |
| SLA-001 | Implement SLA deadline computation at CR submission        | High     | 🔴 Not Started | Developer 1 | CR-08: match CR priority to `sla_policies`; set `sla_deadline` on CR                      |
| SLA-002 | Implement scheduled SLA evaluation job (every 5 min)       | High     | 🔴 Not Started | Developer 1 | CR-09: scan CRs where `sla_deadline` passed and `sla_breached = FALSE`; update and notify |
| SLA-003 | Implement SLA warning notification (X hours before breach) | Medium   | 🔴 Not Started | Developer 1 | `warning_before_hours` from policy; write `SLA_WARNING` to activity stream                |
| SLA-004 | Implement SLA breach notification + escalation contacts    | High     | 🔴 Not Started | Developer 1 | `SLA_BREACHED` activity entry; email escalation contacts                                  |

### Frontend — Comments & Notifications

| Task ID   | Task                                                                                  | Priority | Status         | Assigned To | Notes                                                                                   |
| --------- | ------------------------------------------------------------------------------------- | -------- | -------------- | ----------- | --------------------------------------------------------------------------------------- |
| COM-006   | Build CR Detail — Comments tab                                                        | High     | 🔴 Not Started | Developer 2 | TipTap editor with @mention autocomplete; file attachment; comment thread list          |
| COM-007   | Implement @mention user autocomplete in TipTap                                        | High     | 🔴 Not Started | Developer 2 | Query `GET /api/v1/users?search=` on each `@` keystroke; COM-02                         |
| NOTIF-008 | Build notification bell + badge in header                                             | High     | 🔴 Not Started | Developer 2 | Badge shows unread count from `X-Unread-Count` header                                   |
| NOTIF-009 | Build notification feed panel/drawer                                                  | High     | 🔴 Not Started | Developer 2 | List of notifications with read/unread state; mark read on click; deep link to CR       |
| NOTIF-010 | Implement `plugins/sse.ts` — SSE connection with auto-reconnect                       | High     | 🔴 Not Started | Developer 2 | Connects to `/api/v1/notifications/stream`; dispatches events to `useNotificationStore` |
| NOTIF-011 | Implement `useNotificationStore` (Pinia) — notification list, unread count, SSE state | High     | 🔴 Not Started | Developer 2 | —                                                                                       |

---

## Sprint 5: Admin Configuration Panel (Week 11–12)

> **Goal:** Admins can fully configure the organisation. Custom fields appear on CR forms. SLA policies are manageable. Organisation setup checklist works for first-time Admins.

### Backend — Admin Settings

| Task ID   | Task                                                                                          | Priority | Status         | Assigned To | Notes                                                                                                     |
| --------- | --------------------------------------------------------------------------------------------- | -------- | -------------- | ----------- | --------------------------------------------------------------------------------------------------------- |
| ADMIN-001 | Implement org settings CRUD (`GET/PUT /api/v1/settings`)                                      | High     | 🔴 Not Started | Developer 1 | Approval type, file upload config, SMTP settings, timezone, logo                                          |
| ADMIN-002 | Implement default approvers CRUD (`GET/POST/DELETE/PATCH /api/v1/settings/default-approvers`) | High     | 🔴 Not Started | Developer 1 | Required/Optional; position ordering                                                                      |
| ADMIN-003 | Implement custom field definitions CRUD                                                       | High     | 🔴 Not Started | Developer 1 | `GET/POST/PUT/DELETE /api/v1/settings/custom-fields`; field types: TEXT, NUMBER, DATE, DROPDOWN, CHECKBOX |
| ADMIN-004 | Implement SLA policy CRUD                                                                     | High     | 🔴 Not Started | Developer 1 | `GET/POST/PUT/DELETE /api/v1/settings/sla-policies`; escalation contacts (user multi-select)              |
| ADMIN-005 | Implement SMTP settings storage (encrypted)                                                   | High     | 🔴 Not Started | Developer 1 | AES-256 encrypt SMTP password at rest; SEC-08                                                             |
| ADMIN-006 | Implement file upload settings (max size, allowed MIME types, storage backend)                | Medium   | 🔴 Not Started | Developer 1 | FILE-01 → FILE-04                                                                                         |

### Frontend — Admin Settings Pages

| Task ID   | Task                                                                                | Priority | Status         | Assigned To | Notes                                                                                     |
| --------- | ----------------------------------------------------------------------------------- | -------- | -------------- | ----------- | ----------------------------------------------------------------------------------------- |
| ADMIN-007 | Build Organization Settings page — General tab                                      | High     | 🔴 Not Started | Developer 2 | Org name, logo, timezone; save button                                                     |
| ADMIN-008 | Build Organization Settings page — Workflow tab (`audita_organization_settings/`)   | High     | 🔴 Not Started | Developer 2 | Approval type toggle (Linear / Non-linear); default approvers list                        |
| ADMIN-009 | Build Organization Settings page — Custom Fields tab                                | High     | 🔴 Not Started | Developer 2 | Field list; add/edit/delete; field type selector; required toggle; display order          |
| ADMIN-010 | Build SLA Policies page (`audita_sla_policies/`)                                    | High     | 🔴 Not Started | Developer 2 | Policy table; create/edit/delete; escalation contacts; policy execution timeline diagram  |
| ADMIN-011 | Build Admin setup checklist / onboarding page (`audita_clarity_system_light_mode/`) | Medium   | 🔴 Not Started | Developer 2 | 8-step checklist per USER_FLOW §5.1; visible to Admin on first login                      |
| ADMIN-012 | Inject admin-configured custom fields into CR creation form                         | High     | 🔴 Not Started | Developer 2 | Fetch custom field definitions from `useSettingsStore`; render appropriate input per type |

---

## Sprint 6: Audit Trail & File Storage (Week 13–14)

> **Goal:** Global audit trail is complete, searchable, and exportable. File storage works for both local and S3. Audit log is cryptographically immutable in practice.

### Backend — Audit Trail

| Task ID   | Task                                                                            | Priority | Status         | Assigned To | Notes                                                                                               |
| --------- | ------------------------------------------------------------------------------- | -------- | -------------- | ----------- | --------------------------------------------------------------------------------------------------- |
| AUDIT-001 | Implement global audit log list endpoint with filtering                         | High     | 🔴 Not Started | Developer 1 | `GET /api/v1/audit-log`; filter by actor, action type, entity type, entity ID, date range; AUDIT-05 |
| AUDIT-002 | Implement CSV export of filtered audit log                                      | Medium   | 🔴 Not Started | Developer 1 | `GET /api/v1/audit-log/export`; AUDIT-06; streaming response                                        |
| AUDIT-003 | Verify audit log immutability (deny UPDATE/DELETE at app service account level) | High     | 🔴 Not Started | Developer 1 | AUDIT-02: DB role grants; tested via integration tests attempting mutations                         |
| AUDIT-004 | Verify actor email is denormalised on every audit_log entry                     | High     | 🔴 Not Started | Developer 1 | AUDIT-07: even if user is later deleted                                                             |
| AUDIT-005 | Write audit trail completeness tests                                            | High     | 🔴 Not Started | Developer 1 | Test every action type listed in SRS §3.3 produces an entry                                         |

### Backend — File Storage

| Task ID  | Task                                                                                                   | Priority | Status         | Assigned To | Notes                                                                            |
| -------- | ------------------------------------------------------------------------------------------------------ | -------- | -------------- | ----------- | -------------------------------------------------------------------------------- |
| FILE-001 | Implement `FileStorageService` interface with `LocalFileSystemStorage` and `S3Storage` implementations | High     | 🔴 Not Started | Developer 1 | FILE-01, FILE-02, FILE-03; strategy selected from `org_settings.storage_backend` |
| FILE-002 | Implement file upload endpoint with MIME + size validation                                             | High     | 🔴 Not Started | Developer 1 | `POST /api/v1/change-requests/{id}/attachments`; FILE-04; SEC-07                 |
| FILE-003 | Implement file download — authenticated; pre-signed URLs for S3                                        | High     | 🔴 Not Started | Developer 1 | FILE-05                                                                          |
| FILE-004 | Implement file delete endpoint                                                                         | Medium   | 🔴 Not Started | Developer 1 | `DELETE /api/v1/change-requests/{id}/attachments/{attachmentId}`                 |
| FILE-005 | Implement virus scanning hook (pluggable, no scanner in v1)                                            | Low      | 🔴 Not Started | Developer 1 | FILE-06: interface only; no-op default implementation                            |

### Frontend — Audit Trail

| Task ID   | Task                                           | Priority | Status         | Assigned To | Notes                                                                                    |
| --------- | ---------------------------------------------- | -------- | -------------- | ----------- | ---------------------------------------------------------------------------------------- |
| AUDIT-006 | Build Audit Trail page (`audita_audit_trail/`) | High     | 🔴 Not Started | Developer 2 | Filterable table: timestamp, actor, action type badge, entity type, entity ID; paginated |
| AUDIT-007 | Implement Export to CSV button                 | Medium   | 🔴 Not Started | Developer 2 | Triggers `GET /api/v1/audit-log/export`; streams download                                |
| AUDIT-008 | Build per-CR Activity Stream tab display       | High     | 🔴 Not Started | Developer 2 | Already scaffolded in Sprint 3; wire up real data; diff display for `CR_FIELD_UPDATED`   |

---

## Sprint 7: Security Hardening, Performance & Production Readiness (Week 15–16)

> **Goal:** Application is ready for self-hosted production deployment. Security requirements are fully met. Performance targets are validated. Docker Compose and Helm chart are production-grade.

### Security

| Task ID | Task                                                                       | Priority | Status         | Assigned To | Notes                                                                         |
| ------- | -------------------------------------------------------------------------- | -------- | -------------- | ----------- | ----------------------------------------------------------------------------- |
| SEC-001 | Implement HTTPS redirect (HTTP → HTTPS)                                    | High     | 🔴 Not Started | Developer 1 | SEC-02                                                                        |
| SEC-002 | Audit all API endpoints for missing `@PreAuthorize`                        | High     | 🔴 Not Started | Developer 1 | SEC-01: only public endpoints are login, forgot-pwd, reset-pwd, accept-invite |
| SEC-003 | Implement rate limiting filter for login, forgot-password, accept-invite   | High     | 🔴 Not Started | Developer 1 | SEC-09; Bucket4j or Spring Rate Limiter                                       |
| SEC-004 | Validate actor IP is recorded on all audit_log entries                     | High     | 🔴 Not Started | Developer 1 | SEC-11                                                                        |
| SEC-005 | Penetration test checklist: SQL injection, XSS, path traversal, CSRF, IDOR | High     | 🔴 Not Started | Developer 1 | Run OWASP ZAP baseline scan; fix all findings                                 |
| SEC-006 | Implement CSP, HSTS, X-Frame-Options, SRI headers on frontend              | High     | 🔴 Not Started | Developer 2 | AGENTS.md security baseline                                                   |
| SEC-007 | Review all Thymeleaf templates for `th:utext` → `th:text` substitution     | High     | 🔴 Not Started | Developer 1 | Prevent template injection in email templates                                 |

### Performance

| Task ID  | Task                                                                                  | Priority | Status         | Assigned To | Notes                          |
| -------- | ------------------------------------------------------------------------------------- | -------- | -------------- | ----------- | ------------------------------ |
| PERF-001 | Add DB indexes on `change_requests`: `status`, `created_by`, `created_at`, `priority` | High     | 🔴 Not Started | Developer 1 | SRS §7.1; via Flyway migration |
| PERF-002 | Load test CR list endpoint with 10k records per tenant                                | High     | 🔴 Not Started | Developer 1 | Target: p95 < 300ms            |
| PERF-003 | Configure Brotli/Gzip compression on Nuxt build output                                | Medium   | 🔴 Not Started | Developer 2 | AGENTS.md performance baseline |
| PERF-004 | Enable code splitting and tree-shaking in Nuxt build                                  | Medium   | 🔴 Not Started | Developer 2 | Production build verification  |
| PERF-005 | Validate Flyway parallel tenant migration on startup (avoid linear startup cost)      | Medium   | 🔴 Not Started | Developer 1 | Run migrations concurrently    |

### Production Deployment

| Task ID    | Task                                                                                 | Priority | Status         | Assigned To | Notes                                                                 |
| ---------- | ------------------------------------------------------------------------------------ | -------- | -------------- | ----------- | --------------------------------------------------------------------- |
| DEPLOY-001 | Finalise Docker Compose for production self-hosted use                               | High     | 🔴 Not Started | Developer 1 | Health checks, restart policies, volume mounts, env var documentation |
| DEPLOY-002 | Write Helm chart: API Deployment + Service + HPA; Web Deployment + Service + Ingress | High     | 🔴 Not Started | Developer 1 | SRS §10.2; TLS via cert-manager                                       |
| DEPLOY-003 | Document all required environment variables with descriptions                        | High     | 🔴 Not Started | Developer 1 | Based on SRS §8; `README.md` or `docs/deployment.md`                  |
| DEPLOY-004 | Configure Spring Boot Actuator readiness/liveness probes                             | Medium   | 🔴 Not Started | Developer 1 | For K8s deployment health checks                                      |
| DEPLOY-005 | Write end-to-end Playwright tests for critical paths                                 | High     | 🔴 Not Started | Developer 2 | Login → Create CR → Submit → Approve → Verify Approved status         |

### Accessibility & Polish

| Task ID    | Task                                                       | Priority | Status         | Assigned To | Notes                                                                           |
| ---------- | ---------------------------------------------------------- | -------- | -------------- | ----------- | ------------------------------------------------------------------------------- |
| A11Y-001   | Audit all interactive components for keyboard navigability | Medium   | 🔴 Not Started | Developer 2 | WCAG 2.1 AA                                                                     |
| A11Y-002   | Add ARIA labels to all icon-only buttons                   | Medium   | 🔴 Not Started | Developer 2 | Bell icon, action icons in tables, sidebar icons                                |
| A11Y-003   | Verify colour contrast ratios meet 4.5:1 for body text     | Medium   | 🔴 Not Started | Developer 2 | Use browser DevTools accessibility panel                                        |
| POLISH-001 | Implement dark mode toggle in user menu                    | Medium   | 🔴 Not Started | Developer 2 | Persist preference in localStorage                                              |
| POLISH-002 | Implement global search (header search bar)                | Low      | 🔴 Not Started | Developer 2 | Search CRs, users, audit entries; out of scope for MVP but designed into the UI |

---

## Progress Tracking

### Overall Progress by Sprint

| Sprint                          | Total Tasks | Not Started | In Progress | Completed | Progress % |
| ------------------------------- | ----------- | ----------- | ----------- | --------- | ---------- |
| Sprint 0: Foundation            | 19          | 0           | 0           | 19        | 100%       |
| Sprint 1: Authentication        | 22          | 22          | 0           | 0         | 0%         |
| Sprint 2: Multi-tenancy & Users | 21          | 21          | 0           | 0         | 0%         |
| Sprint 3: Change Request Core   | 21          | 21          | 0           | 0         | 0%         |
| Sprint 4: Comments, Notif & SLA | 18          | 18          | 0           | 0         | 0%         |
| Sprint 5: Admin Config          | 12          | 12          | 0           | 0         | 0%         |
| Sprint 6: Audit Trail & Files   | 13          | 13          | 0           | 0         | 0%         |
| Sprint 7: Security & Production | 20          | 20          | 0           | 0         | 0%         |
| **TOTAL**                       | **146**     | **127**     | **0**       | **19**    | **13%**    |

### Progress by Developer

| Developer              | Assigned Tasks | Not Started | In Progress | Completed | Progress % |
| ---------------------- | -------------- | ----------- | ----------- | --------- | ---------- |
| Developer 1 (Backend)  | 80             | 80          | 0           | 0         | 0%         |
| Developer 2 (Frontend) | 66             | 66          | 0           | 0         | 0%         |

---

## MVP Definition

The **Minimum Viable Product** is reached at the end of **Sprint 4**, when:

1. ✅ Multi-tenant organisation provisioning works (Super Admin)
2. ✅ Users can authenticate (local + SSO)
3. ✅ Admins can invite users and assign roles
4. ✅ Requesters can create, submit, and manage change requests
5. ✅ Approvers can approve/reject with required rejection reasons
6. ✅ Linear and Non-linear approval workflows function correctly
7. ✅ Comments with @mentions work
8. ✅ Real-time in-app notifications via SSE
9. ✅ SLA deadlines computed and breach notifications sent
10. ✅ Immutable activity stream per CR

Sprints 5–7 deliver: Admin configuration panel, full audit trail, file storage backend options, and production-grade hardening.

---

## Recent Implementations

### Sprint 0: Foundation & Scaffolding (Completed 2026-04-23)

**Overview**: Both repositories scaffolded and fully connected — runnable via Docker Compose.

**Files Created/Modified (audita-api)**:

- `settings.gradle.kts`, `build.gradle.kts` — Gradle 8 multi-module root config
- `domain/build.gradle.kts` + domain models (enums, exceptions) — pure domain layer
- `application/build.gradle.kts` + `AuthService`, `EmailService` — application services
- `infrastructure/build.gradle.kts` + all JPA entities (Tenant, SuperAdmin, User, Role, Permission, ChangeRequest, CrApprover, Comment, ActivityStream, AuditLog, Notification, RefreshToken, PasswordResetToken, InviteToken)
- Infrastructure repositories (Spring Data JPA) for all entities
- `TenantContext`, `AuditaTenantIdentifierResolver`, `AuditaMultiTenantConnectionProvider`, `FlywayTenantMigrator`
- `JpaConfig` — Hibernate multi-tenant schema wiring
- `JwtService`, `AesEncryptionService`, `HtmlSanitizer`
- `SecurityConfig`, `JwtAuthenticationFilter`, `UserPrincipal`, `CurrentUser`
- `GlobalExceptionHandler` (RFC 7807 Problem Detail)
- `AuthController`, `PlatformBootstrapController` — auth endpoints
- Auth DTOs (request/response records)
- `application.yml` — full configuration with env var defaults
- Flyway migrations: `V1__create_public_schema.sql`, `V1__create_tenant_schema.sql`, `V2__seed_roles_and_permissions.sql`
- Thymeleaf email templates: password-reset, invite, approval-request, approval-decision, mention, sla-breach
- `Dockerfile` — two-stage build (JDK 21 builder, JRE runtime)
- `.gitignore`

**Files Created/Modified (audita-web)**:

- `package.json`, `nuxt.config.ts`, `tailwind.config.ts` — project config
- `assets/css/main.css` — full design system (btn, card, input, badge, sidebar, table classes)
- `types/index.ts` — all TypeScript interfaces (User, ChangeRequest, Notification, AuditLog, etc.)
- `stores/auth.ts`, `stores/notifications.ts` — Pinia stores
- `plugins/api.ts` — `$fetch` wrapper with auth injection + silent refresh
- `plugins/sse.client.ts` — SSE connection with auto-reconnect
- `middleware/auth.ts`, `middleware/role.ts` — route guards
- `composables/useApi.ts`, `composables/useAuth.ts`, `composables/useChangeRequests.ts`, `composables/useToast.ts`
- `layouts/auth.vue`, `layouts/default.vue`, `layouts/platform.vue`
- `components/shared/AppSidebar.vue`, `AppUserMenu.vue`, `AppNotificationBell.vue`, `AppToastContainer.vue`
- `components/cr/CrStatusBadge.vue`, `CrPriorityBadge.vue`
- `pages/index.vue` (root redirect), `pages/auth/sign-in.vue`, `pages/auth/forgot-password.vue`, `pages/auth/accept-invite.vue`
- `pages/dashboard/index.vue`, `pages/change-requests/index.vue`
- `Dockerfile`, `.gitignore`

**Root**:

- `docker-compose.yml` — PostgreSQL 16, MailHog, API, Web services with health checks

**Test Coverage**: 0 tests yet — Sprint 1 will add auth integration tests
