# Audita — Active Context

**Last Updated:** 2026-04-23
**Current Phase:** Pre-development — Project initialisation & planning
**Active Sprint:** Sprint 0 — Foundation & Scaffolding

---

## What Is Audita?

Audita is a **self-hosted, multi-tenant ITIL/ITSM Change Management platform**. It enables organisations to manage the full lifecycle of IT change requests: creation, structured approval workflows, real-time collaboration, SLA tracking, and a complete immutable audit trail.

**Mission:** Make change management simple, transparent, and auditable for every team — from SMEs to enterprises.

---

## Current State

- No code written yet — project is at the planning and memory-bank initialisation stage.
- Documentation complete: PRD v1.0, SRS v1.0, USER_FLOW v1.0 (all `docs/` folder).
- UI designs complete: 40 screens across light/dark modes covering all major user journeys (all `ui-designs/` folder).
- Memory bank initialised (this session: 2026-04-23).

---

## Five User Personas

| Role | Scope | Key Capability |
|---|---|---|
| **Super Admin** | Platform-wide | Manage tenants, domain whitelists, SSO per org |
| **Admin** | Org-wide | Configure workflows, manage users/roles/groups, SLA, custom fields |
| **Requester** | Org-wide | Create and track change requests |
| **Approver** | Org-wide | Review and action change requests |
| **Auditor** | Org-wide | Read-only: view CRs, audit trail |

---

## Application Structure (Two Repositories)

| Repo | Tech | Description |
|---|---|---|
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

The sprint plan is structured to deliver a **usable, end-to-end MVP** as early as Sprint 4/5, enabling:
1. Organisations to be provisioned.
2. Users to be invited and authenticated.
3. Change requests to be created, submitted, and approved.
4. Basic notification flow.

Advanced features (SLA, custom fields, audit export, full admin config) follow in subsequent sprints.

---

## Active Blockers / Open Questions

- None at this time — documentation is comprehensive and clear.

---

## Next Actions

1. Scaffold `audita-api` — Gradle project, Spring Boot 4, Flyway, Docker Compose.
2. Scaffold `audita-web` — Nuxt 3 project, Tailwind CSS, Pinia, component library baseline.
3. Implement public schema migrations (tenants, super_admins, sso_configs, allowed_domains).
4. Implement authentication module (local login, JWT, refresh token rotation).
