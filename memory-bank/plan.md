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
