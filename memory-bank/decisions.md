# Audita — Architectural Decisions

**Format:** Append-only. New entries at the bottom.

---

## ADR-001: Schema-Per-Tenant Multi-Tenancy

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Use PostgreSQL schema-per-tenant isolation. Each organisation gets its own schema (e.g., `tenant_acme`). A shared `public` schema holds `tenants`, `super_admins`, `tenant_allowed_domains`, and `tenant_sso_configs`.

**Reasoning:**

- Strong data isolation — a bug or misconfiguration cannot leak data across tenants.
- Flyway can run per-schema migrations cleanly.
- Simpler than row-level-security (RLS) for the team's experience level with Spring/JPA.

**Trade-offs:**

- Schema proliferation at scale (thousands of tenants) requires careful Flyway migration performance management.
- Connection pooling must be handled per-tenant or via a pool-of-pools strategy.

**Reference:** SRS §2.2, §4.2

---

## ADR-002: Server-Sent Events (SSE) for Real-Time Notifications

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Use SSE (HTTP long-lived response) for real-time in-app notifications. No WebSocket.

**Reasoning:**

- Notifications are unidirectional server→client. SSE is sufficient.
- Simpler infra: no WebSocket upgrade, no sticky-session concerns at the protocol level (though SSE connections still require sticky routing to the same instance).
- Java virtual threads (Project Loom in Java 25) handle many concurrent SSE connections cheaply.
- Notifications are persisted in the DB; reconnect replay is trivial.

**Trade-offs:**

- SSE connections do require load balancer sticky routing or a shared event bus (Redis Pub/Sub) if horizontal scaling is needed.
- Mitigation in v1: single-instance deployment assumed; horizontal scaling can add Redis Pub/Sub as a follow-up.

**Reference:** SRS §4.9, §7.2

---

## ADR-003: TipTap for Rich Text Editing

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Use TipTap (ProseMirror-based) for CR descriptions and comments in the Nuxt frontend.

**Reasoning:**

- Provides a headless, composable editor that integrates cleanly with Vue 3 / Nuxt.
- Supports all required extensions: StarterKit, Link, Image, Mention, Placeholder.
- `@mention` autocomplete requires a custom backend endpoint for user search; TipTap's Mention extension makes this straightforward.

**Trade-offs:**

- HTML output requires mandatory server-side sanitisation (OWASP Java HTML Sanitizer) before persistence to prevent XSS.
- File images embedded in rich text must be uploaded via the attachments API, not base64 inline.

**Reference:** SRS §4.7, §9.3

---

## ADR-004: Hexagonal Architecture for Backend

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Structure `audita-api` as a hexagonal (ports & adapters) application with four main packages: `domain`, `application`, `infrastructure`, `api`.

**Reasoning:**

- Keeps business logic (change request state machine, approval rules, SLA evaluation) pure and testable without Spring context.
- Infrastructure concerns (JPA, email, file storage, SSE) are plug-replaceable.
- Aligns with the AGENTS.md principle of single responsibility and dependency inversion.

**Reference:** SRS §7.4

---

## ADR-005: JWT + HttpOnly Cookie Refresh Token

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Access tokens are short-lived JWTs (15 min) sent in `Authorization: Bearer` header. Refresh tokens are long-lived (7 days), stored in an HttpOnly `SameSite=Strict` cookie.

**Reasoning:**

- Separating access token (header) from refresh token (cookie) means JavaScript cannot read the refresh token — CSRF is mitigated by `SameSite=Strict`.
- Short-lived access tokens limit the blast radius of a token leak.
- Refresh token rotation on every use invalidates stolen tokens quickly.

**Reference:** SRS §4.1, §6 (SEC-04)

---

## ADR-006: Flyway for Per-Tenant Schema Migrations

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Flyway manages both the `public` schema (platform-level) and each tenant schema. Tenant migrations run at tenant creation time and on application startup for all existing tenants.

**Reasoning:**

- Versioned, idempotent migrations are essential for a self-hosted product where customers upgrade on their own schedule.
- Flyway's Java-based migration callbacks allow dynamic schema routing per tenant.

**Trade-offs:**

- Startup time grows linearly with tenant count. Mitigation: run tenant migrations in parallel on startup.

---

## ADR-007: AES-256 Encryption for Sensitive Stored Values

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** SSO client secrets and SMTP passwords are AES-256 encrypted before storage in the database. Encryption key is provided via `APP_ENCRYPTION_KEY` environment variable.

**Reasoning:**

- If the database is compromised, encrypted secrets are useless without the application key.
- Satisfies PCI-DSS v4.0 data-at-rest encryption requirements (relevant for enterprise customers).

**Reference:** SRS §6 (SEC-08, SEC-14)

---

## ADR-008: Comments Are Immutable in v1

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Comments cannot be edited or deleted after posting in v1.

**Reasoning:**

- Preserves audit trail integrity — immutable comment history is essential for ITIL compliance.
- Simplifies the data model (no edit history needed).
- Consistent with the append-only audit log principle.

**Reference:** PRD §3.6, SRS §4.7 (COM-04)

---

## ADR-009: Approval Workflow State Machine

**Date:** 2026-04-23
**Status:** Accepted

**Decision:** Change request approval logic follows explicit closure rules:

- **Approval:** ALL required approvers must have status `APPROVED`.
- **Rejection:** If there is only one required approver and they reject → immediately `REJECTED`. If multiple required approvers exist → `REJECTED` only when ALL required approvers have rejected.
- `approval_locked = TRUE` after the first approver decision. Approval type (Linear/Non-linear) cannot change after this point.

**Reasoning:**

- These rules are explicitly defined in the PRD and SRS and represent the business decision by the product owner.
- The "all required must reject" rule for multi-approver scenarios prevents a single rogue rejection from blocking a well-supported change.

**Reference:** PRD §3.5, SRS §4.6 (WF-08, WF-09, WF-10)

---

## ADR-010: Internal Nuxt API Proxy Must Strip Browser Origin Headers

**Date:** 2026-04-29
**Status:** Accepted

**Decision:** In the internal Nuxt server route proxy (`audita-web/server/routes/api/[...path].ts`), remove `Origin`, `Referer`, and `Host` headers before forwarding requests to the upstream Spring API.

**Reasoning:**

- Browser-originated bootstrap POSTs proxied through Nuxt returned `403 Invalid CORS request` while direct CLI POSTs returned 200.
- API instrumentation showed bootstrap status GET logs but no bootstrap POST handling in controller/filter paths during failure windows, indicating edge rejection before business logic.
- Controlled browser fetch repro confirmed the exact 403 body (`Invalid CORS request`).
- Removing forwarded browser-origin headers on the internal proxy hop prevents upstream CORS misclassification for same-origin app traffic.

**Trade-offs:**

- Upstream request logs no longer include raw browser `Origin`/`Referer` values for proxied internal calls.
- If origin-specific audit requirements are introduced later, logging should be handled at Nuxt edge before header stripping.

**Validation:**

- Browser fetch to `/api/platform/v1/bootstrap` changed from `403 Invalid CORS request` to `200` with success payload.
- Onboarding state transitioned to `onboardingCompleted=true` and bootstrap page redirected to sign-in.

---

## ADR-011: All JPA Entities Require Explicit @Column(name=...) on camelCase Fields

**Date:** 2026-04-29
**Status:** Accepted

**Decision:** Every camelCase field in every JPA entity that maps to a snake_case DB column must have an explicit `@Column(name = "snake_case_name")` annotation. Relying on Hibernate's automatic naming strategy is unsafe in this codebase.

**Reasoning:**

- `JpaConfig` manually constructs a `LocalContainerEntityManagerFactoryBean` to enable multi-tenant schema routing. This completely bypasses `spring.jpa.hibernate.naming.*` properties in `application.yml`.
- The `CamelCaseToUnderscoresNamingStrategy` is added directly to `JpaConfig.hibernateProperties()` as an instance, but entity fields without explicit `@Column` annotations have been caught mapping to incorrect column names (`tokenhash`, `expiresat`, `issystem`, `createdat`, etc.) in practice.
- Explicit `@Column(name=...)` serves as belt-and-suspenders and makes the mapping unambiguous to the next developer.

**Affected entities (fixed):** `RoleEntity`, `UserEntity`, `InviteTokenEntity`, `RefreshTokenEntity`, `GroupEntity`, `PasswordResetTokenEntity`.

**Rule for new entities:** Any field that would differ under camelCase → snake_case conversion must have `@Column(name = "actual_column_name")`. Fields matching exactly (e.g., `id`, `name`, `email`, `status`) may omit it.

**Trade-offs:**

- Slightly more verbose entities.
- Eliminates an entire class of hard-to-debug 500 errors that only manifest at runtime.

---

## ADR-012: Normalize Security Role Authorities at Principal Construction

**Date:** 2026-04-29
**Status:** Accepted

**Decision:** Normalize tenant user role names in `UserPrincipal` before building authorities (uppercase + underscore canonicalization), then emit Spring-compatible role authorities consistently.

**Reasoning:**

- Method guards using `hasRole`/`hasAnyRole` rely on canonical role names; mixed-case or spaced role strings produced false 403 denials despite valid users.
- Centralizing normalization in principal construction prevents repeated ad-hoc normalization in controllers/services.

**Trade-offs:**

- Introduces a default/fallback role behavior when source role is missing or malformed.
- Requires role naming expectations to stay aligned with seeded role names.

**Validation:**

- Previously failing authorized flows returned expected responses after principal normalization.

---

## ADR-013: Initialize Lazy Relations in Service Read Paths Before API DTO Mapping

**Date:** 2026-04-29
**Status:** Accepted

**Decision:** For change request read endpoints, initialize required lazy relations (`createdBy`) inside the transactional service boundary before entities are returned for API-layer DTO mapping.

**Reasoning:**

- DTO mapping in API layer accessed `createdBy` after service return, causing `LazyInitializationException` and 500 errors on CR detail retrieval after creation.
- Read-path initialization is a low-risk fix that preserves existing API contracts and repository signatures.

**Trade-offs:**

- Relies on explicit relation initialization calls that must be kept in sync with DTO fields.
- A future projection/fetch-join strategy may provide a cleaner long-term read model.

**Validation:**

- CR create → redirect → detail no longer fails with lazy-loading 500.
