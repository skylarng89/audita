# Audita — Changelog

## [0.1.0] — Unreleased (In Development)

### Added (Sprint 4 — 2026-04-28)

- **Comments API**: `GET/POST /api/v1/change-requests/{id}/comments` via `CommentController` + `CommentService`
- **Comment sanitization + mentions**: OWASP sanitization, mention extraction (`@email`), `comment_mentions` persistence
- **Mention fan-out**: in-app notifications and mention email dispatch via `EmailService.sendMentionEmail`
- **Notifications API**: `GET /api/v1/notifications`, `PATCH /api/v1/notifications/{id}/read`, `POST /api/v1/notifications/read-all`
- **Unread count header**: `X-Unread-Count` included on notifications list endpoint
- **SSE notifications stream**: `GET /api/v1/notifications/stream` with server-side emitter registry for real-time pushes
- **SLA automation**: scheduled warning/breach evaluator (`SlaMonitoringService`) with activity stream events, in-app notifications, and breach emails
- **Frontend comments tab**: CR detail page now supports comments list + compose/post flow
- **Frontend notification hydration**: bell now fetches initial notification list before live SSE updates
- **SSE client auth hardening**: stream connects with access-token query parameter and robust payload mapping
- **Endpoint-level controller tests**: added `CommentControllerWebMvcTest` and `NotificationControllerWebMvcTest` for request/response contract coverage
- **Mention extraction fix**: corrected `CommentService` mention parsing to evaluate raw comment text prior to HTML sanitization

### Added (Sprint 3 — 2026-04-27)

- **ChangeRequestController** (`/api/v1/change-requests`): create, update, submit, cancel, list, detail
- **ChangeRequestService**: core CR lifecycle operations with domain transition guards
- **CR DTOs**: `CreateChangeRequestRequest`, `UpdateChangeRequestRequest`, `ChangeRequestResponse`
- **Filtering support**: repository-level optional filters (`status`, `priority`, `category`, `createdBy`) for paginated CR list endpoint
- **SLA baseline logic**: submission sets initial deadline by priority (LOW 72h, MEDIUM 48h, HIGH 24h, CRITICAL 8h)
- **Approver workflow APIs**: add/remove/reorder approvers + approve/reject decisions with rejection reason enforcement
- **Approval closure + lock rules**: closure evaluation after decisions and `approval_locked` enforcement
- **Custom field value APIs**: `PUT/GET /api/v1/change-requests/{id}/custom-fields` backed by `change_request_custom_fields`
- **Activity stream API**: `GET /api/v1/change-requests/{id}/activity` with server-side event logging on CR actions
- **Frontend CR pages**: `/change-requests/new` (TipTap description editor), `/change-requests/[id]` (Details, Approvers, Activity tabs)
- **Frontend integration updates**: `useChangeRequests` expanded for Sprint 3 endpoints; CR list page aligned with backend response shape

### Added (Sprint 2 — 2026-04-27)

- **TenantService**: `provision()` (atomic: schema creation + Flyway + Admin user + invite token), CRUD, domain whitelist management, SSO config upsert/delete
- **UserService**: invite user (48h token + email), list, get, update, deactivate, reactivate
- **GroupService**: group CRUD + member add/remove; `GroupEntity`, `GroupMemberEntity` (composite key)
- **RoleService**: thin listing wrapper keeping JPA out of the `api` module
- **TenantController** (`/api/platform/v1/tenants`): full CRUD + domains + SSO; `@PreAuthorize("hasRole('SUPER_ADMIN')")`
- **UserController** (`/api/v1/users`): invite, list, get, update, deactivate, reactivate
- **RoleController** (`/api/v1/roles`): list roles with permissions
- **GroupController** (`/api/v1/groups`): group CRUD + member management
- **V3 Flyway migration** (`V3__create_groups.sql`): `groups` + `group_members` tables per tenant
- **All DTOs**: request/response objects for tenants, users, roles, groups, domains, SSO configs
- **Frontend — Platform**: `pages/platform/index.vue` (dashboard), `tenants/index.vue`, `tenants/new.vue` (slug auto-suggest), `tenants/[id].vue` (Overview | Domains | SSO tabs)
- **Frontend — Admin**: `pages/admin/users/index.vue` (invite modal), `admin/roles/index.vue` (permission matrix), `admin/groups/index.vue` (member management)
- **`JpaConfig` fix**: added `@EnableJpaRepositories`, `@EnableTransactionManagement`, and `JpaTransactionManager` — custom `EntityManagerFactory` caused Spring Boot JPA auto-config to back off, leaving 0 repositories registered

### Added (Sprint 1 — 2026-04-27)

- **Authentication stack**: JWT access tokens (15-min, jjwt 0.12.6), SHA-256 hashed refresh tokens (7-day rotating, HttpOnly cookie), BCrypt cost=12
- **AuthService**: login, logout, forgot-password, reset-password, refresh, bootstrap, domain whitelist, sliding-window rate limiting
- **JwtAuthenticationFilter**: validates JWT on every protected request; populates `SecurityContextHolder`
- **TenantResolutionFilter**: sets `TenantContext` from `X-Tenant-Slug` header; lives in `api` module
- **SSO**: Google OIDC + Microsoft Azure AD OAuth2 via `SsoController` + `SsoService`; JIT user provisioning; OAuth account linking; AES-256-GCM encrypted client secrets
- **Platform bootstrap endpoint**: `POST /api/platform/v1/bootstrap` — creates first Super Admin when none exists
- **Auth pages (Nuxt 3)**: sign-in, forgot-password, reset-password (4-segment strength bar), accept-invite, bootstrap/first-run, sso-callback
- **`useAuthStore`** (Pinia): accessToken, user, tenantSlug, `setAuth()`, `clearAuth()`
- **18 unit tests** for `AuthService`: all auth flows, rate limiting, domain whitelist, bootstrap — all passing

### Added (Sprint 0 — 2026-04-27)

- Project documentation: PRD v1.0, SRS v1.0, USER_FLOW v1.0
- UI designs: 40 screens (light + dark) covering all user journeys
- Memory bank initialised: context, tech-stack, design, decisions, patterns
- Sprint plan created: 8 sprints (Sprint 0–7) covering MVP through production-ready release
- `audita-api`: Gradle multi-module (domain / application / infrastructure / api), Spring Boot 4.0.6, Java 25, HikariCP, Hibernate 7 multi-tenancy, Flyway (public + per-tenant), Spring Security scaffold, RFC 7807 global exception handler, Logback structured JSON logging (`logstash-logback-encoder:8.1`)
- `audita-web`: Nuxt 3, Tailwind CSS (custom design tokens), all three layouts, auth/role/tenant middleware, `plugins/api.ts`, `useAuthStore` (Pinia), shared component library (AppButton, AppInput, AppBadge, AppCard, AppModal, AppTable, AppPagination)
- `docker-compose.yml`: PostgreSQL 17, MailHog, Spring Boot API, Nuxt 3 web with healthchecks
- `README.md`: full run instructions (Docker Compose + standalone)
- `.dockerignore` for both repos; `.gitkeep` files in all empty Gradle module source directories
