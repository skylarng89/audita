# Audita — Active Context

**Last Updated:** 2026-05-18
**Current Phase:** Active development — Sprint 10 complete (32/36 tasks)
**Active Sprint:** Sprint 10

---

## What Is Audita?

Audita is a **self-hosted, multi-tenant ITIL/ITSM Change Management platform**. It enables organisations to manage the full lifecycle of IT change requests: creation, structured approval workflows, real-time collaboration, SLA tracking, and a complete immutable audit trail.

**Mission:** Make change management simple, transparent, and auditable for every team — from SMEs to enterprises.

---

## Current State

- **Sprint 6 complete (Audit Trail & Admin Configuration).** Global audit trail (paginated query + CSV export), admin custom fields CRUD, `AuditLogService` wired into `ChangeRequestService` for all CR state transitions. 90/90 tests passing. TypeScript typecheck clean.
- **Sprint 7 complete (2026-05-11).** Three-layer file upload validation (magic bytes + extension + MIME; `.env` allowlist), filename normalization, path traversal guard. Custom fields 400 fix (definition-driven pattern). New `/admin/custom-fields` CRUD page + admin sidebar link. CR detail redesigned: read-only default, DRAFT-gated Edit mode with TipTap, custom fields in edit form, `onBeforeUnmount` cleanup. VueDatePicker fully removed from Create CR and CR Edit pages — replaced with native `<input type="date">` + `<input type="time">` fields with `colorScheme` dark mode styling; `vue-datepicker.client.ts` plugin deleted; `.dp__*` CSS override block removed from `main.css`. TypeScript typecheck: exit 0.
- **Sprint 8 started (2026-05-11).** Workflow/SLA admin settings activation slice implemented: new `PATCH /api/v1/settings`, `org_settings` persistence entity/repository, expanded settings response, workflow/sla editing enabled in Admin Settings UI, SLA runtime lookups switched from hard-coded values to tenant settings where configured, and controller regression tests added.
- **Sprint 10 complete (32/36 tasks, 2026-05-18).** Comprehensive UX + WCAG 2.2 overhaul across the entire frontend: mobile navigation drawer, dark mode toggle in header, dead search removed, skip-to-main-content link, page titles on all pages, password show/hide toggles, ARIA tablist on CR detail, focus trap in AppModal, skeleton loaders + empty states on CR list, SLA column, sticky save bar, reject confirm modal, toast progress bar, scroll-margin-top, aria-live regions, label/id wiring on all forms, autocomplete tokens. 4 tasks deferred: sidebar icon-rail (UX10-003), filter pill collapse (UX10-004), AppButton migration on auth pages (UX10-007), affected systems tag UI (UX10-016).
- **Sprint 11 complete (2026-05-12).** Security-first session resilience is implemented end-to-end: refresh cookies now revoke correctly on logout, frontend refresh is `401`-only, access tokens are no longer persisted in JS-readable storage, cold-start restore uses a non-rotating HttpOnly-cookie-backed `/api/v1/auth/session` endpoint, API contract mismatches force local sign-out, and browser tabs synchronize restore/logout events without sharing tokens.
- **Security config stabilized (2026-05-12).** Replaced the temporary reflection-based `SecurityConfig` authorization workaround with Spring Security public APIs: `RequestMatcherDelegatingAuthorizationManager`, `AuthorizationFilter`, and `PathPatternRequestMatcher`. Focused authorization regression tests now lock public auth routes, authenticated fallback, and super-admin platform routes.
- **Sprint 9 complete (2026-05-11).** Change request list scalability update completed: `/change-requests` now uses bounded server-side pagination with `size=50` and explicit previous/next pagination buttons for predictable navigation at scale.
- **Docker environment fully operational (2026-05-08).** Resolved three issues: (1) Hibernate dialect deprecation warning removed; (2) mail health indicator disabled so SMTP auth failure doesn't block container health; (3) CI workflow upgraded to pnpm v10 to match local lockfile format.
- **Sprint 0 complete (19/19 tasks).** Both repositories are scaffolded and runnable via Docker Compose.
- **Sprint 1 complete (22/22 tasks).** Full authentication stack: JWT + refresh tokens, SSO (Google/Microsoft), domain whitelist, rate limiting, 18 unit tests passing.
- **Sprint 2 complete (19/19 tasks).** Remaining tasks (USR-007, USR-008, TENANT-005) were completed with controller policy tests and tenant-schema isolation integration coverage.
- **Sprint 3 complete (21/21 tasks).** Added CR attachments API and frontend upload/list flows; Sprint 3 is fully closed.
- **Sprint 4 complete (10/10 tasks).** Added comments APIs + mention extraction/persistence, notifications REST + SSE stream, SLA warning/breach scheduler automation, and frontend comments/notification wiring.
- **Sprint 5 validation expanded.** Added endpoint-level controller tests for comments and notifications, including stream-token issuance and invalid-token SSE access rejection; targeted Gradle run passes.
- **Comprehensive E2E test coverage complete.** `AllSprintsE2ETest.java` — 44 ordered integration tests covering every implemented endpoint across all 5 sprints. 62/62 tests passing.
- **Security follow-up complete (SEC-001..SEC-004).** Tenant slug boundary hardening, SSO callback URL transport hardening, CR object-level mutation regression coverage, and strict CORS allowlist enforcement are now implemented and verified.
- **Security follow-up refinement complete (2026-05-03).** Bootstrap/setup now reject tenant headers, callback code parsing is fragment-only on frontend, and tenant-header hardening has dedicated filter tests.
- **Tailwind v4 migration complete (2026-05-04).** Frontend now uses `@tailwindcss/vite` (not `@nuxtjs/tailwindcss`), CSS entry is v4-compatible (`@import "tailwindcss"` + `@config`), and custom utility composition was refactored to satisfy v4 `@apply` constraints.
- **Frontend verification green after migration (2026-05-04).** `pnpm test`, `pnpm -s nuxi typecheck`, and `pnpm build` all pass.
- **Production entity bugs fixed (cumulative):** `GroupEntity` (phantom `updated_at` column), `PasswordResetTokenEntity` (`tokenHash`/`expiresAt` column name mapping), `RoleEntity`, `UserEntity`, `InviteTokenEntity`, `RefreshTokenEntity` — all now have explicit `@Column(name=...)` to survive the `JpaConfig` naming-strategy bypass.
- Documentation complete: PRD v1.0, SRS v1.0, USER_FLOW v1.0 (`docs/`). UI designs: 40 screens (`ui-designs/`).
- `audita-api`: hexagonal structure, JPA/Hibernate multi-tenancy, Flyway migrations, Spring Security scaffold, RFC 7807 exception handler, structured JSON logging (Logstash encoder).
- `audita-web`: Nuxt 3, Tailwind tokens, all layouts, auth/role/tenant middleware, `plugins/api.ts`, `useAuthStore`, shared component library (AppButton, AppInput, AppBadge, AppCard, AppModal, AppTable, AppPagination).
- README.md written with Docker Compose and standalone run instructions.
- Known pattern: empty Gradle module `src/` dirs need `.gitkeep` files — Git ignores empty dirs, Docker `COPY` fails without them.

---

## Five User Personas

| Role            | Scope         | Key Capability                                                     |
| --------------- | ------------- | ------------------------------------------------------------------ |
| **Super Admin** | Platform-wide | Manage tenants, domain whitelists, SSO per org                     |
| **Admin**       | Org-wide      | Configure workflows, manage users/roles/groups, SLA, custom fields |
| **Requester**   | Org-wide      | Create and track change requests                                   |
| **Approver**    | Org-wide      | Review and action change requests                                  |
| **Auditor**     | Org-wide      | Read-only: view CRs, audit trail                                   |

---

## Application Structure (Two Repositories)

| Repo         | Tech                    | Description                                       |
| ------------ | ----------------------- | ------------------------------------------------- |
| `audita-api` | Java 25 + Spring Boot 4 | REST API, business logic, data layer              |
| `audita-web` | Nuxt 3 + Vue 3          | SSR frontend, component library, state management |

Both served via Docker Compose (dev) or Helm/K8s (production).

---

## Key Architectural Decisions

1. **Schema-per-tenant** PostgreSQL isolation — each org gets its own schema.
2. **JWT + refresh token** auth (HttpOnly cookie). Access token: 15 min, refresh: 7 days.
3. **SSE** (Server-Sent Events) for real-time in-app notifications — no WebSocket complexity.
4. **TipTap** rich-text editor for CR descriptions and comments.
5. **Hexagonal architecture** on the backend (`domain`, `application`, `infrastructure`, `api` layers).
6. **Flyway** for per-tenant schema migrations, idempotent and version-controlled.
7. **AES-256** encryption for SSO client secrets and SMTP passwords at rest.
8. **OWASP Java HTML Sanitizer** for server-side rich-text sanitisation before storage.

---

## MVP Delivery Strategy

The sprint plan is structured to deliver a **usable, end-to-end MVP** as early as Sprint 4/5, enabling:

1. Organisations to be provisioned.
2. Users to be invited and authenticated.
3. Change requests to be created, submitted, and approved.
4. Basic notification flow.

Advanced features (SLA, custom fields, audit export, full admin config) follow in subsequent sprints.

---

## Active Blockers / Open Questions

- Resolved this session: browser-only bootstrap submit returned 403 while CLI succeeded.
- Root cause confirmed as CORS rejection (`Invalid CORS request`) on proxied browser requests.
- Fix landed in Nuxt proxy route by stripping forwarded `Origin`/`Referer`/`Host` before upstream hop.
- No active blocker currently after fix validation (browser bootstrap now succeeds and redirects to sign-in).
- No active blocker introduced by Tailwind v4 migration; build/test/typecheck are stable.

---

## Sprint 1 Key Patterns & Decisions

- **`TenantResolutionFilter`** lives in `api` module (not `infrastructure`) — only `api` has `spring-boot-starter-webmvc` with servlet API.
- **`infrastructure` module** has `spring-boot-starter-web` (not webmvc) for `RestClient` used by `SsoService`.
- **JWT**: 15-min access tokens (jjwt 0.12.6), SHA-256 hashed refresh tokens (7-day rotating), stored HttpOnly cookie at path `/api/v1/auth/refresh`.
- **AES-256-GCM**: `AesEncryptionService` for SSO client secrets. Key from `audita.encryption.key` (64 hex chars).
- **Rate limiting**: In-memory `ConcurrentHashMap<String, LinkedList<Instant>>` sliding window. Login: 5/15min/IP+email. Forgot-password: 3/hr/email.
- **SSO state**: `ConcurrentHashMap` with 10-min TTL, random 32-byte token.
- **Domain whitelist**: Open tenant if no domains configured; otherwise email domain must match.
- **BCrypt cost=12** in production; cost=4 in tests via `ReflectionTestUtils`.

---

## Next Actions

1. Execute immediate security hardening actions from `docs/SECURITY_AUDIT_2026-04-28.md` (tenant isolation, SSO token transport, BOLA controls, CORS tightening).
2. Keep critical e2e suite as a mandatory CI gate and add security regression gates (tenant fuzz/BOLA/SSO token handling).
3. Continue replacing compatibility shim logic with permanent entity/migration alignment incrementally.
4. Add regression coverage for the Nuxt proxy bootstrap path (ensure upstream bootstrap POST is not CORS-rejected).

---

## Security Review Snapshot (2026-04-28)

- Completed adversarial security review and published findings in `docs/SECURITY_AUDIT_2026-04-28.md`.
- Highest-risk issues identified:
  - Tenant schema switching surface relies on unsanitized header-derived slug concatenated into SQL (`search_path`).
  - SSO callback currently transports access token in URL query string.
  - Change request mutation paths lack object-level authorization checks for requester-level actors.
  - CORS is configured with wildcard origin patterns while credentials are enabled.
- Recommended immediate path: fix those four findings before further feature expansion.

---

## Session Updates (2026-04-29)

- Resolved production UX/runtime chain affecting change request creation flow:
  - submit/create succeeded, but redirect to detail failed with 500 due to lazy `createdBy` initialization.
  - read-path initialization added in `ChangeRequestService` to stabilize response mapping.
- Resolved recurring authorization/rendering issues:
  - role authority normalization in `UserPrincipal` to satisfy Spring `hasRole/hasAnyRole` checks.
  - frontend token refresh fallback expanded to include 403 responses.
  - layout shared-component tag alignment fixed missing sidebar/user menu/notification bell rendering.
- Completed CR UI consistency audit pass:
  - standardized button sizing behavior for variant buttons.
  - patched CR list/create/detail actions and tabs for consistent button treatment.
  - corrected CR description rendering to display editor HTML content.
- Build and deployment validation completed:
  - backend compile and frontend production build passed.
  - container naming conflict resolved during compose redeploy; services returned healthy.

## Current Blockers

- No active blocker currently.
