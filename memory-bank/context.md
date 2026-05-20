# Audita — Active Context

**Last Updated:** 2026-05-20
**Current Phase:** Sprint 12 complete — v0.6.0 Released + Post-Release Fix
**Active Sprint:** None — All development complete

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
| **TOTAL** | **181/181** | **181** | — | — |

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

---

## Remaining Open Tasks (Sprint 12 — Launch Readiness)

| Task ID | Task | Priority | Status | Owner |
|---------|------|----------|--------|-------|
| UX10-006 | Align `AppButton.vue` component with CSS token system | High | ✅ Completed | Developer 2 |
| UX10-008 | Wire CR list pagination to shared `AppPagination` component | Medium | ✅ Completed | Developer 2 |
| WCAG-010 | Ensure all interactive targets meet 24×24 px minimum | Medium | ✅ Completed | Developer 2 |

---

## Quality Gates

- **Backend tests**: 62/62 passing (AllSprintsE2ETest + critical flows + security regression + controller tests)
- **Frontend gates**: `pnpm test`, `pnpm -s nuxi typecheck`, `pnpm build` all passing
- **Docker**: Full Compose stack operational (PostgreSQL 17, MailHog, API, Web)
- **Security hardening**: SEC-001 through SEC-004 complete, with refinements
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

- **No active blockers.** All 181 tasks complete. v0.6.0 released.
- **Post-release fix applied (2026-05-20)**: SmartLifecycle tenant migration runner + defensive SLA monitoring. Prevents race condition where scheduled jobs run before startup migrations complete.
- **All quality gates green**: backend tests 62/62, frontend build/typecheck/test passing, Sonar clean, Playwright smoke test passing.

---

## Next Actions

1. ~~Close remaining 3 open tasks (UX10-006, UX10-008, WCAG-010).~~ ✅ Completed.
2. ~~Run Sonar scan and dependency audit; resolve any new findings.~~ ✅ Completed — zero critical/blocker issues.
3. ~~Add Playwright smoke test for critical end-to-end login → create CR → approve flow.~~ ✅ Completed.
4. ~~Cut `v0.6.0` release tag once final tasks close.~~ ✅ Released.
5. ~~Monitor production metrics and gather user feedback for v0.7.0 planning.~~ In progress.
6. **Post-release fix (2026-05-20)**: SmartLifecycle tenant migration runner ensures pending migrations apply before scheduled jobs start. Defensive catch in `SlaMonitoringService` prevents ERROR spam for temporarily missing schema tables.
