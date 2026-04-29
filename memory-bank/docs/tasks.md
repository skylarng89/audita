# Audita — Developer Task List

**Project:** Audita — Multi-Tenant ITIL/ITSM Change Management Platform
**Version:** 0.1.0
**Last Updated:** 2026-04-29
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

| Task ID  | Task                                                             | Priority | Status       | Assigned To | Notes                                                                               |
| -------- | ---------------------------------------------------------------- | -------- | ------------ | ----------- | ----------------------------------------------------------------------------------- |
| HARD-001 | Resolve critical CR lifecycle integration regressions            | High     | ✅ Completed | Developer 1 | `CriticalFlowsE2EL1Test` now passes all 4 flows after service + compatibility fixes |
| HARD-002 | Add principal identity regression tests                          | High     | ✅ Completed | Developer 1 | Added `UserPrincipalTest` to lock UUID username semantics                           |
| HARD-003 | Harden CR response mapping against lazy-loading runtime failures | High     | ✅ Completed | Developer 1 | Added service-layer creator initialization before returning mapped entities         |
| HARD-004 | Run backend release gate (critical suite)                        | High     | ✅ Completed | Developer 1 | `./gradlew :api:test --tests "io.audita.api.integration.CriticalFlowsE2EL1Test"`    |
| HARD-005 | Run frontend release gate (production build)                     | High     | ✅ Completed | Developer 2 | `cd audita-web && pnpm build`                                                       |

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
| **TOTAL** | **96**      | **0**       | **0**       | **96**    | **100%**   |

---

## Recent Implementations

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
