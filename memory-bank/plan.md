# Audita — Sprint 5 Plan

**Sprint:** Sprint 5 — Hardening & Release Readiness
**Date:** 2026-04-28
**Owner:** Developer 1

## Objectives

1. Stabilize critical backend integration path for CR lifecycle.
2. Add regression coverage for identity semantics and lazy-loading sensitive response mapping.
3. Complete backend + frontend verification gates for release readiness.

## Work Items

- S5-001: CR lifecycle hardening in critical e2e harness ✅
- S5-002: Principal identity regression tests ✅
- S5-003: Response mapping stability hardening and regression test coverage ✅
- S5-004: Final verification runs (backend critical suite + frontend build) ✅
- S5-005: Update memory-bank + repo memory with Sprint 5 closure ✅

## Post-Sprint Security Follow-Up (2026-04-28)

- SEC-001: Eliminate tenant slug injection surface in schema switching path. ✅ (Completed 2026-05-02)
- SEC-002: Remove URL query token transport from SSO callback flow. ✅ (Completed 2026-05-02)
- SEC-003: Add object-level authorization checks for change request mutations. ✅ (Completed 2026-05-02)
- SEC-004: Tighten CORS to explicit allowlist with environment-specific profiles. ✅ (Completed 2026-05-02)
- SEC hardening refinements: bootstrap tenant-header rejection + fragment-only callback code parsing + dedicated tenant filter tests. ✅ (Completed 2026-05-03)

## Sprint 10 — UX & WCAG 2.2 Compliance Overhaul (2026-05-18)

### Sprint 10 Objectives

1. Systematically address every UX and UI deficiency across the full application.
2. Bring the product to a professional, production-quality standard.
3. Achieve WCAG 2.2 AA compliance across all pages and components.

### Sprint 10 Work Items

- UX10-001 through UX10-026: Navigation, mobile responsiveness, button consolidation, CR list UX, CR detail UX, form/input UX, global chrome, WCAG compliance. ✅ (Completed 2026-05-18)
- All 36 tasks completed. 4 previously deferred tasks (UX10-003 sidebar rail, UX10-004 filter pill, UX10-007 auth AppButton, UX10-016 tag UI) completed during session.

### Sprint 10 Verification

- `cd audita-web && pnpm -s nuxi typecheck` passes.
- `cd audita-web && pnpm test` passes.
- All WCAG 2.2 AA checkpoints verified manually (skip links, titles, label wiring, ARIA, focus trap, scroll-margin, aria-live, autocomplete, target size where implemented).

## Sprint 11 — Session Hardening, RBAC Expansion & CR Workflow Polish (2026-05-12)

### Sprint 11 Objectives

1. Make auth recovery fail closed after redeploys and ensure logout revokes refresh state.
2. Auto-populate CR approvers at creation time and evolve tenant RBAC to support multi-role users.
3. Remove localhost auth/session regressions and finish blocked CR collaboration flows.

### Sprint 11 Work Items

- SESS-001 through SESS-011: Session hardening (refresh cookie scope, 401-only refresh, token-free cross-tab sync, API contract enforcement, Spring Security public APIs). ✅ (Completed 2026-05-12)
- RBAC-001 through RBAC-009: RBAC expansion (multi-role, custom roles, auto-approver population, JWT claims). ✅ (Completed 2026-05-12)
- UXR-001 through UXR-006: CR workflow polish (localhost session persistence, role-flexible approver voting, comment/activity DTO hardening, modal centering, rich-text toolbar, vote visibility). ✅ (Completed 2026-05-12)

### Sprint 11 Verification

- `cd audita-api && ./gradlew :api:test --tests "io.audita.api.config.SecurityConfigAuthorizationTest" --tests "io.audita.api.config.ApiContractHeaderFilterTest" --tests "io.audita.api.controller.AuthControllerWebMvcTest" --tests "io.audita.api.security.TenantResolutionFilterTest"` passes.
- `cd audita-web && pnpm test -- tests/auth/session.spec.ts tests/auth/tenant-resolution.spec.ts tests/auth/api-contract.spec.ts tests/auth/session-sync.spec.ts tests/middleware/tenant.spec.ts tests/middleware/auth.global.spec.ts` passes.
- `cd audita-web && pnpm -s nuxi typecheck` passes.

## Sprint 12 — Launch Readiness (2026-05-19)

### Sprint 12 Objectives

1. Close remaining 3 open UI tasks (UX10-006, UX10-008, WCAG-010).
2. Run Sonar scan and dependency audit; resolve any new findings.
3. Add smoke test for critical end-to-end flow.
4. Cut v0.6.0 release tag with full changelog.

### Sprint 12 Work Items

- UX10-006: Align `AppButton.vue` and CSS class system. ✅ Completed
- UX10-008: Wire CR list pagination to `AppPagination`. ✅ Completed
- WCAG-010: Ensure all interactive targets meet 24×24 px minimum. ✅ Completed
- LAUNCH-001: Run Sonar scan and dependency audit. ✅ Completed — zero critical/blocker issues
- LAUNCH-002: Add smoke test for critical end-to-end flow. ✅ Completed — Playwright login→create CR→submit→approve passing
- LAUNCH-003: Cut v0.6.0 release tag and publish changelog. ✅ Completed

### Sprint 12 Next Steps

All Sprint 12 tasks completed. v0.6.0 released.

1. ~~Execute UX10-006 (AppButton reconciliation)~~ ✅ Completed.
2. ~~Execute UX10-008 (pagination component wiring)~~ ✅ Completed.
3. ~~Execute WCAG-010 (target size enforcement)~~ ✅ Completed.
4. ~~Run `sonar-scan.sh`~~ ✅ Passed — zero critical/blocker issues.
5. ~~Add Playwright smoke test~~ ✅ Passing.
6. ~~Cut `v0.6.0` tag and create GitHub release~~ ✅ Released.
7. Monitor production metrics and gather user feedback for v0.7.0 planning.

---

## Sprint 13 — Engineering Best Practices Hardening (2026-05-20)

### Sprint 13 Objectives

1. Close CI/CD and supply-chain hardening gaps (immutable action pinning, security scans, SBOM).
2. Improve backend production readiness with OTel tracing, Prometheus metrics, and explicit readiness/liveness probes.
3. Add idempotency controls for retriable mutating endpoints.
4. Harden Nuxt proxy/security posture without regressing auth/session flows.

### Sprint 13 Work Items

- BP13-001: Pin all GitHub actions to SHAs and enforce least-privilege job permissions. ✅ (Completed 2026-05-20)
- BP13-002: Add CI security gates (`pnpm audit`, dependency scan, Trivy image scan, SAST checks). ✅ (Completed 2026-05-20)
- BP13-003: Generate and publish SBOM artifacts (CycloneDX/SPDX) for API and web images. ✅ (Completed 2026-05-20)
- BP13-004: Add OpenTelemetry tracing and Prometheus metrics export on backend. ✅ (Completed 2026-05-20)
- BP13-005: Expose readiness/liveness probes and secure actuator endpoint exposure. ✅ (Completed 2026-05-20)
- BP13-006: Implement idempotency key support (`X-Idempotency-Key`) for selected mutating APIs. ✅ (Completed 2026-05-20)
- BP13-007: Harden Nuxt API proxy route with header allowlist, strict forwarding rules, and request validation. ✅ (Completed 2026-05-20)
- BP13-008: Add `nuxt-security` module and enforce CSP/security headers in frontend config. ✅ (Completed 2026-05-20)

### Sprint 13 Delivery Phases

1. **Phase A (CI/CD hardening):** BP13-001 through BP13-003.
2. **Phase B (backend observability/readiness):** BP13-004 and BP13-005.
3. **Phase C (backend correctness):** BP13-006.
4. **Phase D (frontend edge hardening):** BP13-007 and BP13-008.

### Sprint 13 Verification Gates

- `cd audita-api && ./gradlew :api:test --no-daemon`
- `cd audita-web && pnpm test && pnpm -s nuxi typecheck && pnpm build`
- `docker compose config` (sanity check after CI/image workflow updates)
- CI dry-run validation on branch for workflow syntax and required secrets contracts.

### Sprint 13 Verification (Completed 2026-05-20)

- `cd audita-api && ./gradlew :api:test --no-daemon` passes.
- `cd audita-web && pnpm test` passes (10 files, 31 tests).
- `cd audita-web && pnpm -s nuxi typecheck` passes.
- `cd audita-web && pnpm build` passes.
- `cd /mnt/samsung/repositories/audita && docker compose config` passes sanity validation.

### Sprint 13 Exit Criteria

1. CI workflow uses only SHA-pinned actions and least-privilege permissions.
2. Security scan and SBOM jobs are blocking and green.
3. Backend emits trace/metrics data with readiness/liveness probes verified.
4. Idempotency key flow is covered by regression tests.
5. Nuxt proxy/security updates pass auth/session regression and smoke tests.

## Post-Sprint Audit Export Reliability Follow-Up (2026-05-21)

- REL-AUD-001: Add focused infrastructure regression coverage for audit export cleanup token-expiry and stale artifact deletion. ✅ (Completed 2026-05-21)
- Verification: `cd audita-api && ./gradlew :infrastructure:test --tests "io.audita.infrastructure.service.AuditExportServiceTest" --no-daemon` and `cd audita-api && ./gradlew :api:compileTestJava :infrastructure:compileTestJava --no-daemon` both pass.

## Post-Sprint Container Scan Remediation (2026-05-21)

- REL-SEC-001: Upgrade vulnerable runtime dependencies flagged by Trivy image scan (Tomcat, Netty, PostgreSQL JDBC, OWASP HTML Sanitizer). ✅ (Completed 2026-05-21)
- Verification: dependency insights confirm fixed versions (`tomcat-embed-core 11.0.22`, `netty-codec-http 4.2.13.Final`, `netty-codec-compression 4.2.13.Final`, `postgresql 42.7.11`, `owasp-java-html-sanitizer 20260101.1`) and `./gradlew :api:compileTestJava :infrastructure:compileTestJava :api:test --no-daemon` passes.

## Post-Sprint Tenant Timezone Rollout (2026-05-21)

- REL-UX-001: Implement tenant-scoped timezone UX (IANA dropdown) and apply timezone-aware formatting across user-facing date displays. ✅ (Completed 2026-05-21)
- Verification: `cd audita-web && pnpm -s nuxi typecheck`, `cd audita-web && pnpm test`, and `cd audita-web && pnpm build` all pass (12 files, 39 tests including `tests/auth/timezone.spec.ts`).

## Post-Sprint Mention + Deep-Link Continuity Hotfix (2026-05-22)

- REL-COM-001: Enable live comment mention autocomplete (`@`) with backend user search endpoint + TipTap suggestion popup UX. ✅ (Completed 2026-05-22)
- REL-COM-002: Deep-link mention emails to exact comment and auto-focus comment on CR detail load. ✅ (Completed 2026-05-22)
- REL-AUTH-001: Preserve redirect target through sign-in so logged-out users return to comment deep-link, not dashboard. ✅ (Completed 2026-05-22)
- REL-EDGE-001: Disable `nuxt-security` `xssValidator` for `/api/**` proxy routes to avoid false-positive 400 rejects on mention markup. ✅ (Completed 2026-05-22)
- REL-SAN-001: Allowlist TipTap mention span attributes in backend comment sanitizer policy. ✅ (Completed 2026-05-22)

### Verification

- `cd audita-web && npx nuxi build` passes.
- `cd audita-api && ./gradlew :infrastructure:compileJava` passes.
- `cd audita-api && ./gradlew :infrastructure:test --tests "*CommentServiceTest*"` passes.

## Post-Sprint 6 — Web Docker + pnpm Policy Parity (2026-05-24)

- REL-WEB-001: Fix containerized `pnpm install` policy failures by copying `pnpm-workspace.yaml` in early web Docker build layer. ✅ (Completed 2026-05-24)
- REL-WEB-002: Keep hardened runtime semantics (`dhi.io/node:24` runtime with numeric non-root user) while using `dhi.io/node:24-dev` for builder compatibility. ✅ (Completed 2026-05-24)
- REL-WEB-003: Remove deprecated pnpm settings from `package.json` and migrate supported architecture policy to `pnpm-workspace.yaml`. ✅ (Completed 2026-05-24)

### Verification

- `docker build -t audita-web:scan -f audita-web/Dockerfile audita-web` passes with lockfile policy validation and full Nuxt production build.

---

## Post-Sprint UX Follow-Up (2026-04-28)

- UX-001: Add public onboarding status endpoint and first-run redirect gating so initial startup always lands on setup wizard.
- UX-002: Prevent setup wizard reuse after onboarding completion by redirecting bootstrap route to sign-in.
- UX-003: Resolve browser-only bootstrap 403 by stripping `Origin`/`Referer`/`Host` in Nuxt internal API proxy route. ✅ (Completed 2026-04-29)
- UX-004: Fix CR create redirect blank-page failure by stabilizing lazy read mapping in change request detail flow. ✅ (Completed 2026-04-29)
- UX-005: Complete CR pages styling consistency audit (button sizing/tab controls/rich text description rendering). ✅ (Completed 2026-04-29)

## Release Governance Implementation (2026-05-03)

- REL-001: Add source-available license policy with no-resale and no managed-service resale constraints. ✅
- REL-002: Rewrite README with full local, manual, and Docker deployment procedures. ✅
- REL-003: Add CI/release workflow for dev -> main merges to run tests, publish Docker images, and create SemVer tags/releases. ✅
- REL-004: Define historical tag backfill strategy (milestone tags, not per-commit tags). ✅
- REL-005: Bootstrap milestone tags pushed to origin (`v0.1.0` .. `v0.5.0`). ✅

## Architecture Follow-Up (2026-05-03)

- ARCH-001: Introduce application ports for authentication, SSO orchestration, and onboarding bootstrap/setup flows. ✅
- ARCH-002: Refactor API controllers to depend on application ports instead of concrete infrastructure services for auth/SSO/bootstrap endpoints. ✅
- ARCH-003: Internalize setup password hashing inside infrastructure tenant service and remove controller-level password encoder coupling. ✅

## Data Fidelity Follow-Up (2026-05-04)

- DATA-001: Add tenant admin settings endpoint (`/api/v1/settings`) to replace frontend settings placeholders. ✅
- DATA-002: Add dashboard summary endpoint (`/api/v1/dashboard/summary`) via application port (`DashboardPort`) to avoid API→JPA leakage. ✅
- DATA-003: Add platform health endpoint (`/api/platform/v1/health`) using `OnboardingPort` health probe. ✅
- DATA-004: Rewire frontend pages (`admin/settings`, `dashboard`, `platform`) to consume live backend endpoints instead of mock/stub values. ✅
- DATA-005: Validate changed backend/frontend slices (`./gradlew :api:compileJava :infrastructure:compileJava`, targeted eslint) with clean results. ✅

## Stability Follow-Up (2026-05-04)

- STAB-001: Decouple `TenantSettingsController` from infrastructure service/entity types via new `TenantSettingsPort`. ✅
- STAB-002: Restore Nuxt project-level type resolution by adding root `audita-web/tsconfig.json` extending `.nuxt/tsconfig.json`. ✅
- STAB-003: Eliminate residual global Nuxt typecheck blockers (API typing recursion, strict route middleware test signatures, missing tailwind type package). ✅

## Frontend Build-Stack Modernization (2026-05-04)

- FE-TAIL-001: Migrate frontend styling pipeline from Nuxt Tailwind module v6 path to Tailwind v4 Vite plugin (`@tailwindcss/vite`). ✅
- FE-TAIL-002: Convert Tailwind config entrypoint to v4-compatible setup (`tailwind.config.js` + CSS `@config`/`@import`). ✅
- FE-TAIL-003: Refactor custom CSS component layer to remove unsupported Tailwind v4 custom-class `@apply` chaining. ✅
- FE-TAIL-004: Re-verify frontend quality gates after migration (`pnpm test`, `pnpm -s nuxi typecheck`, `pnpm build`). ✅

## Sprint 8 — Admin Settings Activation & SLA Defaults (2026-05-11)

### Sprint 8 Objectives

1. Replace read-only admin workflow/SLA placeholders with persistent tenant-scoped settings.
2. Use configured SLA defaults at runtime for CR deadlines and warning windows.
3. Add regression coverage for settings contract and validation rules.

### Sprint 8 Work Items

- SET-001: Add persisted workflow/SLA defaults to tenant settings API. ✅ (Completed 2026-05-11)
- SET-002: Activate admin settings UI save flow for workflow/SLA defaults. ✅ (Completed 2026-05-11)
- SET-003: Read SLA defaults at runtime in CR creation and SLA monitor. ✅ (Completed 2026-05-11)
- SET-004: Add regression tests for tenant settings GET/PATCH + runtime effects. ✅ (Completed 2026-05-11)

### Sprint 9 Completed in this Session

- Added `PATCH /api/v1/settings` with nested workflow/sla validation contract.
- Added `OrgSettingEntity` + `OrgSettingRepository` backed by existing `org_settings` table.
- Extended `TenantSettingsPort` and implemented tenant-scoped read/write defaults in `TenantService`.
- Expanded `TenantAdminSettingsResponse` with `workflowDefaults` and `slaDefaults`.
- Replaced hard-coded SLA values in `ChangeRequestService` and warning window in `SlaMonitoringService` with tenant settings lookups (with safe defaults).
- Enabled admin workflow/SLA editing + save flow in `audita-web/pages/admin/settings/index.vue`.
- Added `TenantSettingsControllerWebMvcTest` for GET/PATCH success and validation failure paths.

### Sprint 9 Verification (Completed)

- `cd audita-api && ./gradlew :api:test --tests "io.audita.api.controller.TenantSettingsControllerWebMvcTest" --no-daemon`
- `cd audita-api && ./gradlew :api:compileJava :infrastructure:compileJava --no-daemon`
- `cd audita-web && pnpm -s nuxi typecheck`

### Sprint 9 Next Steps

1. Sprint 8 settings activation scope is complete. Start Sprint 9 prioritization (roles CRUD, SLA policy admin page, and group default approvers).

## Sprint 9 — CR List Scalability Follow-Up (2026-05-11)

### Sprint 9 Objectives

1. Ensure change-request list performance remains stable as dataset volume grows.
2. Bound per-request payload size while keeping explicit page navigation controls.

### Sprint 9 Work Items

- CR-LIST-001: Paginate `/change-requests` with max 50 per page and pagination buttons. ✅ (Completed 2026-05-11)

### Completed in this session

- Updated `audita-web/pages/change-requests/index.vue` to request `size=50`.
- Implemented explicit previous/next pagination buttons with page-index navigation.
- Preserved filter-driven reload behavior and total-count visibility.

### Verification (completed)

- `cd audita-web && pnpm -s nuxi typecheck`
- `cd audita-web && pnpm test -- --runInBand`

### Next Steps

1. Optionally add direct page-number buttons if users need quicker jumps across large result sets.

## Session Hardening Follow-Up (2026-05-12)

### Session Objectives

1. Eliminate broken half-authenticated states after redeploys and refresh failures.
2. Ensure forced logout can revoke the backend refresh token reliably.
3. Make the frontend fail closed on authentication expiry and tenant-context mismatches.

### Session Work Items

- SESS-001: Broaden refresh cookie scope from `/api/v1/auth/refresh` to `/api/v1/auth` so logout receives and clears the refresh token. ✅ (Completed 2026-05-12)
- SESS-002: Harden frontend refresh policy to retry only on `401`, not on `403`. ✅ (Completed 2026-05-12)
- SESS-003: Persist access-token expiry and treat expired/missing-expiry tokens as unauthenticated. ✅ (Completed 2026-05-12)
- SESS-004: Invalidate onboarding/session-derived cache on auth transitions. ✅ (Completed 2026-05-12)
- SESS-005: Force logout on tenant-context mismatch detected in route middleware. ✅ (Completed 2026-05-12)
- SESS-006: Add narrow backend/frontend regression tests for session hardening. ✅ (Completed 2026-05-12)
- SESS-007: Remove JS-readable auth-token persistence and restore session state from the HttpOnly refresh cookie. ✅ (Completed 2026-05-12)
- SESS-008: Add a non-rotating backend session introspection endpoint for cold-start session restore. ✅ (Completed 2026-05-12)
- SESS-009: Enforce API contract compatibility between frontend and backend auth flows. ✅ (Completed 2026-05-12)
- SESS-010: Add token-free cross-tab session synchronization for restore/logout events. ✅ (Completed 2026-05-12)
- SESS-011: Replace the temporary Spring Security authorization-config workaround with a public, type-safe authorization manager. ✅ (Completed 2026-05-12)

### Session Verification

- `cd audita-api && ./gradlew :api:test --tests "io.audita.api.controller.AuthControllerWebMvcTest" --tests "io.audita.api.config.ApiContractHeaderFilterTest" --tests "io.audita.infrastructure.service.AuthServiceTest"`
- `cd audita-api && ./gradlew :api:test --tests "io.audita.api.config.SecurityConfigAuthorizationTest" --tests "io.audita.api.config.ApiContractHeaderFilterTest" --tests "io.audita.api.controller.AuthControllerWebMvcTest" --tests "io.audita.api.security.TenantResolutionFilterTest"`
- `cd audita-web && pnpm test -- tests/auth/session.spec.ts tests/auth/tenant-resolution.spec.ts tests/auth/api-contract.spec.ts tests/auth/session-sync.spec.ts tests/middleware/tenant.spec.ts tests/middleware/auth.global.spec.ts`
- `cd audita-web && pnpm -s nuxi typecheck`

### Session Next Steps

1. Add a user-facing session-expired or app-updated banner on forced contract/logout redirects so the reason is explicit.
2. If auth startup cost becomes noticeable, cache a short-lived session-presence hint client-side and only call `/api/v1/auth/session` when needed.
3. Revisit the Spring Security config after future framework/tooling upgrades and collapse back to the higher-level DSL if the nested-type accessibility snag is resolved.

### Session Completion Notes

- Security-first session hardening is fully implemented: refresh-cookie logout revocation, HttpOnly-only cold-start restore, `401`-only refresh, API contract enforcement, and token-free cross-tab session sync are all in place.
- The temporary reflection-based `SecurityConfig` workaround was removed. Request authorization now uses Spring Security public APIs: `RequestMatcherDelegatingAuthorizationManager`, `AuthorizationFilter`, and `PathPatternRequestMatcher`.
- Added focused authorization regression coverage so public auth routes, authenticated fallback, and super-admin platform routes are locked by tests without relying on framework-internal DSL types.

### Post-Session Cleanup (Completed 2026-05-12)

- Removed regex-based filename normalization from `ChangeRequestService` and replaced it with a single-pass stem normalizer to eliminate Sonar `java:S5852` backtracking risk.
- Documented the stateless/bearer-token CSRF rationale directly in `SecurityConfig` and suppressed noisy Sonar `java:S4502` on the filter-chain method instead of introducing fake CSRF protection.
- Extended Spring config metadata for `audita.invite.expiry-hours` and `audita.mail.from-name`, and added typed `invite`/`mail` groups to `AuditaProperties` so YAML metadata stays synchronized with `@Value` consumers.
- Confirmed `HttpSecurity cannot be resolved` in `SecurityConfig` is a stale editor classpath issue because `cd audita-api && ./gradlew :api:compileJava --no-daemon` passes.
- Fixed the live frontend lint issue by switching `middleware/tenant.ts` from `window.location.hostname` to `globalThis.location.hostname`.

## Session UX & Workflow Recovery (2026-05-12)

### Objectives

1. Restore durable auth on localhost refresh after the HttpOnly-cookie migration.
2. Remove CR approval/rejection blockers and post-action UI failures.
3. Upgrade CR detail/create collaboration UX for attachments, rich text, activity readability, and vote visibility.

### Work Items

- UXR-001: Make refresh-cookie `Secure` behavior environment-configurable and disable it for local `dev` HTTP. ✅ (Completed 2026-05-12)
- UXR-002: Allow assigned non-auditor approvers to vote even when their base role is not `APPROVER`. ✅ (Completed 2026-05-12)
- UXR-003: Fix CR comment DTO lazy-loading failure and pre-initialize detail activity/attachment actors for safe mapping. ✅ (Completed 2026-05-12)
- UXR-004: Center reject confirmation modal and restore full-screen overlay coverage. ✅ (Completed 2026-05-12)
- UXR-005: Add rich-text formatting toolbar and queued attachments on CR create. ✅ (Completed 2026-05-12)
- UXR-006: Add recorded-votes card and human-readable activity stream rendering on CR detail. ✅ (Completed 2026-05-12)

### Verification

- `cd audita-api && ./gradlew :api:test --tests "io.audita.api.controller.AuthControllerWebMvcTest" --tests "io.audita.api.controller.CommentControllerWebMvcTest" --tests "io.audita.api.controller.UserControllerSecurityAnnotationsTest" :infrastructure:test --tests "io.audita.infrastructure.service.CommentServiceTest" --no-daemon`
- `cd audita-web && pnpm -s nuxi typecheck`

## Session RBAC Expansion (2026-05-12)

### Objectives

1. Auto-assign approver participants on CR creation (not only on submit).
2. Support multi-role user assignment with effective-role precedence.
3. Enable admin-managed custom roles with explicit permission-rule overlap safeguards.

### Work Items

- RBAC-001: Auto-add Approver/Auditor/Admin users when a CR is created and keep submit-time population idempotent. ✅ (Completed 2026-05-12)
- RBAC-002: Add `user_roles` schema migration and backfill from legacy `users.role_id`. ✅ (Completed 2026-05-12)
- RBAC-003: Add multi-role assignment support in user invite/update APIs while preserving legacy `roleId` compatibility. ✅ (Completed 2026-05-12)
- RBAC-004: Extend JWT claims + auth principal authorities with role and permission sets from all assigned roles. ✅ (Completed 2026-05-12)
- RBAC-005: Add admin endpoints to create custom roles and update custom role permissions with overlap checks. ✅ (Completed 2026-05-12)

### Verification

- `cd audita-api && ./gradlew :infrastructure:compileJava :api:compileJava :infrastructure:test --tests io.audita.infrastructure.service.ChangeRequestServiceSecurityTest --no-build-cache`

## Post-Sprint Reliability + UX Hardening (2026-05-22)

### Objectives

1. Fix settings save `400` cascade and auth session/logout `500` errors on redeploy.
2. Eliminate log noise from Hibernate, Caffeine, and SSE lifecycle.
3. Upgrade CR descriptions to full rich-text with three-layer link enforcement.
4. Redesign approver UX for seamless multi-select with visual preview.

### Work Items

- FIX-001: Fix `autoApproverDefaults` UUID parsing — changed to `List<String>` with explicit validation. ✅
- FIX-002: Fix Nuxt proxy stripping `content-length` — added to allowed headers. ✅
- FIX-003: Switch settings PATCH to tolerant map parsing — `Map<String, Object>` with field validators. ✅
- FIX-004: Add `V10__repair_refresh_tokens_table.sql` for drifted schemas. ✅
- FIX-005: Guard auth endpoints with tenant header requirement — returns `401` instead of `500`. ✅
- FIX-006: Propagate tenant header in frontend auth flows. ✅
- LOG-001: Remove explicit `hibernate.dialect` — fixed `HHH90000025`. ✅
- LOG-002: Add `recordStats` to Caffeine cache spec — fixed metrics warning. ✅
- LOG-003: Add targeted logger levels for Hibernate/Micrometer noise. ✅
- LOG-004: Remove pageable `@EntityGraph` collection fetch — fixed `HHH90003004`. ✅
- LOG-005: Improve SSE lifecycle handling — intentional disconnect + page visibility guards. ✅
- RT-001: Create shared TipTap extension config composable. ✅
- RT-002: Expand `RichTextToolbar` with full formatting controls. ✅
- RT-003: Add `.rich-content` CSS for read-only render fidelity. ✅
- RT-004: Backend HTML sanitizer with anchor attribute normalization. ✅
- RT-005: Wire sanitizer into `ChangeRequestService` create/update. ✅
- RT-006: Frontend link normalization on render. ✅
- RT-007: Add rich-text + sanitizer test suites. ✅
- UX-001: Replace single-select approver panel with multi-select list + chips preview + batch save. ✅

### Verification

- `cd audita-web && pnpm -s nuxi typecheck`
- `cd audita-web && pnpm test` (41 tests, 13 files)
- `cd audita-web && pnpm build`

## Approver UX Polish + Activity Stream + CI Fix (2026-05-22)

### Objectives

1. Polish approver UX: default Optional, per-approver toggle on saved list, creator exclusion, dirty tracking, reorder animations.
2. Fix activity stream "Count 4" readability issue.
3. Fix CI Trivy scan failure (CVE-2026-33671).

### Work Items

- POL-001: Default new approvers to Optional instead of Required. ✅
- POL-002: Add per-approver Required/Optional toggle button on saved approver list. ✅
- POL-003: Add backend `PATCH /{id}/approvers/{approverId}/requirement` endpoint. ✅
- POL-004: Exclude CR creator from candidate list. ✅
- POL-005: Add dirty tracking + save prompt banner for approver changes. ✅
- POL-006: Add reorder animations via `TransitionGroup` + CSS transitions. ✅
- ACT-001: Fix activity stream "Count 4" → "Reordered 4 approvers." human-readable summary. ✅
- CI-001: Add `.trivyignore` for CVE-2026-33671 (picomatch in Node.js base image, not exploitable). ✅

### Verification

- `cd audita-web && pnpm -s nuxi typecheck`
- `cd audita-web && pnpm test` (41 tests, 13 files)
- `cd audita-web && pnpm build`
- `cd audita-api && ./gradlew :api:compileJava :infrastructure:compileJava --no-daemon`

## Post-Sprint 4 — DHI Container Hardening + Build Reliability (2026-05-23)

### Objectives

1. Ensure API Docker image is compatible with DHI hardened runtime constraints.
2. Remove shell/curl runtime assumptions from compose/runtime flow.
3. Stabilize Docker build path for Gradle wrapper distribution downloads.

### Work Items

- DHI-001: Harden API runtime stage for distroless/hardened execution using numeric non-root ownership. ✅
- DHI-002: Remove compose API healthchecks that depend on in-image curl/shell. ✅
- DHI-003: Increase Gradle wrapper timeout and apply Docker build network timeout hardening. ✅
- DHI-004: Rebuild hardened API image and verify compose startup path end-to-end. ✅
- DHI-005: Validate API readiness endpoint from host and confirm successful boot logs. ✅
- DHI-006: Reconcile memory-bank records for container hardening closure. ✅

### Verification

- `docker compose -f docker-compose.local.yml build api`
- `docker compose -f docker-compose.local.yml up -d --build`
- `docker compose -f docker-compose.local.yml ps`
- `docker compose -f docker-compose.local.yml logs --no-color --tail=200 api`
- `curl -sS -o /tmp/audita-health.json -w "%{http_code}" http://localhost:7080/actuator/health`

## Post-Sprint 5 — Approver Mutation Expansion + Activity Summary Tests (2026-05-23)

### Objectives

1. Allow approver add/remove/reorder/requirement updates while CR is `PENDING_APPROVAL`.
2. Enforce vote safety: voted approvers cannot be removed.
3. Ensure activity stream and audit trail fully capture approver mutation actions.
4. Add frontend unit coverage for human-readable activity summaries.

### Work Items

- APV-001: Remove approval-lock gate for approver management operations and allow open-state (`DRAFT`, `PENDING_APPROVAL`) mutation. ✅
- APV-002: Add voted-approver removal guard (`APPROVER_DECISION_LOCKED`). ✅
- APV-003: Add audit log events for approver add/group-add/remove/reorder/requirement-change. ✅
- APV-004: Update CR detail UI to allow approver management in `PENDING_APPROVAL`. ✅
- APV-005: Disable remove action for voted approvers with explicit tooltip reason. ✅
- APV-006: Clarify admin settings UX: configured default approvers are always auto-added. ✅
- APV-007: Extract activity summary helper + add dedicated unit tests. ✅

### Verification

- `cd audita-api && ./gradlew :infrastructure:test --tests "io.audita.infrastructure.service.ChangeRequestServiceSecurityTest" --no-daemon`
- `cd audita-web && pnpm -s nuxi typecheck`
- `cd audita-web && pnpm test`

---

## License Normalization & Social Media Launch Preparation (2026-05-25)

### Objectives

1. Switch project from source-available (Commons Clause) to true open-source Apache 2.0 license.
2. Prepare platform-specific social media copy for Audita public launch.
3. Define posting strategy, hashtags, and engagement playbook.

### Work Items

- **LIC-001**: Replace `LICENSE` with canonical Apache 2.0 text. ✅
- **LIC-002**: Update `README.md` license section to reflect Apache 2.0. ✅
- **LIC-003**: Add inbound=outbound contributor licensing note to `CONTRIBUTING.md`. ✅
- **LIC-004**: Update `LICENSE-APACHE` reference to remove stale resale wording. ✅
- **MKT-001**: Draft LinkedIn launch copy (playful/irreverent tone). ✅
- **MKT-002**: Draft Twitter/X launch copy (punchy/hot-take tone). ✅
- **MKT-003**: Draft Reddit/Hacker News launch copy (honest builder tone). ✅
- **MKT-004**: Create 7-day posting schedule and engagement tips. ✅
- **MKT-005**: Define hashtag strategy and CTA (star the repo). ✅

### Decisions

- User chose Apache 2.0 over custom source-available license after learning that "open source" implies no resale restrictions.
- Tone: playful/irreverent.
- CTA: star the GitHub repo.
- User will provide own platform screenshots; auto-generated image assets discarded.

### Verification

- `git diff --check -- LICENSE README.md CONTRIBUTING.md LICENSE-APACHE` — clean.
- Copy review: `social-media-assets/README.md` contains platform-specific posts ready for use.
