# Requests Conditional Workflow - Implementation Specification

**Date:** 2026-06-04  
**Status:** Approved for planning, not started  
**Owners:** Developer 1 (API/domain), Developer 2 (Web UX)

---

## 1. Goal

Implement a conditional Requests workflow that supports two request modes:

1. `APPROVAL_ONLY` for simple operational requests.
2. `DELIVERY_PIPELINE` for technical requests that require `Request -> UAT -> Deployment`.

This must preserve API/route compatibility (`/change-requests`) while renaming UI language from "Change Requests" to "Requests".

---

## 2. Locked Business Rules

The following rules are already approved and must be treated as non-negotiable:

1. UAT create/edit/promotion only when main Request approval status is `APPROVED`.
2. UAT create/promotion actor can be:
   - Request creator, or
   - Admin/Super Admin, or
   - Requester-role user who is already in main request approver list.
3. UAT promotion requires all required UAT approvers approved (optional approvers do not block).
4. Promotion creates Deployment; users cannot create Deployment directly.
5. UAT becomes read-only after promotion, but UAT comments remain allowed.
6. Deployment approvers auto-populate from main Request approvers + UAT approvers, deduplicated, required/optional preserved.
7. Completion status can be set to `COMPLETED` by creator/Admin/Super Admin only.
8. For `DELIVERY_PIPELINE`, completion can be set to `COMPLETED` only after deployment is done.
9. For `APPROVAL_ONLY`, completion can be set to `COMPLETED` once approval status is `APPROVED`.
10. Request links are bidirectional.
11. Request ID format is immutable display ID: `<PREFIX>-<zero-padded sequence>` (example: `RQ-000123`).
12. Prefix changes only affect future requests.
13. Department list is admin-managed master data (active/inactive), no free-text departments on request forms.
14. Every UAT and Deployment action must write to both activity stream and audit trail.

---

## 3. Scope

### In Scope

- Data model additions for workflow mode, dual statuses, display ID, departments, request links, UAT, Deployment.
- API changes for request create/update/read/list and new UAT/Deployment operations.
- Admin Settings: Department management and request ID prefix configuration.
- Request Create/Edit: departments, linked requests, workflow mode.
- Request Detail: new UAT and Deployment tabs, inline forms, approvers/comments/approval flows.
- Activity and audit event expansion.

### Out of Scope

- Full Kanban board UI.
- Route rename from `/change-requests` to `/requests` (deferred; compatibility mode retained).
- Cross-request workflow templates beyond two modes.

---

## 4. Architecture Summary

### 4.1 Core Request Model Changes

Add to `change_requests`:

- `display_id VARCHAR(32) UNIQUE NOT NULL`
- `approval_status VARCHAR(32) NOT NULL`
- `completion_status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS'`
- `workflow_mode VARCHAR(32) NOT NULL DEFAULT 'APPROVAL_ONLY'`
- `request_department_id UUID NULL`
- `destination_department_id UUID NULL`

Keep current `status` column during migration window for backward compatibility; map/phase out in service layer after read/write parity is complete.

### 4.2 New Tenant Tables

- `departments`
- `request_links`
- `request_uat`
- `request_uat_approvers`
- `request_uat_comments`
- `request_deployments`
- `request_deployment_approvers`
- `request_deployment_comments`

### 4.3 Admin Settings Keys in `org_settings`

- `request.id_prefix` (default `RQ`)
- `request.id_sequence` (monotonic integer; stored as text)

Do not infer or derive sequence from UUIDs.

---

## 5. Data Model Blueprint (SQL Guide)

Use this as implementation baseline in new tenant migration file.

```sql
-- request id + workflow extension
ALTER TABLE change_requests
  ADD COLUMN IF NOT EXISTS display_id VARCHAR(32),
  ADD COLUMN IF NOT EXISTS approval_status VARCHAR(32),
  ADD COLUMN IF NOT EXISTS completion_status VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
  ADD COLUMN IF NOT EXISTS workflow_mode VARCHAR(32) NOT NULL DEFAULT 'APPROVAL_ONLY',
  ADD COLUMN IF NOT EXISTS request_department_id UUID,
  ADD COLUMN IF NOT EXISTS destination_department_id UUID;

-- department master
CREATE TABLE IF NOT EXISTS departments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(120) NOT NULL,
  code VARCHAR(32),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  display_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_departments_name UNIQUE (name)
);

-- request links (store canonical pair order: lower UUID in request_id_a)
CREATE TABLE IF NOT EXISTS request_links (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id_a UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
  request_id_b UUID NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
  linked_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT ck_request_links_no_self CHECK (request_id_a <> request_id_b),
  CONSTRAINT uq_request_links_pair UNIQUE (request_id_a, request_id_b)
);
```

```sql
-- UAT
CREATE TABLE IF NOT EXISTS request_uat (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
  title VARCHAR(255) NOT NULL,
  details TEXT,
  status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
  read_only BOOLEAN NOT NULL DEFAULT FALSE,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS request_uat_approvers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  uat_id UUID NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  is_required BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  position INT NOT NULL,
  decided_at TIMESTAMPTZ,
  rejection_reason TEXT,
  CONSTRAINT uq_uat_approver UNIQUE (uat_id, user_id)
);

CREATE TABLE IF NOT EXISTS request_uat_comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  uat_id UUID NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
  author_id UUID REFERENCES users(id),
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

```sql
-- Deployment
CREATE TABLE IF NOT EXISTS request_deployments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
  uat_id UUID NOT NULL UNIQUE REFERENCES request_uat(id),
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
  created_by UUID REFERENCES users(id),
  promoted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  completed_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS request_deployment_approvers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  deployment_id UUID NOT NULL REFERENCES request_deployments(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  is_required BOOLEAN NOT NULL DEFAULT TRUE,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  position INT NOT NULL,
  decided_at TIMESTAMPTZ,
  rejection_reason TEXT,
  CONSTRAINT uq_deployment_approver UNIQUE (deployment_id, user_id)
);

CREATE TABLE IF NOT EXISTS request_deployment_comments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  deployment_id UUID NOT NULL REFERENCES request_deployments(id) ON DELETE CASCADE,
  author_id UUID REFERENCES users(id),
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 6. API Contract Changes

### 6.1 Existing Request APIs (compatibility path)

- Continue to use `/api/v1/change-requests`.
- Add response fields:
  - `displayId`
  - `approvalStatus`
  - `completionStatus`
  - `workflowMode`
  - `requestDepartment`
  - `destinationDepartment`

### 6.2 New Endpoints

- `GET /api/v1/settings/departments`
- `POST /api/v1/settings/departments`
- `PATCH /api/v1/settings/departments/{id}`
- `GET /api/v1/change-requests/search?query=...` (for linking)
- `PUT /api/v1/change-requests/{id}/links`
- `GET /api/v1/change-requests/{id}/uat`
- `POST /api/v1/change-requests/{id}/uat`
- `PATCH /api/v1/change-requests/{id}/uat`
- `POST /api/v1/change-requests/{id}/uat/approvers`
- `POST /api/v1/change-requests/{id}/uat/comments`
- `POST /api/v1/change-requests/{id}/uat/approve`
- `POST /api/v1/change-requests/{id}/uat/reject`
- `POST /api/v1/change-requests/{id}/uat/promote`
- `GET /api/v1/change-requests/{id}/deployment`
- `POST /api/v1/change-requests/{id}/deployment/comments`
- `POST /api/v1/change-requests/{id}/deployment/approve`
- `POST /api/v1/change-requests/{id}/deployment/reject`

---

## 7. State Machine Guide

### 7.1 Request Approval Status

`DRAFT -> PENDING_APPROVAL -> APPROVED | REJECTED | CANCELLED`

### 7.2 Completion Status

`IN_PROGRESS -> COMPLETED`

Guard:

- If `workflowMode=APPROVAL_ONLY`: allowed when `approvalStatus=APPROVED`.
- If `workflowMode=DELIVERY_PIPELINE`: allowed when deployment `status=APPROVED`.

### 7.3 UAT Status

`IN_PROGRESS -> APPROVED | REJECTED -> PROMOTED`

Promotion guard:

- parent request `approvalStatus=APPROVED`
- all required UAT approvers have `APPROVED`

### 7.4 Deployment Status

`PENDING_APPROVAL -> APPROVED | REJECTED`

"Done" condition:

- all required deployment approvers approved

---

## 8. Authorization Matrix

| Action | Creator | Admin/Super Admin | Requester+MainApprover | Other Approver | Auditor |
| --- | --- | --- | --- | --- | --- |
| Edit request draft | Yes | Yes | No | No | No |
| Initiate UAT (after APPROVED) | Yes | Yes | Yes | No | No |
| Edit UAT (before promotion) | Yes | Yes | Yes | No | No |
| Comment on UAT | Yes | Yes | Yes | Yes | Yes |
| Approve UAT | If in UAT approvers | Yes if in approvers or elevated override policy | If in UAT approvers | If in UAT approvers | No |
| Promote UAT | Yes | Yes | Yes | No | No |
| Comment on Deployment | Yes | Yes | Yes | Yes | Yes |
| Approve Deployment | If in deployment approvers | Yes if in approvers or elevated override policy | If in deployment approvers | If in deployment approvers | No |
| Set completion=COMPLETED | Yes | Yes | No | No | No |

---

## 9. Activity and Audit Event Catalog

Log to both `activity_stream` and `audit_log`:

- `REQ_UAT_CREATED`
- `REQ_UAT_UPDATED`
- `REQ_UAT_APPROVER_ADDED`
- `REQ_UAT_APPROVED`
- `REQ_UAT_REJECTED`
- `REQ_UAT_COMMENT_ADDED`
- `REQ_UAT_PROMOTED`
- `REQ_DEPLOYMENT_CREATED`
- `REQ_DEPLOYMENT_APPROVER_ADDED`
- `REQ_DEPLOYMENT_APPROVED`
- `REQ_DEPLOYMENT_REJECTED`
- `REQ_DEPLOYMENT_COMMENT_ADDED`
- `REQ_COMPLETION_STATUS_CHANGED`
- `REQ_LINKS_UPDATED`
- `REQ_DEPARTMENT_CHANGED`
- `REQ_ID_PREFIX_UPDATED`

Payload guidance:

- Always include `requestId`, `requestDisplayId`, `actorId`, `actorEmail`.
- Include stage IDs (`uatId`, `deploymentId`) when relevant.
- Include before/after for mutating fields when possible.

---

## 10. Frontend Delivery Map

Primary files to modify:

- `audita-web/pages/change-requests/index.vue`
- `audita-web/pages/change-requests/new.vue`
- `audita-web/pages/change-requests/[id].vue`
- `audita-web/composables/useChangeRequests.ts`
- `audita-web/pages/admin/settings/index.vue`
- `audita-web/composables/adminSettingsForm.ts`
- `audita-web/components/cr/CrStatusBadge.vue`
- `audita-web/layouts/default.vue`
- `audita-web/components/shared/AppSidebar.vue`
- `audita-web/types/index.ts`

Decomposition requirement:

- Split UAT and Deployment UI from `pages/change-requests/[id].vue` into focused components:
  - `components/cr/CrUatPanel.vue`
  - `components/cr/CrDeploymentPanel.vue`
  - `components/cr/CrCompletionStatusControl.vue`

---

## 11. Implementation Cookbook for Less Powerful LLMs

### 11.1 Safe Order of Work

1. Add DB migration and entities first.
2. Add enums and DTO fields next.
3. Add service guards/state transitions.
4. Add controller endpoints.
5. Add frontend type updates.
6. Add frontend composable methods.
7. Add page-level UI updates.
8. Add tests at each layer.

Do not start UI changes before backend contracts compile and tests pass.

### 11.2 ID Generation Pseudocode

```java
@Transactional
public String nextDisplayId() {
  String prefix = readOrgSetting("request.id_prefix", "RQ");
  long sequence = parseLong(readOrgSetting("request.id_sequence", "0"));
  long next = sequence + 1;
  saveOrgSetting("request.id_sequence", Long.toString(next));
  return prefix + "-" + String.format("%06d", next);
}
```

Rule: call exactly once during request creation; persist to `change_requests.display_id`.

### 11.3 Bidirectional Link Canonicalization

```java
UUID left = requestA.compareTo(requestB) < 0 ? requestA : requestB;
UUID right = left.equals(requestA) ? requestB : requestA;
// persist unique pair (left, right)
```

### 11.4 UAT Promotion Guard

```java
if (request.getApprovalStatus() != APPROVED) deny();
if (!allRequiredUatApproversApproved(uatId)) deny();
createDeploymentFromUat(uat);
markUatReadOnly(uatId);
```

### 11.5 Common Mistakes to Avoid

- Do not rewrite existing `display_id` after prefix changes.
- Do not allow direct Deployment creation endpoint.
- Do not couple completion status with approval status in one enum.
- Do not allow free-text departments in request form when master list exists.
- Do not log UAT/Deployment only in activity stream; audit log is also mandatory.

---

## 12. Verification Strategy

### Backend

- Service-level transition tests for UAT/deployment guards.
- Auth matrix tests for creator/admin/requester+approver rules.
- Migration tests for existing data backfill.
- Repository tests for bidirectional link uniqueness.

### Frontend

- Typecheck after new DTO fields.
- Request list tests for dual status rendering.
- Detail tests for UAT tab gating, promotion button visibility, deployment read model.
- Admin settings tests for departments and prefix save flow.

### Commands

```bash
cd audita-api && ./gradlew :api:test :infrastructure:test --no-daemon
cd audita-web && pnpm test
cd audita-web && pnpm -s nuxi typecheck
cd audita-web && pnpm build
```

---

## 13. Rollout Notes

- Keep route/API compatibility in this release.
- Expose new fields without breaking old clients.
- Use feature flags only if partial deployment risk exists.
- Plan route alias (`/requests`) as a later, isolated migration.
