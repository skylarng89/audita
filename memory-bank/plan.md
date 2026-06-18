# Audita — Current Plan

**Date:** 2026-06-18
**Owner:** Developer 1

---

## Completed Workstreams

### RBAC & Flow Simplification (Completed 2026-06-18)
- Permission-based RBAC: 88 `@PreAuthorize` annotations switched to `@authz.hasPermission()`
- 3 system roles (Admin, Requester, Auditor) — Approver role removed
- 22 permissions (4 new: `cr.view.all`, `cr.manage_participants`, `uat.signoff`, `deployment.execute`)
- Custom role CRUD with permission picker and overlap validation
- Watchers: view-only participants with commenting + notifications, mutual exclusivity with approvers, move operations
- Deployment single assignee model: replaced multi-approver with single deployer
- All approvers required: removed Required/Optional toggle
- Read-only boundary: `completionStatus == COMPLETED` blocks all mutations
- Core fields locked after submission (DRAFT-only editing)
- Copy URL button with redirect-after-login
- File logging with daily rotation and env-configurable levels
- 14 new audit action types
- Sample data updated for new schema (roles, watchers, deployments)
- 245 backend tests passing, 197 frontend tests passing
- Merged to `main`, CI running

### Post-Sprint Notification Wiring (Completed 2026-06-14)
- CR approver flow: 7 hooks in `ChangeRequestService` (in-app + email)
- UAT lifecycle: 4 hooks in `RequestUatService` (in-app + email)
- Deployment lifecycle: 4 hooks in `RequestDeploymentService` (in-app + email)
- Shared `MentionNotifier` extraction from `CommentService`
- SLA WARNING email added, audit export in-app notification added
- 7 new modular email templates

### Versioning Reset (Completed 2026-06-14)
- "v" prefix removed, baseline at 1.0.0
- CI workflow, docker-compose, README updated
- 42/50 remote tags deleted; 8 blocked by repo rules

### Audit Log Hardening (Completed 2026-06-15)
- Full audit of all 27 `.log()` call sites across 5 service files
- Fixed 6 empty/missing payloads, 11 missing actorEmails
- Added IP address capture via new `RequestContext` ThreadLocal
- `TenantResolutionFilter` now captures client IP from `X-Forwarded-For` header

---

## Ongoing / Verification

### CI Verification
1. **RBAC refactoring CI** — merged to `main`, awaiting CI pipeline result
2. **Runtime verification** — drop DB, start backend (Flyway migrations), start frontend, smoke test

### Deferred / Optional
- Stage-specific email templates (UAT/Deployment currently reuse CR templates)
- `RequestUatService.addApproverGroup()` method (doesn't exist — UAT has no group-approver support)
- Group member add/remove notifications (low priority)
- `addApproverGroup` still carries `isRequired` in `AddApproverGroupRequest` — may need cleanup
- Dead composable methods: `approveDeployment`/`rejectDeployment`/`listDeploymentApprovers` remain in `useChangeRequests.ts`
- `CrApproverPanel.vue` is unused (orphaned component)

---

## Next Actions

1. Monitor CI pipeline for RBAC refactoring
2. If CI passes: drop DB, deploy, runtime smoke test
3. If CI fails: fix issues, re-push
4. Update `memory-bank/context.md` after CI result
