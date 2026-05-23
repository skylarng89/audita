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

---

## ADR-014: Adopt Tailwind v4 Native Vite Integration in Nuxt

**Date:** 2026-05-04
**Status:** Accepted

**Decision:** Use Tailwind v4 with official Vite plugin integration (`@tailwindcss/vite`) in Nuxt, and stop using `@nuxtjs/tailwindcss` module integration for this project.

**Reasoning:**

- Build failures indicated legacy Tailwind integration path mismatch (`tailwindcss` being treated as old PostCSS plugin wiring).
- Tailwind v4 expects the native plugin pipeline and stricter CSS composition behavior.
- The v4 path aligns with current Nuxt/Vite toolchain behavior and reduces integration drift.

**Trade-offs:**

- Required CSS refactor where custom classes were chained via `@apply` (unsupported in v4).
- Migration currently uses compatibility mode (`@config` + JS config), with future opportunity to migrate fully to v4-native theme variables.

**Validation:**

- Frontend verification gates passed after migration: `pnpm test`, `pnpm -s nuxi typecheck`, and `pnpm build`.

---

## ADR-015: Use `.trivyignore` for Non-Exploitable Base Image Vulnerabilities

**Date:** 2026-05-22
**Status:** Accepted

**Decision:** Use `.trivyignore` to suppress Trivy-detected vulnerabilities in the Node.js base image that are not exploitable in our application context, rather than upgrading the base image or restructuring the build.

**Reasoning:**

- CVE-2026-33671 (picomatch ReDoS) was detected in `usr/local/lib/node_modules/npm/node_modules/picomatch/package.json` — this is part of the Node.js base image's bundled npm, not our application dependencies.
- picomatch is only used internally by npm during package installation (file globbing), never exposed to user input or runtime application code.
- Upgrading the Node.js base image to get a patched npm would require testing the entire build pipeline and could introduce other compatibility issues.
- `.trivyignore` with documented rationale is the standard Trivy mechanism for acknowledging non-exploitable findings.

**Trade-offs:**

- Requires periodic review of ignored CVEs to ensure they remain non-exploitable.
- Should be revisited when upgrading the Node.js base image in a future release.

**Rule for new entries:** Only ignore CVEs where: (1) the vulnerable package is in the base image or build tooling, not application dependencies, AND (2) the attack vector is not reachable from our application code. Document the rationale in the `.trivyignore` comment.

---

## ADR-016: Approvers Default to Optional with Post-Save Toggle

**Date:** 2026-05-22
**Status:** Accepted

**Decision:** New approvers default to Optional (not Required). Each saved approver has an inline Required/Optional toggle button that calls `PATCH /{id}/approvers/{approverId}/requirement` immediately.

**Reasoning:**

- User feedback indicated that requiring explicit opt-in for Required status is more intuitive than requiring explicit opt-out.
- Post-save toggle allows flexibility — approvers can be promoted to Required after initial assignment without removing and re-adding.
- Dirty tracking with snapshot-based change detection provides clear feedback when changes are made.

**Trade-offs:**

- Requires an additional backend endpoint (`updateApproverRequirement`) instead of bundling requirement changes with other approver mutations.
- Each toggle is an immediate API call (no batch save for requirement changes) — acceptable for the expected low frequency of this operation.

**Validation:**
- Frontend verification gates passed: `pnpm test`, `pnpm -s nuxi typecheck`, `pnpm build`.
- Backend compile passed: `./gradlew :api:compileJava :infrastructure:compileJava --no-daemon`.

---

## ADR-017: Disable Nuxt `xssValidator` on Internal `/api/**` Proxy Routes

**Date:** 2026-05-22
**Status:** Accepted

**Decision:** Disable `nuxt-security` `xssValidator` for internal `/api/**` proxy routes in `audita-web/nuxt.config.ts` route rules.

**Reasoning:**

- TipTap mention markup (`<span class="mention" data-* ...>`) in JSON request bodies triggered Nuxt edge-side XSS mutation detection, causing false-positive `400 Bad Request` before requests reached Spring API.
- Backend already performs authoritative sanitization using OWASP Java HTML Sanitizer before persistence.
- Allowing rich-text JSON payloads through the proxy avoids duplicate sanitization semantics and prevents proxy/API policy mismatch.

**Trade-offs:**

- Removes one edge-layer request-body validator for proxied API paths.
- Security responsibility for payload sanitization is explicitly concentrated in backend service layer.

**Validation:**

- Mention comment POST requests now reach API path instead of failing at Nuxt edge with 400.
- Frontend build remains green after route-rule override.

---

## ADR-018: Preserve TipTap Mention Metadata in Comment Sanitization Policy

**Date:** 2026-05-22
**Status:** Accepted

**Decision:** Extend `CommentService` HTML policy to allow `span` with mention metadata attributes (`class`, `data-type`, `data-id`, `data-label`, `data-mention-suggestion-char`).

**Reasoning:**

- Mention rendering and downstream mention-aware UX rely on structured span metadata emitted by TipTap Mention extension.
- Generic formatting/link policies do not include this metadata by default.
- Explicit allowlisting keeps sanitizer deterministic while preserving required mention semantics.

**Trade-offs:**

- Slightly broader allowlist surface in comment HTML sanitizer.
- Requires policy updates if mention extension attribute contract changes.

**Validation:**

- `CommentServiceTest` passes with mention-email flow and comment persistence path.
- Rich-text comments with mention spans persist and render without proxy/API rejection.

---

## ADR-019: Hardened Runtime Images Must Not Depend on Shell or Curl Healthchecks

**Date:** 2026-05-23
**Status:** Accepted

**Decision:** For DHI hardened runtime images, avoid shell/package-manager/runtime mutation assumptions and remove in-container healthchecks that require `curl` or `/bin/sh`.

**Reasoning:**

- DHI runtime images are intentionally minimal and may not include `/bin/sh`, `apt-get`, or `curl`.
- Runtime-stage `RUN` or shell-form healthchecks fail under hardened images and create false negatives unrelated to application health.
- Numeric non-root ownership (`COPY --chown=<uid>:<gid>`) keeps runtime deterministic and compatible with distroless/hardened bases.

**Trade-offs:**

- Loses convenient in-container `curl` health probe pattern in compose files.
- Health/readiness validation should move to app endpoints from external probes/orchestrator-native checks.

**Validation:**

- `docker compose -f docker-compose.local.yml build api` succeeds with hardened image.
- `docker compose -f docker-compose.local.yml up -d --build` brings up `api` and `web`.
- API actuator endpoint returns HTTP 200 at `http://localhost:7080/actuator/health`.

---

## ADR-020: Approver Mutations Allowed in `PENDING_APPROVAL` with Vote-Safe Removal

**Date:** 2026-05-23
**Status:** Accepted

**Decision:** Allow approver list mutations (add/remove/reorder/requirement update) while CR is open (`DRAFT` or `PENDING_APPROVAL`), but prohibit removal of any approver who has already voted (`APPROVED` or `REJECTED`).

**Reasoning:**

- Operationally, change workflows need flexibility to adjust approver routing after submission without cancelling and recreating CRs.
- Preserving vote integrity is mandatory: once an approver has cast a decision, removing that approver would invalidate historical approval semantics.
- Existing activity events already model approver mutations; extending this to `PENDING_APPROVAL` is low-risk when combined with strict voted-removal guard.

**Trade-offs:**

- Workflow becomes more dynamic, increasing responsibility for clear activity/audit observability.
- Reordering during pending approvals can affect human expectation of sequence in linear flows, but this is acceptable with explicit event logging.

**Validation:**

- Backend regression tests pass for pending-approval removal allow/deny paths, including `APPROVER_DECISION_LOCKED`.
- Frontend UI enforces row-level remove disable for voted approvers with explicit reason text.
- Audit trail now records approver add/group-add/remove/reorder/requirement-change actions.
