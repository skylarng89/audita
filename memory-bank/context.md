# Audita — Active Context

**Last Updated:** 2026-05-25
**Current Phase:** Public launch readiness — unified tenant baseline migration + replica-safe Flyway + CI stabilization
**Active Sprint:** Post-Sprint 8 — CI + production tenant auth stabilization

---

## What Is Audita?

Audita is a **self-hosted, multi-tenant ITIL/ITSM Change Management platform**. It enables organisations to manage the full lifecycle of IT change requests: creation, structured approval workflows, real-time collaboration, SLA tracking, and a complete immutable audit trail.

**Mission:** Make change management simple, transparent, and auditable for every team — from SMEs to enterprises.

---

## Current State

### Sprint Completion Status

| Sprint | Completed | Total | Theme | Date |
|--------|-----------|-------|-------|------|
| Sprint 0 | 19/19 | 19 | Foundation & Scaffolding | 2026-04-27 |
| Sprint 1 | 22/22 | 22 | Authentication & Platform Bootstrap | 2026-04-27 |
| Sprint 2 | 19/19 | 19 | Multi-Tenancy, Users & Groups | 2026-04-27 |
| Sprint 3 | 21/21 | 21 | Change Request Core | 2026-04-28 |
| Sprint 4 | 10/10 | 10 | Collaboration, Notifications & SLA | 2026-04-28 |
| Sprint 5 | 8/8 | 8 | Hardening, Release Readiness & E2E | 2026-04-28 |
| Sprint 6 | 7/7 | 7 | Audit Trail & Admin Configuration | 2026-05-04 |
| Sprint 7 | 8/8 | 8 | File Security, Custom Fields UX & CR Edit | 2026-05-11 |
| Sprint 8 | 4/4 | 4 | Admin Settings Activation & SLA Defaults | 2026-05-11 |
| Sprint 9 | 1/1 | 1 | CR List Scalability | 2026-05-11 |
| Sprint 10 | 36/36 | 36 | UX & WCAG 2.2 Compliance Overhaul | 2026-05-18 |
| Sprint 11 | 26/26 | 26 | Session Hardening, RBAC Expansion & CR Workflow Polish | 2026-05-12 |
| Sprint 12 | 6/6 | 6 | Launch Readiness | 2026-05-19 |
| Sprint 13 | 8/8 | 8 | Engineering Best Practices Hardening | 2026-05-19 |
| Post-Sprint 1 | 19/19 | 19 | Reliability, UX & Rich-Text Hardening | 2026-05-22 |
| Post-Sprint 2 | 8/8 | 8 | Approver UX Polish + Activity Stream + CI Trivy Fix | 2026-05-22 |
| Post-Sprint 3 | 9/9 | 9 | Mention UX + Comment Deep-Link + Nuxt API XSS Validator Scope Fix | 2026-05-22 |
| Post-Sprint 4 | 6/6 | 6 | DHI Hardened Runtime + Docker Build Reliability | 2026-05-23 |
| Post-Sprint 5 | 7/7 | 7 | Approver Workflow Flexibility + Activity Summary Test Coverage | 2026-05-23 |
| Post-Sprint 6 | 3/3 | 3 | Web Docker policy parity + pnpm config cleanup | 2026-05-24 |
| Post-Sprint 7 | 1/1 | 1 | Production: subdomain-based tenant resolution | 2026-05-25 |
| Post-Sprint 8 | 1/1 | 1 | CI + production tenant auth stabilization | 2026-05-25 |
| **TOTAL** | **249/249** | **249** | — | — |

**Sprint 12: Launch Readiness** — All 6 tasks completed. v0.6.0 released.

---

## Sprint Summaries

- **Sprint 0–2 complete.** Scaffold, auth, tenant provisioning, users/groups, roles. 60/60 tasks. All core infrastructure operational.
- **Sprint 3–4 complete.** Change request lifecycle, approvals, comments, notifications, SLA automation, SSE streaming. 31/31 tasks.
- **Sprint 5–6 complete.** E2E harness (44 tests), security audit, hardening, release governance (license, CI/CD, tags `v0.1.0`–`v0.5.0`), Tailwind v4 migration, audit trail, admin configuration, mock data removal. 15/15 tasks.
- **Sprint 7 complete (2026-05-11).** 3-layer file upload validation (magic bytes + extension + MIME), filename normalization, path traversal guard, admin custom fields CRUD page, CR detail read-only/edit mode, native date/time inputs replacing VueDatePicker. 8/8 tasks.
- **Sprint 8 complete (2026-05-11).** Workflow/SLA admin settings activation: `PATCH /api/v1/settings`, `org_settings` persistence, runtime SLA lookups from tenant settings, controller regression tests, tenant settings port decoupling. 4/4 tasks.
- **Sprint 9 complete (2026-05-11).** Server-side pagination with `size=50`, explicit previous/next controls. 1/1 tasks.
- **Sprint 10 complete (2026-05-18).** Comprehensive UX + WCAG 2.2 overhaul: mobile navigation drawer, dark mode toggle, skip-to-content link, page titles on all pages, password show/hide toggles, ARIA tablist on CR detail, focus trap in AppModal, skeleton loaders + empty states, SLA column, sticky save bar, reject confirm modal, toast progress bar, scroll-margin-top, aria-live regions, label/id wiring on all forms, autocomplete tokens, sidebar icon-rail, filter pill collapse, affected systems tag UI. 36/36 tasks.
- **Sprint 11 complete (2026-05-12).** Session hardening: refresh-cookie revocation, 401-only refresh, HttpOnly cold-start restore, API contract enforcement (`X-Audita-Api-Contract`), cross-tab sync, Spring Security public API authorization. RBAC expansion: multi-role assignment, custom roles with overlap prevention, auto-approver population on CR create, JWT role+permission claims. CR workflow polish: role-flexible approver voting, comment/activity DTO hardening, rich-text toolbar, attachment queue, vote visibility, modal centering. 26/26 tasks.
- **Sprint 12 complete (2026-05-19).** Launch readiness: CSS token alignment, pagination wiring, WCAG 2.5.8 target sizing, Sonar scan + dependency audit, E2E smoke test, v0.6.0 release cut. 6/6 tasks.
- **Sprint 13 complete (2026-05-19).** Engineering best practices: GitHub Actions SHA pinning, CI security gates (SAST/SCA/image scan), SBOM generation, OpenTelemetry + Prometheus metrics, readiness/liveness probes, API idempotency keys, Nuxt proxy hardening, `nuxt-security` CSP headers. 8/8 tasks.
- **Post-Sprint hardening complete (2026-05-22).** Settings save 400 fixes (UUID parsing → proxy content-length → tolerant map parsing), auth session/logout tenant-header guards + V10 refresh_tokens repair migration, log noise elimination (dialect, cache stats, pagination fetch, SSE lifecycle), full rich-text editor upgrade (TipTap Link + expanded toolbar + backend sanitizer + render normalization + CSS), approver UX redesign (multi-select list with checkboxes, per-user Required toggle, real-time selected chips preview, batch save). 19/19 tasks.
- **Post-Sprint 2 polish complete (2026-05-22).** Approver UX polish: default Optional, per-approver Required/Optional toggle on saved list, creator excluded from candidates, dirty tracking + save prompt, reorder animations (`TransitionGroup` + CSS). Backend: `PATCH /{id}/approvers/{approverId}/requirement` endpoint. Activity stream: "Reordered 4 approvers" human-readable summary instead of raw "COUNT 4" field. CI: `.trivyignore` for CVE-2026-33671 (picomatch ReDoS in Node.js base image, not exploitable). 8/8 tasks.
- **Post-Sprint 3 hotfix complete (2026-05-22).** Comment mentions now support live `@` autocomplete (backend user search endpoint + TipTap mention popup with keyboard support), mention emails deep-link directly to comment (`?commentId=`), CR detail auto-scroll/highlight targets comment anchor, auth middleware/login preserve redirect target after sign-in, Nuxt `xssValidator` disabled for `/api/**` proxy routes to prevent false-positive 400 rejects on mention markup, and backend comment sanitizer allowlists mention `span` attributes. 9/9 tasks.
- **Post-Sprint 6 container scan reliability complete (2026-05-24).** Fixed web image build failure in CI/container scan path by copying `pnpm-workspace.yaml` before `pnpm install` so minimum release-age exclusions are available during lockfile policy validation. Cleaned pnpm config drift by moving deprecated `package.json#pnpm.supportedArchitectures` to `pnpm-workspace.yaml`. Runtime image remains hardened (`dhi.io/node:24`) with numeric non-root user. 3/3 tasks.

---

## Sprint 13 Task Status (Engineering Best Practices Hardening)

| Task ID | Task | Priority | Status | Owner |
|---------|------|----------|--------|-------|
| BP13-001 | Pin GitHub Actions to immutable SHAs + least-privilege permissions | High | ✅ Completed | Developer 1 |
| BP13-002 | Add CI security gates (dependency audit, image scan, SAST) | High | ✅ Completed | Developer 1 |
| BP13-003 | Generate and publish SBOM artifacts | Medium | ✅ Completed | Developer 1 |
| BP13-004 | Add backend OpenTelemetry + Prometheus metrics | High | ✅ Completed | Developer 1 |
| BP13-005 | Add readiness/liveness probes and tighten actuator exposure | Medium | ✅ Completed | Developer 1 |
| BP13-006 | Implement API idempotency key support for retriable mutating endpoints | High | ✅ Completed | Developer 1 |
| BP13-007 | Harden Nuxt API proxy forwarding and request validation | Medium | ✅ Completed | Developer 2 |
| BP13-008 | Add `nuxt-security` and enforce frontend CSP/security headers | Medium | ✅ Completed | Developer 2 |

---

## Quality Gates

- **Backend tests**: API module and infrastructure module tests passing; pre-existing `AuthServiceTest` Mockito stubbing issues confirmed unrelated to recent changes
- **Frontend gates**: `pnpm test` (47 tests, 14 files), `pnpm -s nuxi typecheck`, `pnpm build` all passing
- **CI Trivy/container image scan path**: web Docker build now passes lockfile supply-chain policy validation in containerized install step after workspace-policy copy fix
- **Docker**: Full Compose stack operational (PostgreSQL 17, MailHog, API, Web)
- **Security hardening**: SEC-001 through SEC-004 complete, with refinements; Sprint 13 best practices complete
- **Sonar**: `sonar-scan.sh` ready; last known state: no critical/blocker issues after regex normalization fix

---

## Five User Personas

| Role | Scope | Key Capability |
|------|-------|---------------|
| **Super Admin** | Platform-wide | Manage tenants, domain whitelists, SSO per org |
| **Admin** | Org-wide | Configure workflows, manage users/roles/groups, SLA, custom fields |
| **Requester** | Org-wide | Create and track change requests |
| **Approver** | Org-wide | Review and action change requests |
| **Auditor** | Org-wide | Read-only: view CRs, audit trail |

---

## Application Structure (Two Repositories)

| Repo | Tech | Description |
|------|------|-------------|
| `audita-api` | Java 25 + Spring Boot 4 | REST API, business logic, data layer |
| `audita-web` | Nuxt 3 + Vue 3 | SSR frontend, component library, state management |

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

The sprint plan delivered a **usable, end-to-end MVP** by Sprint 5, enabling:

1. Organisations to be provisioned.
2. Users to be invited and authenticated.
3. Change requests to be created, submitted, and approved.
4. Basic notification flow.

Advanced features (SLA, custom fields, audit export, full admin config, RBAC expansion, session hardening) delivered in Sprints 6–11.

---

## Current Blockers

- **No active implementation blockers.**
- **Production note:** Deploy latest `audita-web` image containing slug-precedence hotfix (`auth.global.ts`, `plugins/auth.ts`, `tenant.ts`) to stop post-refresh logout/login regression.

---

## Today's Work Summary (2026-05-25)

### CI Test Failures Fixed
- **`tests/server/api-proxy.spec.ts`**: Removed `"x-forwarded-host"` from `ALLOWED_HEADERS` in `apiProxy.ts` — test expected this spoofable header to be stripped.
- **`tests/middleware/tenant.spec.ts`**: Fixed tenant middleware (`middleware/tenant.ts`) to properly logout authenticated users when resolved tenant changes, matching test expectation.
- **Verification**: `pnpm test` in `audita-web` passed: `14/14` test files, `47/47` tests.

### Container Log Analysis + Flyway Idempotency Hardening
- Identified "Unknown tenant" pre-setup rejections as expected behavior for fresh containers.
- **Flyway migration warnings** (`already exists, skipping`) fixed:
  - `V3__create_groups.sql`: Changed from `CREATE TABLE IF NOT EXISTS groups` (which collided with V1) to `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` + `DO $$` constraint block.
  - Added PostgreSQL advisory lock (`pg_try_advisory_lock`) to `TenantMigrationStartupRunner` to prevent concurrent migrations across container replicas.
  - Added `audita.migration.startup.enabled` property (env var `MIGRATION_STARTUP_ENABLED`) for environments that want to disable startup migrations.

### Unified Tenant Baseline Migration (Public Launch Prep)
- **Consolidated V1-V10** into single `V1__create_tenant_schema.sql` — complete schema + seed data + all indexes in one script.
- **Deleted V2-V10** (`seed_roles_and_permissions`, `create_groups`, `add_refresh_token_context`, `add_audit_indexes`, `add_user_roles`, `add_idempotency_keys`, `add_audit_export_requests`, `ensure_refresh_tokens_table`, `repair_refresh_tokens_table`).
- **Rationale**: No existing production data to migrate. Fresh public launch benefits from clean single-script provisioning. Future schema changes follow as V2, V3, etc.
- **Files changed**: `V1__create_tenant_schema.sql` (rewritten), `TenantMigrationStartupRunner.java` (advisory lock + opt-out flag), `application.yml` (new property), 10 migration files deleted.
- **Verification**: Frontend `pnpm test` 47/47 pass. Backend `:api:test` and `:infrastructure:test` (tenant tests) pass.

### License Normalization — Switched to Apache 2.0
- Replaced custom source-available `LICENSE` (Commons Clause restriction) with canonical **Apache License 2.0** text.
- Updated `README.md` license section: now states "Apache 2.0" instead of "source-available / no-resale".
- Updated `CONTRIBUTING.md` with explicit inbound=outbound contributor licensing statement.
- Updated `LICENSE-APACHE` reference to point to canonical `LICENSE` without resale conditions.
- **Rationale:** User decided true open source (allowing commercial redistribution) aligns better with project goals.

### Social Media Launch Kit Prepared
- Generated platform-specific copy for **LinkedIn, Twitter/X, Reddit, Hacker News**.
- Tone: **playful/irreverent** (e.g., "Your change approval process is just a shared Google Sheet with extra steps").
- CTA: **Star the GitHub repo** (`github.com/skylarng89/audita`).
- Includes posting schedule, hashtag strategy, engagement tips, and asset pairing guide.
- Image generation was attempted but discarded; user will provide their own platform screenshots.
- All copy lives in `social-media-assets/README.md`.

---

## Next Actions

1. Deploy latest `audita-web` build to production.
2. Hard refresh browser/storage and verify login + refresh + settings-save flows remain authenticated.
3. Verify CI remains green on `pnpm test` for middleware/proxy specs.
4. Prepare v0.7.0 release candidate.
5. Execute social media launch using copy from `social-media-assets/README.md` (playful/irreverent tone, star-the-repo CTA).
