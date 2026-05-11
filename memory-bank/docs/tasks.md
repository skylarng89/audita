# Audita — Developer Task List

**Project:** Audita — Multi-Tenant ITIL/ITSM Change Management Platform
**Version:** 0.1.0
**Last Updated:** 2026-05-11
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

## Sprint 1: Authentication & Platform Bootstrap (Week 3–4)

> **Goal:** Full auth stack end-to-end. Users can sign in, refresh tokens, reset passwords, and authenticate via SSO. Super Admin bootstrap flow complete.

### Backend — Auth Service (`audita-api`)

| Task ID  | Task                                                                            | Priority | Status       | Assigned To | Notes                                                                                              |
| -------- | ------------------------------------------------------------------------------- | -------- | ------------ | ----------- | -------------------------------------------------------------------------------------------------- |
| AUTH-001 | Implement platform bootstrap endpoint (first Super Admin creation)              | High     | ✅ Completed | Developer 1 | `PlatformBootstrapController` + `AuthService.bootstrap()`; `POST /api/platform/v1/bootstrap`       |
| AUTH-002 | Implement tenant user login with BCrypt password verification                   | High     | ✅ Completed | Developer 1 | `AuthService.loginTenantUser()`; BCrypt cost=12; `AuthController`; `POST /api/v1/auth/login`       |
| AUTH-003 | Implement JWT access token generation (15-min expiry)                           | High     | ✅ Completed | Developer 1 | `JwtService`; jjwt 0.12.6; claims: userId, tenantSlug, role, email                                 |
| AUTH-004 | Implement refresh token rotation (7-day, HttpOnly cookie, SHA-256 hashed in DB) | High     | ✅ Completed | Developer 1 | `AuthService.refreshToken()`; `POST /api/v1/auth/refresh`; rotating + cookie                       |
| AUTH-005 | Implement logout (revoke refresh token)                                         | Medium   | ✅ Completed | Developer 1 | `AuthService.logout()`; `POST /api/v1/auth/logout`; deletes cookie                                 |
| AUTH-006 | Implement forgot-password email flow with rate limiting (3/hr/email)            | High     | ✅ Completed | Developer 1 | `AuthService.forgotPassword()`; `EmailService`; `POST /api/v1/auth/forgot-password`                |
| AUTH-007 | Implement reset-password token validation and password update                   | High     | ✅ Completed | Developer 1 | `AuthService.resetPassword()`; `POST /api/v1/auth/reset-password`; token expiry + used flag        |
| AUTH-008 | Implement JWT authentication filter (`JwtAuthenticationFilter`)                 | High     | ✅ Completed | Developer 1 | Extends `OncePerRequestFilter`; validates JWT; populates `SecurityContextHolder`                   |
| AUTH-009 | Implement tenant domain whitelist check on login                                | High     | ✅ Completed | Developer 1 | `AuthService.checkDomainWhitelist()`; open if no domains; `DomainNotPermittedException`            |
| AUTH-010 | Write unit + integration tests for all auth flows                               | High     | ✅ Completed | Developer 1 | 18 unit tests; login, logout, refresh, forgot/reset, domain block, rate limits, bootstrap          |
| AUTH-011 | Implement Google OIDC SSO initiation + callback                                 | High     | ✅ Completed | Developer 1 | `SsoController` + `SsoService.buildAuthorizationUrl()` + callback; `GET /api/v1/auth/oauth/google` |
| AUTH-012 | Implement Microsoft Azure AD SSO initiation + callback                          | High     | ✅ Completed | Developer 1 | Same pattern as Google; `GET /api/v1/auth/oauth/microsoft`                                         |
| AUTH-013 | Implement JIT user provisioning on first SSO login                              | High     | ✅ Completed | Developer 1 | `SsoService.resolveOrProvisionUser()`; creates `UserEntity` if not found by email                  |
| AUTH-014 | Implement OAuth account linking (provider + sub → existing user)                | High     | ✅ Completed | Developer 1 | `OAuthAccountEntity` lookup by provider+sub before email fallback                                  |
| AUTH-015 | Encrypt SSO client secrets at rest with AES-256-GCM                             | High     | ✅ Completed | Developer 1 | `AesEncryptionService`; `audita.encryption.key` (64 hex); applied in `SsoService`                  |

### Frontend — Auth Pages (`audita-web`)

| Task ID  | Task                                                                                 | Priority | Status       | Assigned To | Notes                                                                                          |
| -------- | ------------------------------------------------------------------------------------ | -------- | ------------ | ----------- | ---------------------------------------------------------------------------------------------- |
| AUTH-016 | Build Sign In page (`/auth/sign-in`)                                                 | High     | ✅ Completed | Developer 2 | Email + password; tenant slug; SSO buttons; calls `useAuth().login()`                          |
| AUTH-017 | Build Forgot Password page (`/auth/forgot-password`)                                 | Medium   | ✅ Completed | Developer 2 | Email field; success state; calls `useAuth().forgotPassword()`                                 |
| AUTH-018 | Build Reset Password page (`/auth/reset-password`)                                   | Medium   | ✅ Completed | Developer 2 | Reads `?token=`; password + confirm; 4-segment strength bar; calls `useAuth().resetPassword()` |
| AUTH-019 | Build Accept Invite page (`/auth/accept-invite`)                                     | High     | ✅ Completed | Developer 2 | Reads `?token=`; full name + password; calls `useAuth().acceptInvite()`                        |
| AUTH-020 | Build Bootstrap/first-run page (`/platform/bootstrap`)                               | High     | ✅ Completed | Developer 2 | Full name + email + password; `POST /api/platform/v1/bootstrap`; success shows sign-in link    |
| AUTH-021 | Build `useAuthStore` Pinia store                                                     | High     | ✅ Completed | Developer 2 | `stores/auth.ts`; accessToken, user, tenantSlug; `setAuth()`, `clearAuth()`                    |
| AUTH-022 | Implement role-based redirect after login (SUPER_ADMIN → /platform, else /dashboard) | High     | ✅ Completed | Developer 2 | `sso-callback.vue` + `useAuth` redirect logic                                                  |

---

## Sprint 4: Collaboration, Notifications & SLA Automation (Week 9–10)

> **Goal:** Deliver rich CR collaboration, real-time in-app notifications, and automatic SLA warning/breach handling.

### Backend — Collaboration & Realtime (`audita-api`)

| Task ID   | Task                                                                  | Priority | Status       | Assigned To | Notes                                                                                                |
| --------- | --------------------------------------------------------------------- | -------- | ------------ | ----------- | ---------------------------------------------------------------------------------------------------- |
| CR-022    | Implement comments API (list + create)                                | High     | ✅ Completed | Developer 1 | Added `CommentController` + `CommentService`; wired CR comments endpoints                            |
| CR-023    | Implement mention extraction and mention notifications                | High     | ✅ Completed | Developer 1 | Added mention regex extraction, `comment_mentions` persistence, in-app + email mention notifications |
| NOTIF-001 | Implement notifications query endpoint with unread count support      | High     | ✅ Completed | Developer 1 | Added `GET /api/v1/notifications` with `X-Unread-Count` header                                       |
| NOTIF-002 | Implement notification read and read-all endpoints                    | Medium   | ✅ Completed | Developer 1 | Added `PATCH /api/v1/notifications/{id}/read` and `POST /api/v1/notifications/read-all`              |
| NOTIF-003 | Implement SSE notifications stream endpoint                           | High     | ✅ Completed | Developer 1 | Added `GET /api/v1/notifications/stream` + emitter registry service                                  |
| SLA-001   | Implement scheduled SLA warning/breach evaluator                      | High     | ✅ Completed | Developer 1 | Added `SlaMonitoringService` scheduled evaluator using warning/breach queries                        |
| SLA-002   | Persist SLA activity + notification events and send SLA breach emails | High     | ✅ Completed | Developer 1 | Added SLA warning/breach activity events and breach email fan-out                                    |

### Frontend — Collaboration & Realtime (`audita-web`)

| Task ID   | Task                                                    | Priority | Status       | Assigned To | Notes                                                                |
| --------- | ------------------------------------------------------- | -------- | ------------ | ----------- | -------------------------------------------------------------------- |
| CR-024    | Add Comments tab to CR detail page with list + compose  | High     | ✅ Completed | Developer 2 | Added comments tab in CR detail with render + compose + post action  |
| NOTIF-004 | Wire notification bell to backend list endpoint on load | Medium   | ✅ Completed | Developer 2 | Added startup hydration in `AppNotificationBell`                     |
| NOTIF-005 | Stabilise SSE plugin auth/connection behavior           | Medium   | ✅ Completed | Developer 2 | Hardened SSE with short-lived stream-token issuance + reconnect flow |

---

## Sprint 5: Hardening & Release Readiness (Week 11)

> **Goal:** Stabilize critical integration flows, lock in regression coverage, and complete final verification gates.

### Backend + Frontend Hardening

| Task ID  | Task                                                                          | Priority | Status       | Assigned To | Notes                                                                                                                                     |
| -------- | ----------------------------------------------------------------------------- | -------- | ------------ | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| HARD-001 | Resolve critical CR lifecycle integration regressions                         | High     | ✅ Completed | Developer 1 | `CriticalFlowsE2EL1Test` now passes all 4 flows after service + compatibility fixes                                                       |
| HARD-002 | Add principal identity regression tests                                       | High     | ✅ Completed | Developer 1 | Added `UserPrincipalTest` to lock UUID username semantics                                                                                 |
| HARD-003 | Harden CR response mapping against lazy-loading runtime failures              | High     | ✅ Completed | Developer 1 | Added service-layer creator initialization before returning mapped entities                                                               |
| HARD-004 | Run backend release gate (critical suite)                                     | High     | ✅ Completed | Developer 1 | `./gradlew :api:test --tests "io.audita.api.integration.CriticalFlowsE2EL1Test"`                                                          |
| HARD-005 | Run frontend release gate (production build)                                  | High     | ✅ Completed | Developer 2 | `cd audita-web && pnpm build`                                                                                                             |
| ARCH-001 | Introduce application ports + controller inversion (auth/SSO/bootstrap slice) | High     | ✅ Completed | Developer 1 | Added `AuthPort`, `SsoPort`, `OnboardingPort`; rewired `AuthController`, `SsoController`, `PlatformBootstrapController`; compile verified |

---

## Sprint 7: File Security, Custom Fields UX & CR Edit Mode (2026-05-11)

> **Goal:** Harden file uploads with multi-layer type enforcement, fix custom fields 400 bug, give admins a dedicated custom fields configuration page, and redesign the CR detail page to be read-only by default with a gated Edit mode.

### Backend — File Security & Validation (`audita-api`)

| Task ID | Task                                            | Priority | Status       | Assigned To | Notes                                                                                                                                                             |
| ------- | ----------------------------------------------- | -------- | ------------ | ----------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| SEC-010 | Implement 3-layer file upload type enforcement  | High     | ✅ Completed | Developer 1 | `isAllowedMimeType()` + `isExtensionAllowed()` + `isSignatureValid()` (magic bytes) in `ChangeRequestService`; DOCX/XLSX ZIP ambiguity handled by extension check |
| SEC-011 | Add path traversal guard on attachment download | High     | ✅ Completed | Developer 1 | `outputPath.normalize()` + `outputPath.startsWith(storageDir)` in `ChangeRequestService`                                                                          |
| UX-001  | Implement filename normalization on upload      | Medium   | ✅ Completed | Developer 1 | `normalizeFileName()` → lowercase, hyphenated, filesystem-safe; original name preserved in DB                                                                     |

### Frontend — Custom Fields Admin Page (`audita-web`)

| Task ID | Task                                                                    | Priority | Status       | Assigned To | Notes                                                                                                         |
| ------- | ----------------------------------------------------------------------- | -------- | ------------ | ----------- | ------------------------------------------------------------------------------------------------------------- |
| ADM-010 | Create `/admin/custom-fields` page with full CRUD for field definitions | High     | ✅ Completed | Developer 2 | `pages/admin/custom-fields/index.vue`; TEXT/NUMBER/DATE/DROPDOWN/CHECKBOX; uses `/api/v1/admin/custom-fields` |
| ADM-011 | Add "Custom Fields" admin-only sidebar link                             | Low      | ✅ Completed | Developer 2 | `AppSidebar.vue`; `v-if="auth.isAdmin"`; placed before Settings link                                          |

### Frontend — CR Detail Edit Mode (`audita-web`)

| Task ID | Task                                                              | Priority | Status       | Assigned To | Notes                                                                                                                                                                           |
| ------- | ----------------------------------------------------------------- | -------- | ------------ | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| CR-030  | Redesign CR detail page to read-only default with gated Edit mode | High     | ✅ Completed | Developer 2 | Edit button visible only for DRAFT; form covers all CR fields + TipTap + custom fields; Save calls update() + saveCustomFields() atomically; `onBeforeUnmount` cleans up editor |
| CR-031  | Replace VueDatePicker with native date/time inputs in Create CR   | High     | ✅ Completed | Developer 2 | `new.vue`: 4 native inputs, `colorScheme` dark mode, `combineParts(string, string)`, state types `""` not null                                                                  |
| CR-032  | Replace VueDatePicker with native date/time inputs in CR Edit     | High     | ✅ Completed | Developer 2 | `[id].vue`: same as CR-031 + `enterEditMode` serialises `"HH:mm"` strings; plugin + CSS removed                                                                                 |

## Sprint 8: Admin Settings Activation & SLA Defaults (2026-05-11)

> **Goal:** Activate editable tenant admin settings for workflow/SLA defaults and wire runtime SLA behavior to persisted tenant-level settings.

### Backend + Frontend Settings Slice (`audita-api` + `audita-web`)

- SET-001 | Add persisted workflow/SLA defaults to tenant settings API | Priority: High | Status: ✅ Completed | Assigned To: Developer 1 | Notes: Added `PATCH /api/v1/settings`, `org_settings` persistence entity/repository, expanded response contract, and validation guard for warning/deadline ratio.
- SET-002 | Activate admin settings UI save flow for workflow/SLA defaults | Priority: High | Status: 🟡 In Progress | Assigned To: Developer 2 | Notes: `pages/admin/settings/index.vue` now editable and PATCH-wired; pending page interaction tests and UX polish feedback messages.
- SET-003 | Read SLA defaults at runtime in CR creation and SLA monitor | Priority: High | Status: ✅ Completed | Assigned To: Developer 1 | Notes: Added runtime tests in `ChangeRequestServiceSecurityTest` and `SlaMonitoringServiceTest` to verify configured SLA hours and warning window behavior.
- SET-004 | Add regression tests for tenant settings GET/PATCH | Priority: High | Status: ✅ Completed | Assigned To: Developer 1 | Notes: Added `TenantSettingsControllerWebMvcTest` and `TenantServiceSettingsTest` covering defaults, malformed persisted values, and settings write assertions.

---

## Progress Tracking

### Overall Progress by Sprint

| Sprint    | Total Tasks | Not Started | In Progress | Completed | Progress % |
| --------- | ----------- | ----------- | ----------- | --------- | ---------- |
| Sprint 0  | 19          | 0           | 0           | 19        | 100%       |
| Sprint 1  | 22          | 0           | 0           | 22        | 100%       |
| Sprint 2  | 19          | 0           | 0           | 19        | 100%       |
| Sprint 3  | 21          | 0           | 0           | 21        | 100%       |
| Sprint 4  | 10          | 0           | 0           | 10        | 100%       |
| Sprint 5  | 5           | 0           | 0           | 5         | 100%       |
| Sprint 7  | 8           | 0           | 0           | 8         | 100%       |
| Sprint 8  | 4           | 0           | 1           | 3         | 75%        |
| **TOTAL** | **108**     | **0**       | **1**       | **107**   | **99%**    |

---

## Recent Implementations

### Sprint 8 — Workflow/SLA Settings Activation (In Progress 2026-05-11)

**Overview**: Started Sprint 8 with a vertical settings slice: workflow and SLA defaults are now editable in Admin Settings, persisted in tenant scope, and consumed by SLA runtime calculations.

**Files Created/Modified**:

- `audita-api/application/src/main/java/io/audita/application/port/TenantSettingsPort.java` — added settings aggregate + workflow/sla records + update contracts
- `audita-api/api/src/main/java/io/audita/api/controller/TenantSettingsController.java` — added `PATCH /api/v1/settings` and workflow/sla mapping in GET
- `audita-api/api/src/main/java/io/audita/api/dto/request/PatchTenantAdminSettingsRequest.java` — request validation contract
- `audita-api/api/src/main/java/io/audita/api/dto/response/TenantAdminSettingsResponse.java` — workflow/sla defaults in response model
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/OrgSettingEntity.java` — `org_settings` entity mapping
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/OrgSettingRepository.java` — key-value settings repository
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/TenantService.java` — settings persistence/read implementation with safe defaults
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` — SLA deadline hours resolved from `org_settings`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SlaMonitoringService.java` — warning window resolved from `org_settings`
- `audita-api/api/src/test/java/io/audita/api/controller/TenantSettingsControllerWebMvcTest.java` — GET/PATCH + validation regression tests
- `audita-web/pages/admin/settings/index.vue` — editable workflow/sla controls + dirty-state + save flow

**Key Changes**:

- Admin settings are no longer read-only for workflow and SLA defaults.
- SLA warning threshold is validated against configured deadline windows.
- Runtime SLA calculations now honor tenant-configured values when present.

**Test Coverage**: `TenantSettingsControllerWebMvcTest` passes; backend compile passes; frontend `nuxi typecheck` passes.

**Additional Verification (2026-05-11 continuation)**: `ChangeRequestServiceSecurityTest` and `SlaMonitoringServiceTest` pass with new runtime SLA configuration assertions.

**Additional Verification (2026-05-11 continuation 2)**: `TenantServiceSettingsTest` passes with tenant settings default/malformed-value/write-path assertions.

### Sprint 7 — VueDatePicker Replaced with Native Inputs (Completed 2026-05-11)

**Overview**: Resolved persistent time-leaks-into-date-field bug by replacing VueDatePicker with native browser date/time inputs across both Create CR and CR Edit pages.

**Files Created/Modified**:

- `audita-web/pages/change-requests/new.vue` — 4 `<VueDatePicker>` → 4 `<input type="date/time">`; state `""` not null; `combineParts(string, string)`
- `audita-web/pages/change-requests/[id].vue` — same + `enterEditMode` time as `"HH:mm"` strings
- `audita-web/plugins/vue-datepicker.client.ts` — deleted
- `audita-web/assets/css/main.css` — `.dp__*` override block removed

**Key Changes**:

- Native `<input type="date">` and `<input type="time">` are architecturally incapable of mixing date/time concerns — VueDatePicker's internal `Date` model was the root cause.
- Dark mode handled via `:style="{ colorScheme: isDark ? 'dark' : 'light' }"`.
- `isDark` ref + MutationObserver kept in both pages for the new style binding.

**Test Coverage**: `pnpm exec vue-tsc --noEmit` exits 0. Docker rebuilt and deployed.

### Sprint 7 — File Security, Custom Fields UX & CR Edit Mode (Completed 2026-05-11)

**Overview**: Three-layer file upload enforcement, filename normalization, path traversal guard, admin custom fields dedicated page, and CR detail read-only/edit mode redesign.

**Files Created/Modified**:

- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` — `isAllowedMimeType()`, `isExtensionAllowed()`, `isSignatureValid()` (magic bytes), `normalizeFileName()`, path traversal guard on download
- `audita-api/.env` — `UPLOAD_ALLOWED_MIME_TYPES`, `UPLOAD_MAX_SIZE_BYTES`, `UPLOAD_MAX_SIZE`
- `audita-web/pages/admin/custom-fields/index.vue` — new: full CRUD for global custom field definitions
- `audita-web/components/shared/AppSidebar.vue` — admin-only "Custom Fields" NuxtLink added
- `audita-web/pages/change-requests/[id].vue` — read-only default, Edit mode toggle, TipTap editor integration, `onBeforeUnmount`, file pre-flight validation

**Key Changes**:

- Magic byte validation covers PDF (`%PDF-`), PNG (`89 50 4E 47`), JPEG (`FF D8 FF`), DOCX/XLSX (`50 4B 03 04`). DOCX/XLSX share ZIP magic bytes — disambiguated by file extension check.
- Filename normalization produces lowercase, hyphenated, filesystem-safe names; display name in DB is unchanged.
- Custom field definitions drive both the admin page and the CR edit form — no more add-row / empty-fieldId pattern.
- CR detail Edit button only appears for `DRAFT` status; workflow actions (Submit/Cancel/Approve/Reject) remain in read-only view.
- TipTap editor destroyed on component unmount to prevent memory leaks.

**Test Coverage**: `pnpm exec vue-tsc --noEmit` exits 0. Docker containers rebuilt and redeployed successfully.

### Frontend Tailwind v4 Migration (Completed 2026-05-04)

**Overview**: Migrated frontend styling/build integration to Tailwind v4 native Vite plugin path and completed compatibility fixes to keep CI build/test/typecheck green.

**Files Created/Modified**:

- `audita-web/nuxt.config.ts` — removed `@nuxtjs/tailwindcss`, added `@tailwindcss/vite`, and wired global CSS entry
- `audita-web/assets/css/main.css` — switched to v4 directives and refactored component-layer `@apply` usage for v4 compatibility
- `audita-web/package.json` — removed `@nuxtjs/tailwindcss`, added `@tailwindcss/vite`, pinned Tailwind v4 stack
- `audita-web/pnpm-lock.yaml` — lockfile refresh for Tailwind v4 dependency graph
- `audita-web/tailwind.config.js` — new v4-compatible config entrypoint used by CSS `@config`
- `audita-web/tailwind.config.ts` — removed legacy config entrypoint

**Key Changes**:

- Migrated from Nuxt Tailwind module integration to official Tailwind v4 Vite plugin integration.
- Removed invalid custom-class `@apply` chaining that Tailwind v4 rejects.
- Preserved existing design tokens during migration through compatibility-mode config.

**Test Coverage**: `cd audita-web && pnpm test && pnpm -s nuxi typecheck` passes; `cd audita-web && pnpm build` passes.

### Controller Decoupling + Nuxt Typecheck Stabilization (Completed 2026-05-04)

**Overview**: Completed requested follow-up refactor to remove remaining API-layer infrastructure coupling in tenant settings and resolved project-wide Nuxt typecheck failures after restoring missing tsconfig wiring.

**Files Created/Modified**:

- `audita-api/application/src/main/java/io/audita/application/port/TenantSettingsPort.java` — new application contract for tenant settings profile retrieval
- `audita-api/api/src/main/java/io/audita/api/controller/TenantSettingsController.java` — now consumes `TenantSettingsPort` and `UserPrincipal`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/TenantService.java` — implements `TenantSettingsPort`
- `audita-web/tsconfig.json` — restored Nuxt tsconfig extension for auto-import and alias typing
- `audita-web/composables/useApi.ts` — relaxed typed wrapper to prevent deep route-type recursion
- `audita-web/plugins/api.ts` — strict-safe request path and headers typing
- `audita-web/pages/admin/groups/index.vue` — pagination handler typing + query-based API calls + accessibility label wiring
- `audita-web/pages/admin/users/index.vue` — pagination handler typing + accessibility label wiring
- `audita-web/pages/platform/tenants/index.vue` — `useAsyncData` typing cleanup and query-based API calls
- `audita-web/pages/users/index.vue` — strict prop typing compatibility (`SharedAppTable`, `SharedAppModal`) and role literal alignment
- `audita-web/tests/middleware/auth.global.spec.ts` — middleware signature-compatible route argument typing
- `audita-web/package.json` + lockfile — added `tailwindcss` dev dependency for config type resolution

**Key Changes**:

- Removed remaining direct infrastructure type dependency from settings controller boundary.
- Recovered Nuxt global auto-import/type resolution and closed all blocking typecheck errors.
- Preserved existing UI behavior while improving strict type safety and accessibility metadata.

**Test Coverage**: `cd audita-api && ./gradlew :api:compileJava :infrastructure:compileJava --no-daemon` passes; `cd audita-web && pnpm -s nuxi typecheck` passes.

### Mock Data Removal — Backend + Frontend Wiring (Completed 2026-05-04)

**Overview**: Replaced remaining settings/dashboard/platform mock placeholders with live endpoint-driven data and fixed controller dependency boundaries that caused API compile leakage.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/controller/TenantSettingsController.java` — serves tenant admin settings payload at `/api/v1/settings`
- `audita-api/api/src/main/java/io/audita/api/controller/DashboardController.java` — now depends on `DashboardPort` for summary data
- `audita-api/api/src/main/java/io/audita/api/controller/PlatformHealthController.java` — now depends on `OnboardingPort` and returns live health summary
- `audita-api/application/src/main/java/io/audita/application/port/DashboardPort.java` — new application contract for dashboard aggregation
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/DashboardService.java` — repository-backed dashboard summary implementation
- `audita-web/pages/admin/settings/index.vue` — consumes `/api/v1/settings`, renders profile/flags/security defaults
- `audita-web/pages/dashboard/index.vue` — consumes `/api/v1/dashboard/summary` and `/api/v1/notifications`
- `audita-web/pages/platform/index.vue` — consumes `/api/platform/v1/health` for live system health card

**Key Changes**:

- Removed direct Spring Data repository usage from API controller boundary for dashboard summary.
- Eliminated hardcoded KPI and settings placeholders in the identified frontend pages.
- Added resilient loading/error fallbacks that only activate on fetch failure.

**Test Coverage**: `cd audita-api && ./gradlew :api:compileJava :infrastructure:compileJava --no-daemon` passes; `cd audita-web && pnpm -s eslint pages/admin/settings/index.vue pages/dashboard/index.vue pages/platform/index.vue` passes.

### Post-Sprint Runtime + UI Hardening (Completed 2026-04-29)

**Overview**: Resolved CR detail runtime failure after create, removed recurring authorization/rendering regressions, and completed a focused style consistency audit for CR pages.

**Files Created/Modified**:

- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` — initialize lazy creator relation in read paths (`list`, `getById`)
- `audita-api/api/src/main/java/io/audita/api/security/UserPrincipal.java` — normalize role authorities for Spring role checks
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SlaMonitoringService.java` — transaction boundary hardening for tenant SLA evaluation flow
- `audita-web/plugins/api.ts` — refresh fallback expanded for `403` responses
- `audita-web/layouts/default.vue` — corrected shared component tags (`SharedAppSidebar`, `SharedAppUserMenu`, `SharedAppNotificationBell`, `SharedAppToastContainer`)
- `audita-web/layouts/platform.vue` — corrected shared toast container tag
- `audita-web/assets/css/main.css` — default button variant sizing fallback
- `audita-web/pages/change-requests/new.vue` — normalized button classes
- `audita-web/pages/change-requests/index.vue` — normalized action button classes
- `audita-web/pages/change-requests/[id].vue` — normalized button/tab classes and rich description rendering

**Key Changes**:

- Eliminated `LazyInitializationException` on CR detail fetch after draft creation.
- Corrected authority normalization so valid users are not blocked by role-name mismatch.
- Restored missing shell UI elements by aligning Nuxt auto-import component naming.
- Standardized CR page button rendering and fixed display of rich-text description content.

**Test Coverage**: Backend compile and frontend production build passed; containerized API/web redeploy completed with healthy runtime.

---

### Post-Sprint UX Hardening — Bootstrap Browser 403 Resolution (Completed 2026-04-29)

**Overview**: Resolved browser-only platform bootstrap failures by fixing an internal proxy/CORS interaction in the Nuxt server route and validated end-to-end onboarding completion.

**Files Created/Modified**:

- `audita-web/server/routes/api/[...path].ts` — strip `Origin`/`Referer`/`Host` before upstream proxying
- `audita-web/plugins/api.ts` — bootstrap endpoint detection and anonymous header handling hardening
- `audita-web/pages/platform/bootstrap.vue` — bootstrap submit credentials omitted for anonymous first-run setup
- `audita-web/composables/useOnboarding.ts` — onboarding status request credentials omitted
- `audita-api/api/src/main/java/io/audita/api/security/JwtAuthenticationFilter.java` — bootstrap-path diagnostics
- `audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java` — bootstrap-path diagnostics
- `audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java` — bootstrap request diagnostics
- `audita-api/api/src/main/java/io/audita/api/exception/GlobalExceptionHandler.java` — 403 context diagnostics

**Key Changes**:

- Confirmed browser response body for failing submit was exactly `Invalid CORS request`.
- Confirmed API application-layer bootstrap POST logs were absent during failures, indicating pre-controller rejection.
- Implemented Nuxt proxy header stripping to prevent upstream CORS rejection for same-origin app calls.
- Verified browser bootstrap returns 200 success payload and onboarding status flips to `true`.

**Test Coverage**: Docker Compose rebuild + browser repro + CLI status verification completed; bootstrap browser path is now successful.

### Post-Sprint UX — First-Time Onboarding Gate (Completed 2026-04-28)

**Overview**: Added backend onboarding status tracking endpoint and frontend first-run navigation guards so fresh instances route directly to setup and completed instances skip setup.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java` — added `GET /api/platform/v1/bootstrap/status`
- `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java` — permit public access to onboarding status route
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java` — exposed onboarding completion check
- `audita-web/composables/useOnboarding.ts` — added status fetch composable
- `audita-web/pages/index.vue` — root redirects to bootstrap on first run, sign-in after onboarding complete
- `audita-web/pages/auth/sign-in.vue` — blocks sign-in page when onboarding is incomplete; shows setup-complete banner
- `audita-web/pages/platform/bootstrap.vue` — redirects away when onboarding already completed

**Key Changes**:

- Introduced explicit onboarding completion status contract: `{ onboardingCompleted: boolean }`.
- Centralized frontend status fetch in a dedicated composable to avoid duplicate endpoint wiring.
- Enforced one-time onboarding UX by checking status in root, sign-in, and bootstrap entry points.

**Test Coverage**: `./gradlew :infrastructure:test :api:test` passes and `cd audita-web && pnpm build` passes.

### Security Review — Adversarial Audit (Completed 2026-04-28)

**Overview**: Completed a full adversarial security review of backend/frontend authentication, multi-tenant isolation, authorization depth, token transport, and upload paths.

**Files Created/Modified**:

- `docs/SECURITY_AUDIT_2026-04-28.md` — comprehensive findings report with severity ranking, attack chains, and prioritized remediation plan

**Key Findings**:

- Critical tenant boundary risk in schema switching path due to unsanitized tenant slug flowing into `search_path` SQL.
- High-risk token exposure due to SSO access token being transported in URL query params.
- High-risk object-level authorization gaps on change request mutation paths.
- High-risk CORS policy mismatch (`*` patterns + credentials enabled).

**Implementation Guidance Included**:

- Immediate fixes (tenant slug validation + safe schema switching, SSO callback redesign, CR ownership policy checks, CORS tightening).
- Follow-on hardening (secure config defaults, upload guardrails, Redis-backed anti-abuse state, stronger password/session controls).

### Security Follow-Up Remediation (Completed 2026-05-02)

**Overview**: Implemented and verified SEC-001 through SEC-004 from the security audit follow-up plan.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java` — rejects invalid and unknown tenant slugs before request processing continues.
- `audita-api/api/src/main/java/io/audita/api/security/JwtAuthenticationFilter.java` — enforces tenant context consistency between header-resolved tenant and JWT tenant claim.
- `audita-api/api/src/main/java/io/audita/api/controller/SsoController.java` — changed SSO callback redirect to URL fragment code transport and body-based exchange endpoint contract.
- `audita-api/api/src/main/java/io/audita/api/dto/request/ExchangeSsoCodeRequest.java` — request DTO for one-time SSO exchange code.
- `audita-web/pages/auth/sso-callback.vue` — reads one-time exchange code from hash fragment and posts code in JSON body.
- `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java` — blocks wildcard origins and fails startup when CORS allowlist is missing.
- `audita-api/api/src/main/resources/application.yml` — profile-specific CORS defaults for dev and explicit allowlist requirement in prod.
- `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/ChangeRequestServiceSecurityTest.java` — regression tests for requester cross-user mutation denial.

**Key Changes**:

- Added strict tenant existence validation at filter boundary to reduce tenant header abuse surface.
- Added JWT tenant mismatch guard to prevent cross-tenant context confusion.
- Removed query-parameter style code transport from SSO callback and exchange request paths.
- Enforced explicit CORS allowlist and blocked wildcard origins under credentialed requests.
- Added regression tests proving non-owner requester cannot mutate another requester's change requests.

**Test Coverage**:

- `cd audita-api && ./gradlew :infrastructure:test --tests io.audita.infrastructure.service.ChangeRequestServiceSecurityTest` passes.
- `cd audita-api && ./gradlew :api:test --tests io.audita.api.controller.NotificationControllerWebMvcTest` passes.
- `cd audita-web && pnpm build` passes.

### Security Follow-Up Refinement (Completed 2026-05-03)

**Overview**: Completed remaining edge hardening for SEC-001, SEC-002, and SEC-004 and added targeted tenant filter regression tests.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java` — rejects bootstrap/setup requests that include a tenant header.
- `audita-api/api/src/test/java/io/audita/api/security/TenantResolutionFilterTest.java` — verifies bootstrap tenant-header rejection and invalid slug rejection.
- `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java` — removes constructor-level default CORS origin fallback.
- `audita-api/api/src/main/resources/application.yml` — keeps profile-specific CORS values while making base value explicit-empty.
- `audita-api/api/src/test/resources/application.yml` — supplies explicit test CORS allowlist.
- `audita-web/pages/auth/sso-callback.vue` — removes query-parameter fallback for SSO exchange code.

**Key Changes**:

- Prevents tenant-context injection attempts on platform bootstrap/setup routes by denying tenant headers at filter boundary.
- Ensures production-like explicit CORS config path by removing hidden Java fallback and relying on profile/config values.
- Enforces fragment-only callback exchange code handling in frontend to reduce URL query token residue.

**Test Coverage**:

- `cd audita-api && ./gradlew :api:test --tests io.audita.api.security.TenantResolutionFilterTest --tests io.audita.infrastructure.service.ChangeRequestServiceSecurityTest` passes.
- `cd audita-web && pnpm build` passes.

### Release Governance Foundation (Completed 2026-05-03)

**Overview**: Added source-available licensing, comprehensive README deployment/release documentation, and CI automation to publish Docker images and release tags on dev -> main merges.

**Files Created/Modified**:

- `LICENSE` — source-available policy with no resale and no managed-service resale terms.
- `LICENSE-APACHE` — Apache 2.0 base license reference linked by project license terms.
- `README.md` — complete project handbook with licensing, architecture, local/manual/Docker deployment, versioning strategy, and CI release behavior.
- `.github/workflows/ci-release.yml` — CI checks for PR/push and release automation on merged dev -> main PRs.

**Key Changes**:

- Enforced explicit no-resale and no hosted-service resale license condition while preserving personal and internal commercial use.
- Documented manual and Docker deployment procedures end-to-end for contributors and operators.
- Automated Docker Hub publishing with `latest`, `vX.Y.Z`, `vX.Y`, and `sha-<git-sha>` tags.
- Automated SemVer git tag and GitHub release generation on release merges.
- Standardized recommendation for historical versioning using milestone tags only.

### Release Tag Bootstrap (Completed 2026-05-03)

**Overview**: Created and pushed historical milestone SemVer tags to bootstrap release history before automated release flow takes over.

**Tags Created**:

- `v0.1.0` -> `50453f6` (Sprint 1 auth/bootstrap completion)
- `v0.2.0` -> `6ed7794` (Sprint 4 collaboration/notifications/SLA completion)
- `v0.3.0` -> `5cea0f4` (comprehensive E2E and entity mapping hardening)
- `v0.4.0` -> `98df35a` (security hardening pass)
- `v0.5.0` -> `dba36ce` (security refinement pass)

### Sprint 4 — Collaboration, Notifications & SLA Automation (Completed 2026-04-28)

**Overview**: Implemented comments with mention handling, in-app notification APIs + SSE streaming, SLA warning/breach scheduler automation, and frontend comments/notification wiring.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/controller/CommentController.java` — `GET/POST /api/v1/change-requests/{id}/comments`
- `audita-api/api/src/main/java/io/audita/api/controller/NotificationController.java` — notifications list/read/read-all + SSE stream
- `audita-api/api/src/main/java/io/audita/api/dto/request/CreateCommentRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/CommentResponse.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/NotificationResponse.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/StreamTokenResponse.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/CommentService.java` — sanitisation, mention extraction, mention persistence, notifications
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/NotificationService.java` — notification persistence, unread count, SSE emitter registry
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SlaMonitoringService.java` — scheduled SLA warning/breach monitor
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/CommentMentionEntity.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/CommentMentionId.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/CommentRepository.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/CommentMentionRepository.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/UserRepository.java` — added `findByEmailIgnoreCase`
- `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java` — permit notification stream route
- `audita-web/pages/change-requests/[id].vue` — added comments tab and compose flow
- `audita-web/components/shared/AppNotificationBell.vue` — initial notifications hydration
- `audita-web/plugins/sse.client.ts` — tokenized SSE stream URL and payload normalization
- `audita-api/api/src/test/java/io/audita/api/controller/NotificationControllerWebMvcTest.java` — added stream-token issuance and invalid-token rejection coverage
- `audita-web/types/index.ts` — comment author shape update

**Key Changes**:

- Added immutable comments endpoints with HTML sanitization and mention-based notifications.
- Added notification REST endpoints and unread-count header for bell state.
- Added SSE push channel hardening via short-lived stream tokens, with client reconnect behavior and invalid-token rejection coverage.
- Added scheduled SLA warning/breach event processing with activity + notification fan-out.

**Test Coverage**: Backend compile succeeded (`./gradlew compileJava`), frontend production build succeeded (`pnpm build`), Sprint 4 service tests pass (`CommentServiceTest`, `NotificationServiceTest`, `SlaMonitoringServiceTest`), and endpoint-level controller tests pass (`CommentControllerWebMvcTest`, `NotificationControllerWebMvcTest`).

### Post-Sprint 4 Hardening — E2E Harness (Completed 2026-04-28)

**Overview**: Implemented a fast isolated Layer-1 e2e harness for critical multi-tenant auth flow using Testcontainers + real HTTP server runtime.

**Files Created/Modified**:

- `audita-api/api/src/test/java/io/audita/api/integration/CriticalFlowsE2EL1Test.java` — isolated tenant schema setup with passing auth login/refresh/logout and invite acceptance/login e2e flows
- `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java` — corrected public route allowlist to include `/api/v1/auth/accept-invite`

**Validation**: harness expanded to 4 tests; current run is 3/4 passing with one remaining CR lifecycle failure on add-approver endpoint.

### Post-Sprint 4 Hardening — SSE Resilience Integration (Completed 2026-04-28)

**Overview**: Added integration coverage for stream-token issuance and unauthorized SSE stream guards.

**Files Created/Modified**:

- `audita-api/api/src/test/java/io/audita/api/integration/CriticalFlowsE2EL1Test.java` — added `tenant_notification_stream_token_resilience_e2e`

**Key Changes**:

- Added runtime integration assertion for `POST /api/v1/notifications/stream-token` with authenticated tenant user.
- Added negative-path assertions for `/api/v1/notifications/stream` with invalid and missing stream tokens.

**Test Coverage**: `./gradlew :api:test --tests "io.audita.api.integration.CriticalFlowsE2EL1Test.tenant_notification_stream_token_resilience_e2e"` passes.

### Final Verification Snapshot (2026-04-28)

- Backend: `./gradlew :api:test --tests "io.audita.api.integration.CriticalFlowsE2EL1Test"` => success (4 tests, 0 failed).
- Frontend: `cd audita-web && pnpm build` => success.

### Sprint 5 — Hardening & Release Readiness (Completed 2026-04-28)

**Overview**: Closed critical backend integration regressions and completed release-readiness verification gates.

**Files Created/Modified**:

- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` — initialize creator relation before response mapping on create/update/submit/approve/reject paths
- `audita-api/api/src/test/java/io/audita/api/security/UserPrincipalTest.java` — regression tests for UUID username semantics
- `audita-api/api/src/test/java/io/audita/api/integration/CriticalFlowsE2EL1Test.java` — compatibility hardening updates for CR lifecycle and activity stream mapping mismatches
- `audita-api/api/src/main/java/io/audita/api/exception/GlobalExceptionHandler.java` — unhandled exception logging for root-cause observability

**Test Coverage**:

- `./gradlew :api:test --tests "io.audita.api.security.UserPrincipalTest"` passes
- `./gradlew :api:test --tests "io.audita.api.integration.CriticalFlowsE2EL1Test"` passes
- `cd audita-web && pnpm build` passes

### Sprint 3 — Change Request Core (Completed, 2026-04-28)

**Overview**: Sprint 3 backend and frontend were implemented end-to-end, including approval workflow APIs, custom fields, activity stream, CR create/detail pages, and attachment upload/list support.

**Files Created/Modified**:

- `audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java` — added `/api/v1/change-requests` endpoints
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` — CR lifecycle, approver workflow, custom fields, activity logging, SLA deadline by priority
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/ChangeRequestRepository.java` — added optional multi-filter list query
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/CrApproverRepository.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/ChangeRequestCustomFieldRepository.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/ActivityStreamRepository.java`
- `audita-api/infrastructure/src/test/java/io/audita/infrastructure/persistence/entity/ChangeRequestEntityTest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/CreateChangeRequestRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/UpdateChangeRequestRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/AddApproverRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/ReorderApproversRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/RejectChangeRequestRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/request/UpsertChangeRequestCustomFieldsRequest.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/ChangeRequestResponse.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/CrApproverResponse.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/ChangeRequestCustomFieldResponse.java`
- `audita-api/api/src/main/java/io/audita/api/dto/response/ActivityStreamResponse.java`
- `audita-web/pages/change-requests/new.vue`
- `audita-web/pages/change-requests/[id].vue`
- `audita-web/pages/change-requests/index.vue`
- `audita-web/composables/useChangeRequests.ts`
- `audita-web/types/index.ts`

**Key Changes**:

- Added full approver workflow endpoints (add/remove/reorder/approve/reject) with linear sequence enforcement.
- Added custom field persistence endpoints and repository-backed storage in `change_request_custom_fields`.
- Added activity stream API and server-side event logging for all major CR actions.
- Added TipTap editor integration for CR description in create flow.
- Added CR detail tabs for Details, Approvers, and Activity with live API integration.
- Added attachment upload/list APIs and CR detail drag-and-drop file upload with uploaded files list.

**Test Coverage**: `./gradlew compileJava test --tests io.audita.infrastructure.persistence.entity.ChangeRequestEntityTest` passes and `pnpm build` for web succeeds.

---

### Sprint 2 — Multi-Tenancy, Users & Groups (Completed 2026-04-27)

**Overview**: Backend services and controllers for tenant provisioning, user management, roles, and groups are complete. Super Admin platform pages and tenant-admin pages are live.

**Files Created/Modified**:

- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/config/JpaConfig.java` — added `@EnableJpaRepositories`, `@EnableTransactionManagement`, and `JpaTransactionManager` bean to fix runtime crash (custom EMF caused Spring Boot JPA auto-config to back off)
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/TenantService.java` — provisioning (atomic: schema + Flyway + Admin user + invite), CRUD, domain whitelist, SSO config management
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/UserService.java` — invite, list, get, update, deactivate, reactivate
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/GroupService.java` — CRUD + member add/remove
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RoleService.java` — thin wrapper around `RoleRepository`
- `audita-api/api/src/main/java/io/audita/api/controller/TenantController.java` — `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- `audita-api/api/src/main/java/io/audita/api/controller/UserController.java` — tenant-scoped user management
- `audita-api/api/src/main/java/io/audita/api/controller/RoleController.java` — role listing; injects `RoleService` (not repository)
- `audita-api/api/src/main/java/io/audita/api/controller/GroupController.java` — group CRUD + member management
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/GroupEntity.java` — `groups` table entity
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/GroupMemberEntity.java` — composite key `GroupMemberId`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/GroupRepository.java`
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/GroupMemberRepository.java`
- `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__create_groups.sql` — `groups` + `group_members` tables
- `audita-api/api/src/main/java/io/audita/api/dto/` — all request + response DTOs for tenants, users, roles, groups
- `audita-web/pages/platform/index.vue` — Super Admin platform dashboard (layout: `platform`)
- `audita-web/pages/platform/tenants/index.vue` — tenant list with pagination
- `audita-web/pages/platform/tenants/new.vue` — provision new org; slug auto-suggest
- `audita-web/pages/platform/tenants/[id].vue` — tenant detail: Overview | Domain Whitelist | SSO Config tabs
- `audita-web/pages/admin/users/index.vue` — user table + inline invite modal + role edit
- `audita-web/pages/admin/roles/index.vue` — roles & permissions matrix (read view)
- `audita-web/pages/admin/groups/index.vue` — group table + create/delete + member management modal

**Key Changes**:

- `JpaConfig` was missing `@EnableJpaRepositories` — once you define a custom `LocalContainerEntityManagerFactoryBean`, Spring Boot's JPA auto-config backs off entirely, taking the `@EnableJpaRepositories` setup with it. Added explicitly.
- `api/build.gradle.kts` gained `spring-data-commons` dep so `Pageable`/`Page` types resolve in controller layer without pulling full JPA starter.
- `RoleController` intentionally injects `RoleService` rather than `RoleRepository` — keeps JPA out of the `api` module.

**Test Coverage**: Compilation clean across all 4 modules. Runtime fix verified. Integration tests deferred to Sprint 5.

---

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

### Comprehensive E2E Test Coverage (Completed 2026-04-29)

**Overview**: Created `AllSprintsE2ETest.java` — 44 ordered Layer 1 integration tests covering every implemented endpoint across all 5 sprints. Uncovered and fixed 2 production bugs in the process.

**Files Created/Modified**:

- `audita-api/api/src/test/java/io/audita/api/integration/AllSprintsE2ETest.java` — 44 ordered tests, full sprint coverage
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/GroupEntity.java` — removed phantom `updatedAt` field (column not in schema)
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/PasswordResetTokenEntity.java` — added `@Column(name="token_hash")` and `@Column(name="expires_at")`

**Key Changes**:

- `GroupEntity` had `private OffsetDateTime updatedAt` with a `@PreUpdate` hook, but the `groups` DB table has no `updated_at` column — Hibernate threw on every INSERT/UPDATE
- `PasswordResetTokenEntity` fields lacked explicit `@Column(name=...)` — JpaConfig bypasses `application.yml` naming strategy; Hibernate mapped `tokenHash` → `tokenhash` causing `forgot-password` 500
- Test uses `@SpringBootTest(properties={"audita.storage.local.base-path=/tmp/audita-test-uploads"})` to provide a writable storage directory
- SSE stream (`GET /notifications/stream`) intentionally not connected in tests — long-lived connection blocks indefinitely; token issuance verified instead

**Test Coverage**: 62/62 tests passing (0 failures). Covers Sprint 1–5 full API surface.
