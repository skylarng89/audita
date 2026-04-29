# Audita — Active Context

**Last Updated:** 2026-04-29
**Current Phase:** Active development
**Active Sprint:** Sprint 5 — Hardening & Release Readiness (Completed)

---

## What Is Audita?

Audita is a **self-hosted, multi-tenant ITIL/ITSM Change Management platform**. It enables organisations to manage the full lifecycle of IT change requests: creation, structured approval workflows, real-time collaboration, SLA tracking, and a complete immutable audit trail.

**Mission:** Make change management simple, transparent, and auditable for every team — from SMEs to enterprises.

---

## Current State

- **Sprint 0 complete (19/19 tasks).** Both repositories are scaffolded and runnable via Docker Compose.
- **Sprint 1 complete (22/22 tasks).** Full authentication stack: JWT + refresh tokens, SSO (Google/Microsoft), domain whitelist, rate limiting, 18 unit tests passing.
- **Sprint 2 complete (19/19 tasks).** Remaining tasks (USR-007, USR-008, TENANT-005) were completed with controller policy tests and tenant-schema isolation integration coverage.
- **Sprint 3 complete (21/21 tasks).** Added CR attachments API and frontend upload/list flows; Sprint 3 is fully closed.
- **Sprint 4 complete (10/10 tasks).** Added comments APIs + mention extraction/persistence, notifications REST + SSE stream, SLA warning/breach scheduler automation, and frontend comments/notification wiring.
- **Sprint 4 validation expanded.** Added endpoint-level controller tests for comments and notifications, including stream-token issuance and invalid-token SSE access rejection; targeted Gradle run passes.
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
