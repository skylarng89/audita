# RBAC & Flow Simplification — Design Spec

**Date:** 2026-06-18
**Status:** Draft
**Owner:** Developer 1

---

## 1. Overview

Simplify the user role system and change request flow in Audita. The current system has 4 tenant roles (Admin, Requester, Approver, Auditor) with a permissions table that is provisioned but never enforced. This refactor activates permission-based RBAC, reduces to 3 system roles, introduces watchers, replaces deployment approvers with a single assignee, makes all approvers required, and clarifies the read-only boundary.

### Goals

1. **Permission-based enforcement**: Replace all `hasRole()` checks with `hasAuthority()` against the existing permission catalogue, making custom roles functional.
2. **Simplify roles**: Drop the `Approver` role. System roles become Admin, Requester, Auditor. Custom roles remain admin-defined.
3. **Introduce watchers**: View-only participants who can comment and receive notifications but cannot approve/reject.
4. **Single deployer assignee**: Replace deployment approvers with one assigned deployer who marks completion.
5. **All approvers required**: Remove the Required/Optional toggle. Every approver is required.
6. **Clear read-only boundary**: Requests become read-only only when `completionStatus = COMPLETED`. Core fields lock after submission; participant management continues until completion.
7. **Shareable request links**: Copy-URL button with redirect-after-login.
8. **Comprehensive audit logging**: Every new action recorded in the audit trail.
9. **File logging**: Daily-rotated log files in `logs/` directory with env-configurable levels and retention.

### Non-Goals

- Multi-tenant cross-tenant permissions
- Changing the SUPER_ADMIN identity model (separate `super_admins` table)
- Changing the two workflow modes (`APPROVAL_ONLY`, `DELIVERY_PIPELINE`)
- Removing the `approvalLocked` flag
- Changing the UAT requester sign-off feature (ADR-045)

### Database Strategy

The database will be dropped and recreated from scratch. Migration files are modified in-place rather than creating incremental migrations. No data preservation needed.

---

## 2. RBAC Permission Enforcement

### 2.1 Permission Catalogue (22 permissions)

The existing 18 permissions plus 4 new ones:

| Code | Label | New? |
|------|-------|------|
| `cr.create` | Create Change Requests | |
| `cr.view` | View Change Requests (own/participating) | |
| `cr.view.all` | View All Change Requests (global) | **New** |
| `cr.edit` | Edit Change Requests | |
| `cr.cancel` | Cancel Change Requests | |
| `cr.submit` | Submit Change Requests for Approval | |
| `cr.approve` | Approve / Reject Change Requests | |
| `cr.manage_participants` | Add/Remove Approvers, Watchers, Assignees | **New** |
| `uat.signoff` | UAT Sign-Off (requester + approver) | **New** |
| `deployment.execute` | Mark Deployment Completed | **New** |
| `users.view` | View Users | |
| `users.manage` | Invite, Edit and Deactivate Users | |
| `roles.view` | View Roles | |
| `roles.manage` | Create and Manage Custom Roles | |
| `groups.view` | View Groups | |
| `groups.manage` | Create and Manage Groups | |
| `settings.view` | View Organisation Settings | |
| `settings.manage` | Manage Organisation Settings | |
| `sla.view` | View SLA Policies | |
| `sla.manage` | Create and Manage SLA Policies | |
| `audit.view` | View Audit Trail | |
| `audit.export` | Export Audit Trail to CSV | |

### 2.2 System Role Permission Sets

| Permission | Admin | Requester | Auditor |
|---|---|---|---|
| `cr.create` | Yes | Yes | No |
| `cr.view` | Yes | Yes | Yes |
| `cr.view.all` | Yes | No | Yes |
| `cr.edit` | Yes | Yes | No |
| `cr.cancel` | Yes | Yes | No |
| `cr.submit` | Yes | Yes | No |
| `cr.approve` | Yes | Yes | No |
| `cr.manage_participants` | Yes | Yes | No |
| `uat.signoff` | Yes | Yes | No |
| `deployment.execute` | Yes | Yes | No |
| `users.view` | Yes | Yes | Yes |
| `users.manage` | Yes | No | No |
| `roles.view` | Yes | No | Yes |
| `roles.manage` | Yes | No | No |
| `groups.view` | Yes | Yes | Yes |
| `groups.manage` | Yes | No | No |
| `settings.view` | Yes | Yes | Yes |
| `settings.manage` | Yes | No | No |
| `sla.view` | Yes | Yes | Yes |
| `sla.manage` | Yes | No | No |
| `audit.view` | Yes | No | Yes |
| `audit.export` | Yes | No | Yes |

### 2.3 Controller Enforcement

All 88 `@PreAuthorize` annotations across 16 controllers change from `hasRole()`/`hasAnyRole()` to `hasAuthority()`.

```java
// Before
@PreAuthorize("hasAnyRole('REQUESTER','ADMIN','SUPER_ADMIN')")

// After
@PreAuthorize("hasAuthority('cr.submit')")
```

### 2.4 SUPER_ADMIN Wildcard

SUPER_ADMIN uses a separate identity store (`super_admins` table) and bypasses tenant context. In `UserPrincipal.ofSuperAdmin()`, add a `*` authority (a plain `SimpleGrantedAuthority("*")`).

A custom SpEL function or a custom `AuthorizationManager` interprets `*` as matching any permission. The simplest approach: register a custom `PermissionExpressionRoot` via a `SecurityExpressionRoot` extension, or use a bean-based SpEL function:

```java
@PreAuthorize("@authz.hasPermission(authentication, 'cr.submit')")
```

Where `@authz` is a registered `AuthorizationLogic` bean that returns `true` if the user has the specific permission OR has the `*` wildcard authority.

This keeps annotations clean and handles the wildcard uniformly.

### 2.5 Custom Role Overlap Validation

`RoleService.denyPermissionOverlap()` is updated to reject:

1. **No full-permission custom roles**: A custom role whose permission set equals ALL available permissions is rejected with `OVERLAPPING_PERMISSIONS`. Admin is the only role that can have all permissions.
2. **No exact-set duplicates**: Two custom roles with the exact same permission set are rejected with `OVERLAPPING_PERMISSIONS`.
3. **Partial overlap allowed**: Custom roles may share some permissions with other roles. This is acceptable.

### 2.6 Approver Role Removal

The `Approver` system role (UUID `...0003`) is removed from the V1 seed. Existing users with the Approver role are migrated to Requester during the DB recreation (no migration script needed — the role simply doesn't exist in the new seed).

`RoleHierarchy.precedence()` is updated to remove the `APPROVER` case. Custom roles with `default -> 100` remain.

---

## 3. Watchers

### 3.1 Database

New tables in tenant schema:

```sql
CREATE TABLE cr_watchers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at        TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(change_request_id, user_id)
);

CREATE TABLE request_uat_watchers (
    id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    uat_id  UUID NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(uat_id, user_id)
);
```

No `is_required`, no `status`, no `position` — watchers don't vote or have ordering.

### 3.2 Entities

- `CrWatcherEntity` — `id`, `changeRequest` (ManyToOne), `user` (ManyToOne), `createdAt`
- `RequestUatWatcherEntity` — `id`, `uat` (ManyToOne), `user` (ManyToOne), `createdAt`

### 3.3 Mutual Exclusivity with Approvers

A user cannot be both an approver and a watcher on the same request. Enforced in:

- `ChangeRequestService.addWatcher()` — checks `cr_approvers` for this CR, throws `CONFLICT` if user is already an approver
- `ChangeRequestService.addApprover()` — checks `cr_watchers` for this CR, throws `CONFLICT` if user is already a watcher
- Same for UAT: `RequestUatService.addApprover()` and `RequestUatService.addWatcher()`

### 3.4 Move Operations

| Method | Action |
|---|---|
| `moveWatcherToApprover(crId, userId)` | Remove from `cr_watchers`, add to `cr_approvers` with `position=last+1`, `status=PENDING`. Audited. |
| `moveApproverToWatcher(crId, approverId)` | Remove from `cr_approvers` (only if `status=PENDING` — can't convert a voter who already voted), add to `cr_watchers`. Audited. |
| `moveUatWatcherToApprover(uatId, userId)` | Same for UAT. |
| `moveUatApproverToWatcher(uatId, approverId)` | Same for UAT. |

### 3.5 API Endpoints (CR-level)

All require `cr.manage_participants` permission:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/{id}/watchers` | List watchers |
| `POST` | `/{id}/watchers` | Add watcher(s) — body: `{ userIds: [] }` |
| `DELETE` | `/{id}/watchers/{userId}` | Remove a watcher |
| `POST` | `/{id}/watchers/{userId}/promote` | Move watcher to approver |
| `POST` | `/{id}/approvers/{approverId}/demote` | Move approver to watcher |

### 3.6 API Endpoints (UAT-level)

All require `cr.manage_participants` permission:

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/{id}/uat/watchers` | List UAT watchers |
| `POST` | `/{id}/uat/watchers` | Add UAT watcher(s) |
| `DELETE` | `/{id}/uat/watchers/{userId}` | Remove UAT watcher |
| `POST` | `/{id}/uat/watchers/{userId}/promote` | Move UAT watcher to approver |
| `POST` | `/{id}/uat/approvers/{approverId}/demote` | Move UAT approver to watcher |

### 3.7 Auditor Restriction

Auditors cannot be added as watchers — they already have global view access via `cr.view.all` permission. Adding them as watchers would be redundant. Enforced in `addWatcher()` service methods (defense-in-depth, same pattern as the existing approver block from ADR-047).

### 3.8 Watcher Capabilities

- **Can**: View the request, view attachments, add comments, receive notifications (in-app + email) for status changes and new comments
- **Cannot**: Approve/reject, edit the request, manage participants, mark completion

### 3.9 Visibility

A watcher is included in the "allowed viewers" set in `ChangeRequestService.isViewerAllowed()`. Even on PENDING_APPROVAL requests, a watcher can view the request. This matters for the Requester-only visibility rule. DRAFT requests remain private to the creator — watchers cannot see a DRAFT.

### 3.10 Notifications

Watchers receive the same notification types as approvers for status changes and new comments. Wired through the existing `notificationService.createAndPush()` pattern. Email notifications via the existing modular template system (ADR-034).

---

## 4. Deployment Model — Single Assignee

### 4.1 Database

The `request_deployment_approvers` table is removed entirely. The `request_deployments` table is modified:

```sql
CREATE TABLE request_deployments (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id    UUID NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
    uat_id        UUID NOT NULL UNIQUE REFERENCES request_uat(id) ON DELETE CASCADE,
    assignee_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    status        VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED','CANCELLED')),
    created_by    UUID NOT NULL,
    promoted_at   TIMESTAMP NOT NULL DEFAULT now(),
    completed_at  TIMESTAMP,
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

Key changes:
- `assignee_id` is nullable (set after promotion by the requester)
- Status values: `PENDING` / `COMPLETED` / `CANCELLED` (was `PENDING_APPROVAL` / `APPROVED` / `REJECTED`)
- No `rejection_reason` — no reject concept

### 4.2 Entity Changes

- `RequestDeploymentEntity`: Remove `approvers` collection. Add `assignee` (`@ManyToOne UserEntity`, `assignee_id` FK, nullable). Add `completedAt` (already exists). No `rejectionReason`.
- Delete `RequestDeploymentApproverEntity` entirely.

### 4.3 Promotion from UAT

`RequestDeploymentService.createFromPromotion()` creates the deployment row with `assignee_id = NULL` and `status = PENDING`. The requester then assigns a deployer via a separate endpoint. The deployment is not actionable until an assignee is set.

### 4.4 Assignment Endpoints

| Method | Path | Permission | Purpose |
|---|---|---|---|
| `PATCH` | `/{id}/deployment/assignee` | `cr.manage_participants` | Set/change deployer assignee — body: `{ userId }` |
| `GET` | `/{id}/deployment` | `cr.view` | Returns deployment incl. `assignee` resolved object |

Reassignment is allowed at any time while `status = PENDING`. Cannot reassign a completed deployment. Audited.

### 4.5 Completion Endpoint

| Method | Path | Permission | Purpose |
|---|---|---|---|
| `POST` | `/{id}/deployment/complete` | `deployment.execute` | Deployer marks deployment completed |

### 4.6 Completion Logic

`RequestDeploymentService.completeDeployment()`:
1. Load deployment — must have `status = PENDING` and `assignee_id != NULL`
2. Verify actor is the assignee OR the actor has `cr.view.all` permission (Admin override). The assignee check is the primary gate — a Requester who is not the assignee cannot complete the deployment.
3. Set `status = COMPLETED`, `completedAt = now()`
4. Audit log + notification to requester ("Deployment completed by {assignee}, ready to close")
5. Do **NOT** auto-complete the CR — the requester clicks "Mark Complete" on the CR afterward

### 4.7 Auditor Restriction

Auditors cannot be assigned as deployers — enforced in the assign endpoint (defense-in-depth).

### 4.8 Deployment Status DTO

```java
public record RequestDeploymentResponse(
    UUID id,
    UUID requestId,
    UUID uatId,
    DeploymentStatus status,        // PENDING | COMPLETED | CANCELLED
    UserSummary assignee,           // { id, email, fullName } or null
    String createdByFullName,
    OffsetDateTime promotedAt,
    OffsetDateTime completedAt
);
```

### 4.9 `isDeploymentDone()` Update

`RequestDeploymentService.isDeploymentDone()` now checks `status == COMPLETED` (was `status == "APPROVED"`).

---

## 5. All Approvers Are Required

### 5.1 Database

`cr_approvers` table: remove `is_required` column, remove `is_ad_hoc` column:

```sql
CREATE TABLE cr_approvers (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id  UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id            UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    position           INTEGER NOT NULL DEFAULT 0,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    rejection_reason   TEXT,
    decided_at         TIMESTAMP,
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(change_request_id, user_id)
);
```

`request_uat_approvers` table: remove `is_required` column. Same structure otherwise.

### 5.2 Entity Changes

- `CrApproverEntity`: Remove `isRequired` field, remove `isAdHoc` field.
- `RequestUatApproverEntity`: Remove `isRequired` field.

### 5.3 Approval Evaluation Simplification

`ChangeRequestEntity.evaluateApprovalClosure()`:
- All approvers must approve. If any approver rejects -> `REJECTED`. If all approvers have `status = APPROVED` -> `APPROVED`. Otherwise stays `PENDING_APPROVAL`.
- No more required/optional distinction.

### 5.4 API Changes

- Delete `PATCH /{id}/approvers/{approverId}/requirement` endpoint
- Delete `PATCH /{id}/uat/approvers/{approverId}/requirement` endpoint
- `addApprover()` no longer accepts `isRequired` parameter
- `addUatApprover()` no longer accepts `isRequired` parameter

### 5.5 Approver Removal

The requester can add and remove approvers at any point while the request is in progress (to avoid blocking if an approver is unavailable). Existing behavior stays:
- Can remove a `PENDING` approver at any time before completion
- Cannot remove an approver who has already voted (`APPROVED` or `REJECTED` status) — their vote is part of the audit trail

---

## 6. Read-Only Boundary & Completion Flow

### 6.1 Editable State Matrix

| Action | DRAFT | PENDING_APPROVAL | APPROVED (IN_PROGRESS) | COMPLETED |
|---|---|---|---|---|
| Edit core fields | Yes | No | No | No |
| Submit for approval | Yes | No | No | No |
| Cancel request | Yes | Yes | Yes | No |
| Add/remove approvers | Yes | Yes | Yes | No |
| Add/remove watchers | Yes | Yes | Yes | No |
| Move approver <-> watcher | Yes | Yes | Yes | No |
| Reorder approvers | Yes | Yes | Yes | No |
| Approve/reject (approvers) | No | Yes | No | No |
| Create UAT | No | No | Yes | No |
| Manage UAT approvers/watchers | No | No | Yes | No |
| UAT sign-off | No | No | Yes | No |
| Promote to deployment | No | No | Yes | No |
| Assign/change deployer | No | No | Yes | No |
| Mark deployment completed | No | No | Yes | No |
| Mark CR complete (APPROVAL_ONLY) | No | No | Yes | No |
| Mark CR complete (DELIVERY_PIPELINE) | No | No | Yes (after deployment done) | No |
| Add comments | Yes | Yes | Yes | No |
| View request | Yes | Yes | Yes | Yes |

Comments are locked when `completionStatus = COMPLETED` (see section 6.4). The entire request is read-only — no mutations of any kind, including comments.

### 6.2 Completion Logic — APPROVAL_ONLY

`ChangeRequestService.completeRequest()`:
1. Verify `completionStatus != COMPLETED`
2. Verify `approvalStatus == APPROVED`
3. Set `completionStatus = COMPLETED`
4. Audit log + notification ("Change request completed")
5. Request is now read-only

This is a **manual** step — the requester clicks "Mark Complete" after all approvers have approved.

### 6.3 Completion Logic — DELIVERY_PIPELINE

`ChangeRequestService.completeRequest()`:
1. Verify `completionStatus != COMPLETED`
2. Verify `approvalStatus == APPROVED`
3. Verify `deploymentService.isDeploymentDone(requestId)` — deployment `status == COMPLETED`
4. Set `completionStatus = COMPLETED`
5. Same audit + notification

This is a **manual** step — the requester clicks "Mark Complete" after the deployer has marked the deployment completed.

### 6.4 Read-Only Enforcement

A shared guard method `assertNotCompleted(ChangeRequestEntity cr)` called at the start of every mutation method in `ChangeRequestService`, `RequestUatService`, and `RequestDeploymentService`:

```java
private void assertNotCompleted(ChangeRequestEntity cr) {
    if (cr.getCompletionStatus() == CompletionStatus.COMPLETED) {
        throw new DomainConflictException("REQUEST_COMPLETED",
            "This request is completed and read-only.");
    }
}
```

Comments are also locked when completed — the comment endpoints check `completionStatus` and reject with the same error.

### 6.5 Core-Field Lock After Submission

`ChangeRequestService.update()` currently checks `status.isEditable()` (DRAFT or PENDING_APPROVAL). This is **tightened**: core fields are only editable in DRAFT. After submission, core fields are locked. The `isEditable()` check becomes `status == DRAFT` only. Participant management uses separate methods that check `completionStatus != COMPLETED` instead of `status.isEditable()`.

### 6.6 `assertCanMutate` Update

The current `assertCanMutate()` checks `ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN")` by role name. In the new permission-based model, this changes to check `cr.view.all` permission (Admin override) OR SUPER_ADMIN wildcard. Requesters do not have `cr.view.all`, so they can only mutate CRs they created. `GLOBAL_VIEW_ROLES` is replaced the same way — Auditor has `cr.view.all` for read access but lacks `cr.edit`/`cr.cancel`/`cr.submit` etc., so mutation endpoints reject them at the `@PreAuthorize` layer before `assertCanMutate` is reached.

### 6.7 Frontend Completion Control

`CrCompletionStatusControl.vue`:
- "Mark Complete" button gated by `canMarkComplete` computed: requester or admin, `completionStatus == IN_PROGRESS`, `approvalStatus == APPROVED`, and (for DELIVERY_PIPELINE) `deploymentDone == true`
- When completed, show read-only state with a clear "This request is completed and read-only" banner

---

## 7. Visibility Rules

### 7.1 Visibility Principle

A user can see a request if ANY of:
1. They have `cr.view.all` permission (Admin, Auditor)
2. They are the creator
3. They are an approver on the request
4. They are a watcher on the request
5. They are the deployment assignee
6. They are a UAT approver or UAT watcher

### 7.2 Implementation

- `hasGlobalViewAccess(userPermissions)` -> `userPermissions.contains("cr.view.all")`
- `isViewerAllowed()` now checks `cr_approvers` OR `cr_watchers` for the viewer's userId
- Deployment assignee added to visibility check
- DRAFT requests remain private to creator (return `NOT_FOUND` to others)
- `ChangeRequestService.list()` repository query updated to include `cr_approvers.user_id = viewerId OR cr_watchers.user_id = viewerId OR request_deployments.assignee_id = viewerId` in the WHERE clause for non-global viewers

### 7.3 Auditor Visibility

Auditor has `cr.view.all` + `audit.view` + `audit.export`. They see ALL requests (all statuses). They cannot interact with requests (no approve, no comment, no manage participants).

---

## 8. Custom Role Management

### 8.1 Backend

| Method | Path | Permission | Purpose |
|---|---|---|---|
| `GET` | `/api/v1/roles` | `roles.view` | List all roles (system + custom) with permissions |
| `GET` | `/api/v1/roles/permissions` | `roles.view` | List permission catalogue (all 22 permissions with labels) |
| `POST` | `/api/v1/roles` | `roles.manage` | Create custom role — body: `{ name, description, permissionCodes: [] }` |
| `PATCH` | `/api/v1/roles/{id}` | `roles.manage` | Update custom role (name, description, permissions) |
| `DELETE` | `/api/v1/roles/{id}` | `roles.manage` | Delete custom role (only if no users assigned) |

### 8.2 Validation Rules

1. **Name uniqueness**: Case-insensitive, cannot use reserved names (`Admin`, `Requester`, `Auditor`, `SUPER_ADMIN`)
2. **No full-permission custom roles**: Permission set cannot equal ALL available permissions
3. **No exact-set duplicates**: Two custom roles cannot have the exact same permission set
4. **System role immutability**: System roles (`isSystem = true`) cannot be modified or deleted
5. **Deletion safety**: Cannot delete a custom role if any user is assigned. Throws `ROLE_IN_USE`.
6. **Permission validity**: All `permissionCodes` must exist in the `permissions` table

### 8.3 Frontend

`pages/admin/roles/index.vue` becomes full CRUD:
- Roles list table with `isSystem` badge, description, permission count. System roles show "System" badge, no edit/delete. Custom roles have Edit/Delete buttons.
- Create Role button (admin, `roles.manage`): Opens modal with name, description, permission picker (checkbox grid grouped by category). Live validation for full-permission and duplicate-set checks.
- Edit Role modal: Pre-populated. System roles don't open this.
- Delete Role confirmation: Shows assigned user count. Blocks if > 0.

### 8.4 Permission Catalogue Grouping (UI)

| Group | Permissions |
|---|---|
| Change Requests | `cr.create`, `cr.view`, `cr.view.all`, `cr.edit`, `cr.cancel`, `cr.submit`, `cr.approve`, `cr.manage_participants`, `uat.signoff`, `deployment.execute` |
| Users | `users.view`, `users.manage` |
| Roles | `roles.view`, `roles.manage` |
| Groups | `groups.view`, `groups.manage` |
| Settings | `settings.view`, `settings.manage` |
| SLA | `sla.view`, `sla.manage` |
| Audit | `audit.view`, `audit.export` |

---

## 9. Self-Approval Rule

A requester cannot approve their own change request. Only other users added as approvers can approve. Admins can override (they have `cr.approve` and elevated permissions).

This is the **existing behavior** in `ChangeRequestService.assertCreatorSelfApprovalRule()` — it stays as-is. The rule applies to CR-level approvals only.

In the new permission-based model, "elevated" is determined by: the actor has `cr.view.all` permission (which only Admin has among tenant roles) OR is SUPER_ADMIN (wildcard). This replaces the current `ELEVATED_ROLES = Set.of("ADMIN", "SUPER_ADMIN")` role-name check. Requesters do not have `cr.view.all`, so they cannot self-approve.

**UAT exception**: The UAT requester sign-off (ADR-045) remains. A user who raised a UAT request can sign-off that request in addition to other UAT approvers. This is a separate sign-off mechanism, not an approval.

---

## 10. Shareable Request Links

### 10.1 Copy URL Button

A "Copy URL" button on the change request detail page (`pages/change-requests/[id].vue`). Clicking it copies the full URL (e.g., `https://audita.example.com/change-requests/{uuid}`) to the clipboard. Shows a brief "Copied!" tooltip/confirmation.

### 10.2 Redirect After Login

The frontend already has the redirect mechanism:
- `useAuth().login()` accepts a `redirectTarget` parameter
- The sign-in page reads `route.query.redirect`

The only change needed is in `middleware/auth.global.ts`: when redirecting an unauthenticated user to sign-in, preserve the original path as a `?redirect=` query param:

```typescript
// Before
if (!auth.isAuthenticated) {
    return navigateTo("/auth/sign-in");
}

// After
if (!auth.isAuthenticated) {
    const redirect = encodeURIComponent(to.fullPath);
    return navigateTo(`/auth/sign-in?redirect=${redirect}`);
}
```

When the user logs in, `login()` receives `redirectTarget` and navigates to the original request page.

### 10.3 Edge Cases

- If the redirect target is a DRAFT request the user doesn't own, the backend returns `NOT_FOUND` and the frontend shows an appropriate error.
- If the redirect target no longer exists, same behavior.
- SUPER_ADMIN users redirected to `/platform` regardless (they don't access tenant CRs).

---

## 11. Sample Data Updates

### 11.1 User Roles

`SampleDataService.createSampleUsers()`:
- Remove the `Approver` role lookup (`roleRepository.findByName("Approver")`)
- Users who previously had the Approver role (`david_kim`, `robert_johnson`, `priya_sharma`) are assigned the `Requester` role instead
- 8 sample users: 1 Admin, 6 Requester, 1 Auditor

### 11.2 Approvers

`SampleDataService.createSampleApprovers()`:
- `addApprover()` method signature changes: remove `isRequired` parameter (always required now)
- Remove `isAdHoc` parameter from `CrApproverEntity` constructor
- All sample approvers are required

### 11.3 Watchers

Add `createSampleWatchers()` to `SampleDataService`:
- Add 2-3 watchers to various sample CRs (users who are not already approvers on those CRs)
- Demonstrates the watcher concept in sample data

### 11.4 Deployments

Add `createSampleDeployments()` to `SampleDataService`:
- For DELIVERY_PIPELINE CRs that are APPROVED, create deployment rows with an assignee
- One deployment in `PENDING` status (assignee set, not yet completed)
- One deployment in `COMPLETED` status (assignee set, completedAt set)

### 11.5 Remove Deployment Approvers

Remove all references to `RequestDeploymentApproverEntity` and deployment approver creation from sample data.

### 11.6 Audit Log Entries

Ensure all sample data actions produce corresponding `audit_log` entries. The `removeSampleData()` method must also clean up new tables (`cr_watchers`, `request_uat_watchers`).

---

## 12. Audit Trail Coverage

### 12.1 New Action Types

| Action Type | Trigger |
|---|---|
| `CR_WATCHER_ADDED` | User added as watcher to a CR |
| `CR_WATCHER_REMOVED` | Watcher removed from a CR |
| `CR_WATCHER_PROMOTED` | Watcher moved to approver |
| `CR_APPROVER_DEMOTED` | Approver moved to watcher |
| `UAT_WATCHER_ADDED` | User added as UAT watcher |
| `UAT_WATCHER_REMOVED` | UAT watcher removed |
| `UAT_WATCHER_PROMOTED` | UAT watcher moved to approver |
| `UAT_APPROVER_DEMOTED` | UAT approver moved to watcher |
| `DEPLOYMENT_ASSIGNEE_SET` | Deployer assigned to a deployment |
| `DEPLOYMENT_ASSIGNEE_CHANGED` | Deployer reassigned |
| `DEPLOYMENT_COMPLETED` | Deployer marked deployment completed |
| `ROLE_CREATED` | Custom role created |
| `ROLE_UPDATED` | Custom role updated |
| `ROLE_DELETED` | Custom role deleted |

### 12.2 Existing Actions (Maintained)

All existing audit log actions remain. Every state-changing operation continues to produce both `activity_stream` (CR-scoped) and `audit_log` (global) entries per the pattern in `patterns.md` section 4.2.

### 12.3 Audit Log Entry Requirements

Every new `auditLogService.log()` call must include (per `patterns.md` section 5.1):
- `actionType` — descriptive constant
- `entityType` — lowercase snake_case
- `entityId` — UUID of the affected entity
- `actorId` — UUID of the acting user
- `actorEmail` — resolved via `resolveActorEmail()`
- `payload` — meaningful `Map<String, Object>` (never null, never empty)
- `ipAddress` — `RequestContext.getCurrentIp()`

### 12.4 Frontend Audit Trail Page

Update `pages/audit-trail/index.vue` `ACTION_TYPES` array to include all new action types with their labels.

---

## 13. Logging

### 13.1 Current State

The backend has `logback-spring.xml` with:
- `CONSOLE_JSON` appender (non-dev profiles) — structured JSON via LogstashEncoder
- `CONSOLE_PLAIN` appender (dev profile) — human-readable coloured output
- `io.audita` package at DEBUG level
- MDC keys: `tenant_id`, `user_id`, `request_id`

No file logging exists. No `logs/` directory.

### 13.2 File Logging Addition

Add a `FILE` appender to `logback-spring.xml` using `RollingFileAppender`:

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_FILE:-logs/audita-api.log}</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/audita-api-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>${LOG_FILE_MAX_SIZE:-50MB}</maxFileSize>
        <maxHistory>${LOG_FILE_MAX_HISTORY:-30}</maxHistory>
        <totalSizeCap>${LOG_FILE_TOTAL_SIZE_CAP:-1GB}</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{ISO8601} %-5level [%thread] [%X{tenant_id}|%X{user_id}|%X{request_id}] %logger{36} - %msg%n%rEx</pattern>
    </encoder>
</appender>
```

The file appender is active in **all profiles** (dev and non-dev). The `logs/` directory is relative to the API working directory (`audita-api/logs/`). Compressed archives (`.gz`) for rotated files.

### 13.3 Env-Configurable Log Levels

`application.yml` logging section uses env variables:

```yaml
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    "[io.audita]": ${LOG_LEVEL_APP:DEBUG}
    "[org.flywaydb]": ${LOG_LEVEL_FLYWAY:INFO}
    "[org.hibernate.orm.deprecation]": ERROR
    "[org.hibernate.orm.connections.pooling]": WARN
    "[org.hibernate.orm.query]": ERROR
    "[io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics]": ERROR
```

The `logback-spring.xml` root logger level uses the same env variable:

```xml
<springProperty name="LOG_LEVEL_ROOT" name="logLevelRoot" scope="context" defaultValue="INFO"/>
<root level="${logLevelRoot}">
```

### 13.4 New Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `LOG_LEVEL_ROOT` | `INFO` | Root logger level |
| `LOG_LEVEL_APP` | `DEBUG` | Audita application code level (`io.audita`) |
| `LOG_LEVEL_FLYWAY` | `INFO` | Flyway migration logging |
| `LOG_FILE` | `logs/audita-api.log` | Log file path |
| `LOG_FILE_MAX_SIZE` | `50MB` | Max size per log file before rotation |
| `LOG_FILE_MAX_HISTORY` | `30` | Max days to keep log files |
| `LOG_FILE_TOTAL_SIZE_CAP` | `1GB` | Total max size of all log files |

### 13.5 `.env.example` Updates

Add a new "Logging" section to `.env.example`:

```env
# Logging configuration
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_FLYWAY=INFO
LOG_FILE=logs/audita-api.log
LOG_FILE_MAX_SIZE=50MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE_CAP=1GB
```

### 13.6 Adequate Application Logging

Add structured `LOGGER.debug()` / `LOGGER.info()` calls at key decision points in the modified services:

- **Service entry/exit**: Log method name, key parameters, and outcome at DEBUG level
- **Permission checks**: Log denied access attempts at WARN level (who, what, why denied)
- **State transitions**: Log CR/UAT/deployment status changes at INFO level
- **Error paths**: Log exceptions with context (operation, userId, entityId) at ERROR level
- **Audit log writes**: Log at DEBUG level when audit entries are written (for debugging audit gaps)

These logs are especially useful when `LOG_LEVEL_APP=DEBUG` (the default for dev).

### 13.7 Frontend Debug Logging

In the frontend, use the existing Nuxt `useLogger()` or `console.debug` behind a debug flag. Add debug logging to:
- API call entry/exit in composables (when `process.env.NODE_ENV === 'development'`)
- Permission/role check failures in components
- State transitions in stores

---

## 14. Schema & Migration Strategy

### 14.1 Migration Files Modified

| File | Changes |
|---|---|
| `V1__create_tenant_schema.sql` | Add 4 new permissions to seed (`cr.view.all`, `cr.manage_participants`, `uat.signoff`, `deployment.execute`). Remove `Approver` system role (id `...0003`). Update `role_permissions` for 3 roles. Remove `is_required` from `cr_approvers`. Remove `is_ad_hoc` from `cr_approvers`. Add `cr_watchers` table. |
| `V3__requests_workflow_core.sql` | Remove `is_required` from `request_uat_approvers`. Drop `request_deployment_approvers` table. Add `assignee_id` + `completed_at` to `request_deployments`. Change deployment status CHECK to `('PENDING','COMPLETED','CANCELLED')`. Add `request_uat_watchers` table. |
| `V10__uat_requester_sign_off.sql` | Keep as-is (still relevant). |

### 14.2 New Tables Summary

| Table | Location | Purpose |
|---|---|---|
| `cr_watchers` | V1 | CR-level watchers |
| `request_uat_watchers` | V3 | UAT-level watchers |

### 14.3 Removed Tables

| Table | Was In | Reason |
|---|---|---|
| `request_deployment_approvers` | V3 | Replaced by single assignee on `request_deployments` |

### 14.4 Removed Columns

| Column | Table | Reason |
|---|---|---|
| `is_required` | `cr_approvers` | All approvers are required |
| `is_ad_hoc` | `cr_approvers` | No longer needed |
| `is_required` | `request_uat_approvers` | All approvers are required |

---

## 15. Backend Changes Summary

### 15.1 Controllers (16 files, 88 annotations)

All `@PreAuthorize` annotations change from `hasRole()`/`hasAnyRole()` to `hasAuthority()` or `@authz.hasPermission()`.

New endpoints:
- `ChangeRequestController`: watcher CRUD + move endpoints (5 new)
- `RequestUatController`: UAT watcher CRUD + move endpoints (5 new)
- `RequestDeploymentController`: assignee endpoint, complete endpoint (2 new, replacing approve/reject)
- `RoleController`: `GET /permissions`, `DELETE /{id}` (2 new)

Removed endpoints:
- `ChangeRequestController`: `PATCH /{id}/approvers/{approverId}/requirement`
- `RequestUatController`: `PATCH /{id}/uat/approvers/{approverId}/requirement`
- `RequestDeploymentController`: approve, reject endpoints

### 15.2 Services

| Service | Changes |
|---|---|
| `ChangeRequestService` | Add watcher methods. Add move approver<->watcher. Remove `isRequired` logic. Simplify `evaluateApprovalClosure`. Add `assertNotCompleted()`. Tighten core-field edit to DRAFT only. Update visibility checks. Replace role-set checks with permission checks. Remove `updateApproverRequirement()`. Add audit logging for all new actions. Add debug logging. |
| `RequestUatService` | Add UAT watcher methods. Add move UAT approver<->watcher. Remove `isRequired` logic. Add `assertNotCompleted()`. Add audit logging for all new actions. Add debug logging. |
| `RequestDeploymentService` | Remove approver methods. Add assignee methods. Add `completeDeployment()`. Remove `approveDeployment()`/`rejectDeployment()`. Update `isDeploymentDone()`. Add audit logging. Add debug logging. |
| `RoleService` | Update `denyPermissionOverlap()`. Add `deleteRole()` with `ROLE_IN_USE` check. Add `listPermissions()`. Add debug logging. |
| `UserService` | Remove `Approver` role from any hardcoded references. Add debug logging. |
| `SampleDataService` | Update user roles (no Approver). Update approver creation (no `isRequired`). Add watcher sample data. Add deployment sample data. Remove deployment approver sample data. |

### 15.3 Entities

| Entity | Changes |
|---|---|
| `CrApproverEntity` | Remove `isRequired`, `isAdHoc` |
| `RequestUatApproverEntity` | Remove `isRequired` |
| `RequestDeploymentEntity` | Remove `approvers` collection. Add `assignee` (ManyToOne UserEntity). |
| `RequestDeploymentApproverEntity` | **Delete entirely** |
| `CrWatcherEntity` | **New** |
| `RequestUatWatcherEntity` | **New** |
| `UserEntity` | No structural change (already supports multi-role via `roles` set) |
| `RoleEntity` | No structural change |

### 15.4 Security

| File | Changes |
|---|---|
| `UserPrincipal.java` | Add `*` wildcard authority for SUPER_ADMIN in `ofSuperAdmin()` |
| `SecurityConfig.java` | Register `@authz` bean for SpEL permission evaluation |
| New: `AuthorizationLogic.java` | Bean registered as `authz` with `hasPermission(authentication, permission)` method. Returns true if user has the permission OR has `*` wildcard. |

### 15.5 Configuration

| File | Changes |
|---|---|
| `logback-spring.xml` | Add `FILE` appender with `RollingFileAppender`. Env-configurable log levels. |
| `application.yml` | Env variables for log levels. |
| `.env.example` | Add logging section. |

---

## 16. Frontend Changes Summary

| File | Changes |
|---|---|
| `types/index.ts` | Remove `Approver` from `UserRole`. Add `Watcher` types. Update `DeploymentStatus` to `PENDING \| COMPLETED \| CANCELLED`. Add `assignee` to `Deployment` type. Remove `isRequired` from approver types. |
| `stores/auth.ts` | Remove `Approver` from role checks. Load permissions from JWT. Add permission-based getters (`can(permission)`). |
| `composables/useChangeRequests.ts` | Add watcher methods (add/remove/list/promote/demote). Add deployment assignee method. Add deployment complete method. Remove `updateApproverRequirement` methods. Remove `isRequired` from `addApprover`/`addUatApprover`. |
| `pages/change-requests/[id].vue` | Remove Required/Optional toggle. Add watcher management UI. Add watcher<->approver move buttons. Add Copy URL button. Update `canManageApprovers` to check `completionStatus`. Lock core fields after submit. Show read-only banner when completed. |
| `pages/change-requests/new.vue` | No structural change (approvers added on detail page). |
| `components/cr/CrUatPanel.vue` | Remove Required/Optional toggle. Add UAT watcher management. Add UAT watcher<->approver move. |
| `components/cr/CrDeploymentPanel.vue` | Remove approvers list + approve/reject UI. Add assignee selection UI. Add "Mark Deployment Completed" button (assignee only). Remove reject modal. |
| `components/cr/CrCompletionStatusControl.vue` | Gate by `completionStatus` + `canMarkComplete` (requester/admin). |
| `pages/admin/roles/index.vue` | Add Create/Edit/Delete custom role UI with permission picker. |
| `pages/admin/users/index.vue` | Already works — custom roles appear in dropdown. No change needed. |
| `composables/useRoleGuard.ts` | Update for permission-based checks where applicable. |
| `middleware/can-create-cr.ts` | Change from role check to permission check (`cr.create`). |
| `middleware/auth.global.ts` | Preserve `?redirect=` query param when redirecting to sign-in. |
| `pages/auth/sign-in.vue` | Already reads `route.query.redirect` — no change needed. |
| `pages/audit-trail/index.vue` | Update `ACTION_TYPES` array with new action types. |

---

## 17. Testing Considerations

- Permission enforcement: verify each endpoint rejects users without the required permission and accepts users with it
- Custom role overlap validation: test full-permission rejection, exact-set duplicate rejection, partial overlap acceptance
- Watcher mutual exclusivity: verify a user can't be both approver and watcher
- Move operations: verify watcher->approver and approver->watcher transitions
- Deployment assignment and completion: verify assignee-only completion, reassignment while pending
- Read-only boundary: verify all mutations blocked when `completionStatus = COMPLETED`, comments also blocked
- Visibility: verify requester can't see requests they're not part of, watchers can see requests they're watching, DRAFT is private to creator
- Self-approval: verify non-admin creator cannot approve own CR, admin can
- Copy URL + redirect: verify URL copies correctly, redirect works after login
- Audit trail: verify all new actions produce audit log entries with complete payloads
- File logging: verify log files are created and rotated

---

## 18. Risk Assessment

| Risk | Mitigation |
|---|---|
| Breaking existing API consumers with permission-based enforcement | DB is dropped — no existing data. All clients (the frontend) are updated in the same sprint. |
| Custom role with partial overlap causing unexpected access | Partial overlap is allowed by design. The permission checks are explicit per endpoint — a custom role only gets what its permissions grant. |
| Watcher notification spam | Watchers only get status-change and new-comment notifications, same as approvers. No additional notification types. |
| Deployment stuck if assignee unavailable | Requester/admin can reassign at any time while status = PENDING. |
| Audit trail gaps | All new actions are enumerated in section 12. Each has explicit audit log calls. Testing verifies coverage. |
| Log file disk growth | Daily rotation + max history + total size cap. Env-configurable for production tuning. |
