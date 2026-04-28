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

- SEC-001: Eliminate tenant slug injection surface in schema switching path.
- SEC-002: Remove URL query token transport from SSO callback flow.
- SEC-003: Add object-level authorization checks for change request mutations.
- SEC-004: Tighten CORS to explicit allowlist with environment-specific profiles.

## Post-Sprint UX Follow-Up (2026-04-28)

- UX-001: Add public onboarding status endpoint and first-run redirect gating so initial startup always lands on setup wizard.
- UX-002: Prevent setup wizard reuse after onboarding completion by redirecting bootstrap route to sign-in.
