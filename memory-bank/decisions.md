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

---

## ADR-023: Switch to Apache License 2.0 (True Open Source)

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Replace the custom "Audita Source-Available License" (Apache 2.0 base + Commons Clause no-resale condition) with the canonical Apache License 2.0 text. Remove all resale and managed-service restrictions.

**Reasoning:**

- The user initially wanted source-available terms with a no-resale clause but learned that "open source" (OSI definition) requires freedom to redistribute commercially.
- After discussion, the user decided to embrace true open source to maximize adoption, community contributions, and ecosystem growth.
- Apache 2.0 provides a patent grant, clear commercial-use permissions, and is the industry standard for enterprise-friendly open source.

**Trade-offs:**

- Competitors or resellers may technically redistribute or sell Audita as a service under Apache 2.0 terms.
- Mitigation: build a strong brand, community, and hosted offering so the official project remains the preferred source.

**Files changed:**

- `LICENSE` — canonical Apache 2.0 text (201 lines).
- `README.md` — license section now says "Apache 2.0" and removes resale language.
- `CONTRIBUTING.md` — added inbound=outbound statement.
- `LICENSE-APACHE` — updated to reference canonical `LICENSE` without resale conditions.

---

## ADR-024: Social Media Launch — Playful/Irreverent Tone, Star-the-Repo CTA

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Publicize Audita using a playful/irreverent tone across LinkedIn, Twitter/X, Reddit, and Hacker News. Primary CTA is starring the GitHub repo. User provides own platform screenshots; no auto-generated image assets retained.

**Reasoning:**

- ITSM is traditionally perceived as dry/enterprise-y; an irreverent tone differentiates Audita and captures attention.
- "Star the repo" is the simplest, lowest-friction CTA for developer audiences.
- Auto-generated image assets (Pillow-based composites) were deemed inadequate; real platform screenshots will be more authentic and higher quality.

**Tone examples:**

- LinkedIn: "Your change approval process is just a shared Google Sheet with extra steps."
- Twitter/X: "2026 and we're still emailing spreadsheets for change approval."
- Reddit/HN: "[Show HN] Audita — Open-source ITIL change management that doesn't suck."

**Launch kit location:** `social-media-assets/README.md`

---

## ADR-025: Unified Tenant Baseline Migration for Public Launch

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Consolidate the 10 incremental tenant Flyway migrations (V1-V10) into a single unified `V1__create_tenant_schema.sql` baseline. Delete V2-V10. Future schema changes continue as V2, V3, etc.

**Reasoning:**

- No production data exists yet; this is the ideal window to establish a clean baseline before public launch.
- New tenant provisioning was running 10 scripts with many "already exists, skipping" warnings due to overlapping CREATE TABLE statements.
- A single script is faster, cleaner in logs, and easier for operators to read/audit.
- Future migrations (V2+) will be true deltas applied only when the schema evolves, maintaining Flyway's versioned migration model.

**Trade-offs:**

- Cannot run old migrations against existing schemas (but no existing production schemas exist).
- Any future schema change must be added as a new VN+1 script, not folded back into V1.

**Files changed:**

- `V1__create_tenant_schema.sql` — rewritten with complete schema (all V1-V10 tables, columns, constraints, indexes, seed data)
- Deleted: `V2__seed_roles_and_permissions.sql`, `V3__create_groups.sql`, `V4__add_refresh_token_context.sql`, `V5__add_audit_indexes.sql`, `V6__add_user_roles.sql`, `V7__add_idempotency_keys.sql`, `V8__add_audit_export_requests.sql`, `V9__ensure_refresh_tokens_table.sql`, `V10__repair_refresh_tokens_table.sql`

**Validation:**

- `pnpm test` frontend: 47/47 pass
- `./gradlew :api:test` backend: pass
- `./gradlew :infrastructure:test` (tenant tests): pass

---

## ADR-026: PostgreSQL Advisory Lock for Multi-Replica Tenant Migrations

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Use `pg_try_advisory_lock()` in `TenantMigrationStartupRunner` to ensure only one container replica runs tenant schema migrations on startup.

**Reasoning:**

- In replicated/containerized deployments, multiple API instances start simultaneously and could race to run the same Flyway migrations.
- PostgreSQL advisory locks are connection-scoped and auto-release on disconnect, making them safe even if a JVM crashes.
- Replicas that fail to acquire the lock log a message and skip migration, proceeding with startup.

**Trade-offs:**

- Adds a small dependency on PostgreSQL-specific SQL (not portable to other databases).
- Lock acquisition failure logs a warning but does not block startup.

**Files changed:**

- `TenantMigrationStartupRunner.java` — added `acquireLock()` / `releaseLock()` methods
- `application.yml` — added `audita.migration.startup.enabled` property

---

## ADR-027: API Proxy Header Allowlist Must Strip Client-Spoofable Forwarding Headers

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Remove `x-forwarded-host` from the Nuxt API proxy's `ALLOWED_HEADERS` allowlist. Test asserts that `x-forwarded-host` is stripped before forwarding to the upstream Spring API.

**Reasoning:**

- `x-forwarded-host` is a client-sent header that can be spoofed to manipulate upstream tenant resolution or routing logic.
- The Nuxt proxy's purpose is to forward safe, necessary headers only; host-level forwarding headers should be set by the edge reverse proxy (Nginx), not passed through from the browser.
- The test `tests/server/api-proxy.spec.ts` explicitly validates this security boundary.

**Trade-offs:**

- Nuxt server logs no longer show the original `x-forwarded-host` value for proxied internal calls (intentional).

**Files changed:**

- `audita-web/server/utils/apiProxy.ts` — removed `"x-forwarded-host"` from `ALLOWED_HEADERS`
- `tests/server/api-proxy.spec.ts` — asserts `sanitized["x-forwarded-host"]` is `undefined`

---

## ADR-028: Tenant Middleware Must Logout on Tenant Mismatch for Authenticated Users

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** The tenant route middleware (`audita-web/middleware/tenant.ts`) resolves the active tenant from the current hostname/subdomain. If the user is already authenticated and the resolved tenant differs from their stored `tenantSlug`, the middleware calls `auth.logout()` to prevent cross-tenant session reuse.

**Reasoning:**

- Without this guard, a user authenticated with tenant A could navigate to tenant B's subdomain and potentially operate within the wrong tenant context.
- The logout forces re-authentication against the correct tenant, maintaining tenant isolation boundaries.
- This behavior is explicitly tested in `tests/middleware/tenant.spec.ts`.

**Trade-offs:**

- Users switching between subdomains (e.g., acme.audita.io → beta.audita.io) will be logged out even if they have access to both — acceptable for v1.
- Subdomain resolution must be accurate; incorrect resolution causes spurious logouts.

**Files changed:**

- `audita-web/middleware/tenant.ts` — resolve tenant before auth check; call `logout()` on mismatch
- `tests/middleware/tenant.spec.ts` — asserts `logout` is called once on tenant change

---

## ADR-022: Subdomain-Based Tenant Resolution with X-Forwarded-Host

**Date:** 2026-05-25
**Status:** Accepted

**Decision:** Tenant resolution now uses `X-Forwarded-Host` subdomain as the primary mechanism, with `X-Tenant-Slug` header as fallback. A `subdomain` column is stored on the `tenants` table and populated during setup from the browser hostname.

**Reasoning:**

- The previous approach derived tenant slug from subdomain client-side (`cm.mypixelpay.com` → `cm`), but the tenant slug was `pixelpay-systems-limited`. The mismatch caused 403 `Unknown tenant` errors.
- `X-Forwarded-Host` is set by Nginx (server-side, not spoofable by clients), making it a trustable resolution source.
- The `TenantResolutionFilter` now resolves in order: (1) `X-Forwarded-Host` subdomain → `tenants.subdomain` lookup, (2) `X-Tenant-Slug` header → `tenants.slug` lookup.
- Subdomain is captured during setup from `window.location.hostname` and stored in the database, creating an explicit mapping.
- The frontend middleware no longer force-logs-out on slug mismatch — the backend resolves the correct tenant regardless of what the frontend sends.

**Trade-offs:**

- Requires Nginx to set `proxy_set_header X-Forwarded-Host $host;` (already in our recommended config).
- Requires the Nuxt proxy to forward `x-forwarded-host` to the API (added to `ALLOWED_HEADERS`).
- Existing deployments must run the V2 Flyway migration and backfill the `subdomain` column, or rerun setup fresh.

**Validation:**

- Backend compilation and all existing tests pass.
- `TenantSettingsControllerWebMvcTest`, `TenantResolutionFilterTest`, `TenantServiceSettingsTest` all green.
- Pre-existing `AuthServiceTest` failures confirmed unrelated to this change.

---

## ADR-021: Docker Build Must Include `pnpm-workspace.yaml` Before Install

**Date:** 2026-05-24
**Status:** Accepted

**Decision:** In `audita-web` Docker build stages, copy `pnpm-workspace.yaml` together with `package.json` and `pnpm-lock.yaml` before running `pnpm install`.

**Reasoning:**

- Supply-chain policy settings (`minimumReleaseAgeExclude`, `allowBuilds`, `supportedArchitectures`) are resolved from workspace-level config, not `package.json#pnpm` in current pnpm.
- Omitting `pnpm-workspace.yaml` in early image layers caused lockfile policy verification to fail under CI/containerized builds despite passing locally.
- Keeping policy files in install layer ensures deterministic install behavior between local development, Docker image builds, and GitHub Actions scan jobs.

**Trade-offs:**

- Slightly broader cache invalidation for dependency layers when workspace policy file changes.
- Requires Dockerfile maintenance discipline when pnpm policy files are added/renamed.

**Validation:**

- `docker build -t audita-web:scan -f audita-web/Dockerfile audita-web` passes lockfile verification and full production build.
- Previous `ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION` failure in web builder install step is eliminated.

---

## ADR-022: Security Audit Remediation Strategy (Sprint 14)

**Date:** 2026-05-31
**Status:** Accepted

**Decision:** Address 52 audit findings via a prioritized 21-task sprint organized into 5 phases: Critical Security → High Auth → High Infrastructure → Medium Security → Performance & Architecture. All 3 critical findings are deployment blockers for v0.7.0.

**Reasoning:**

- Critical findings (unauthenticated setup, XSS, open redirect) are exploitable in production and must be resolved before any public release.
- High-severity auth issues (missing rate limits, JWT irrevocability, SSO bypass) are attack surface that must close before HA deployment.
- Infrastructure hardening (Docker port binding, healthchecks, DDL auto-update) prevents production misconfiguration.
- Medium-severity items (N+1 queries, CSP, tenant isolation) improve defense-in-depth and scalability.
- Component decomposition included to prevent further monolithic growth.

**Trade-offs:**

- Redis migration (SA14-016) deferred to Phase D — current single-instance deployment makes in-memory stores acceptable. Must be completed before horizontal scaling.
- Component decomposition (SA14-021) is large-scope — may spill into a follow-up sprint if time-constrained.
- 21 tasks is ambitious for one sprint — phases are ordered by priority so partial completion still delivers maximum security value.

**Reference:** `memory-bank/docs/security-audit-2026-05-31.md`
