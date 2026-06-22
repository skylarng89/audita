# Audita — Current Plan

**Date:** 2026-06-22
**Owner:** Developer 1

---

## Completed Workstreams

### Post-Deployment Hardening (Completed 2026-06-22)
- Setup token injection via Nuxt server proxy
- SLA component silent failure fix (SFC → raw inline inputs)
- Skeleton loaders replace loading overlay on all 20 pages
- Log permissions fix (bind mount + user override)
- Upload permissions fix (bind mount + user override)
- LazyInit fix on watcher list (EntityGraph)
- Audit log UPDATE fix (native SQL INSERT instead of JPA save)
- Approver not removed on demotion fix (collection remove)
- Activity stream missing for watchers fix
- Mark Complete restricted to creator + Admin
- Comment mentions fix (param name + dedicated endpoint)
- Multi-file upload on detail page

### RBAC & Flow Simplification (Completed 2026-06-18)
- Permission-based RBAC: 88 annotations switched, 22 permissions, 3 system roles
- Watchers: view-only participants, move operations
- Deployment single assignee, all approvers required
- Read-only boundary, copy URL, file logging
- 245 backend + 197 frontend tests passing
- Merged to main, deployed, verified

### Post-Sprint Notification Wiring (Completed 2026-06-14)
- CR/UAT/deployment notification hooks, MentionNotifier, email templates

---

## Ongoing / Verification

### Deferred / Optional
- S3 upload implementation (config scaffolding exists, no runtime code)
- Stage-specific email templates (UAT/Deployment reuse CR templates)
- `RequestUatService.addApproverGroup()` method (doesn't exist)
- `CrApproverPanel.vue` is unused (orphaned component)
- Dead composable methods: `approveDeployment`/`rejectDeployment`/`listDeploymentApprovers` in `useChangeRequests.ts`

---

## Next Actions

- Monitor for any remaining runtime issues
- S3 upload implementation if needed
