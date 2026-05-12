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
- SET-002: Activate admin settings UI save flow for workflow/SLA defaults. 🟡 (In progress)
- SET-003: Read SLA defaults at runtime in CR creation and SLA monitor. 🟡 (In progress)
- SET-004: Add regression tests for tenant settings GET/PATCH + runtime effects. 🟡 (In progress)

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
