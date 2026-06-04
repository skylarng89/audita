# Sprint 15 Requests Workflow Expansion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver Sprint 15 (`RW15-001` to `RW15-024`) with a compatibility-safe Requests workflow that supports both `APPROVAL_ONLY` and `DELIVERY_PIPELINE` modes.

**Architecture:** Extend existing `/change-requests` domain with immutable display IDs, dual statuses, department master data, bidirectional links, and stage entities for UAT/Deployment. Keep route/API compatibility while introducing new fields and stage endpoints. Enforce state-machine and authorization guardrails in service layer, then wire UX and tests incrementally by phase.

**Tech Stack:** Java 25, Spring Boot 4, Hibernate/JPA, Flyway, PostgreSQL, Nuxt 3/Vue 3, TypeScript, Vitest, Gradle, pnpm.

---

## Execution Protocol for Agents/Subagents

- Run phases in order: A -> B -> C -> D -> E.
- Each task must satisfy TDD loop: failing test -> minimal implementation -> passing test.
- Open one subagent per task; do not merge multiple IDs in one subagent unless explicitly paired in this plan.
- After each task, post verification evidence (command + pass/fail + key assertion).
- Do not rename compatibility routes (`/change-requests`) in Sprint 15.

Reusable verification commands:

```bash
cd audita-api && ./gradlew :api:test :infrastructure:test --no-daemon
cd audita-web && pnpm test
cd audita-web && pnpm -s nuxi typecheck
cd audita-web && pnpm build
```

---

## Phase A - Data Contracts

### Task RW15-001: Request display ID and dual-status migration

**Files:**
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V1__create_tenant_schema.sql`
- Create: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/ChangeRequestEntity.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/persistence/entity/ChangeRequestEntityTest.java`

- [ ] Add failing migration/entity tests for `display_id`, `approval_status`, `completion_status`, `workflow_mode`.
- [ ] Run infrastructure tests and confirm failure due to missing columns or mappings.
- [ ] Add migration + entity fields + enum mappings with defaults/backfill constraints.
- [ ] Re-run targeted tests until pass.
- [ ] Record migration assumptions in test comments and task notes.

### Task RW15-002: Department master table and request FK wiring

**Files:**
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/DepartmentEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/DepartmentRepository.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/persistence/repository/DepartmentRepositoryTest.java`

- [ ] Add failing repository test for active/inactive department retrieval and unique name rule.
- [ ] Add schema objects + FK references from `change_requests` to departments.
- [ ] Implement entity/repository with ordering query (`display_order`, `name`).
- [ ] Run tests for department CRUD/read constraints.
- [ ] Validate migration replay on empty tenant schema.

### Task RW15-003: Bidirectional links schema and canonical uniqueness

**Files:**
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestLinkEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/RequestLinkRepository.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/persistence/repository/RequestLinkRepositoryTest.java`

- [ ] Add failing tests for no-self-link and duplicate pair prevention.
- [ ] Implement canonical pair schema (`request_id_a`, `request_id_b`) unique constraint.
- [ ] Implement repository methods to list links in both directions.
- [ ] Run tests and verify pair normalization behavior.
- [ ] Document canonicalization helper in test fixture.

### Task RW15-004: UAT/Deployment schema foundation

**Files:**
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestUatEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestDeploymentEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestUatApproverEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestDeploymentApproverEntity.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/persistence/entity/RequestWorkflowEntitiesTest.java`

- [ ] Write failing entity tests for one-UAT-per-request and one-deployment-per-request.
- [ ] Add stage tables and FK constraints.
- [ ] Implement JPA entities with correct `@Column(name=...)` mapping.
- [ ] Run persistence tests and verify unique constraints.
- [ ] Confirm migration script remains idempotent.

---

## Phase B - Workflow Engine

### Task RW15-005: Immutable display ID generator

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/TenantService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/ChangeRequestServiceSecurityTest.java`

- [ ] Add failing service tests for display ID format and prefix mutation safety.
- [ ] Implement `request.id_prefix` + `request.id_sequence` sequence generation.
- [ ] Ensure display ID assigned exactly once on create.
- [ ] Run service tests for prefix-change-forward-only behavior.
- [ ] Verify concurrent create behavior does not duplicate IDs.

### Task RW15-006: Mode-aware request lifecycle guardrails

**Files:**
- Modify: `audita-api/domain/src/main/java/io/audita/domain/model/ChangeRequestStatus.java`
- Create: `audita-api/domain/src/main/java/io/audita/domain/model/RequestWorkflowMode.java`
- Create: `audita-api/domain/src/main/java/io/audita/domain/model/CompletionStatus.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/ChangeRequestWorkflowModeTest.java`

- [ ] Add failing tests for completion status transitions per mode.
- [ ] Implement mode-aware guards in service layer.
- [ ] Ensure legacy approval flow behavior remains unchanged.
- [ ] Run tests for both `APPROVAL_ONLY` and `DELIVERY_PIPELINE`.
- [ ] Add negative tests for invalid transition attempts.

### Task RW15-007: UAT lifecycle service with promotion lock

**Files:**
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestUatService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestUatServiceTest.java`

- [ ] Add failing tests for create/edit gate (`approvalStatus=APPROVED`) and read-only-after-promotion.
- [ ] Implement UAT create/update/promotion methods.
- [ ] Enforce required-approver-only promotion criteria.
- [ ] Run tests for allowed and denied actor paths.
- [ ] Verify comments remain allowed after read-only lock.

### Task RW15-008: Deployment lifecycle and inherited approvers

**Files:**
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestDeploymentService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestUatService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestDeploymentServiceTest.java`

- [ ] Write failing tests for promotion-created deployment only.
- [ ] Implement approver inheritance merge (dedupe + required flag preservation).
- [ ] Implement deployment done detection (`all required approved`).
- [ ] Run tests for approval/rejection state transitions.
- [ ] Verify completion status guard dependency uses deployment done state.

---

## Phase C - API Surface

### Task RW15-009: Request DTO/controller expansion

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/ChangeRequestResponse.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/request/CreateChangeRequestRequest.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/request/UpdateChangeRequestRequest.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java`
- Test: `audita-api/api/src/test/java/io/audita/api/controller/ChangeRequestControllerIdempotencyWebMvcTest.java`

- [ ] Add failing API tests for new fields and compatibility-safe responses.
- [ ] Add DTO fields for workflow mode, departments, linked request IDs.
- [ ] Wire controller/service mapping for dual statuses.
- [ ] Run API controller tests and ensure legacy fields still resolve.
- [ ] Validate request list filtering still works.

### Task RW15-010: Department admin endpoints

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/TenantSettingsController.java`
- Create: `audita-api/api/src/main/java/io/audita/api/controller/DepartmentAdminController.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/response/DepartmentResponse.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/request/UpsertDepartmentRequest.java`
- Test: `audita-api/api/src/test/java/io/audita/api/controller/DepartmentAdminControllerWebMvcTest.java`

- [ ] Add failing WebMvc tests for create/list/update/deactivate department flows.
- [ ] Implement department endpoints under settings/admin scope.
- [ ] Enforce active-only retrieval for request forms.
- [ ] Run controller tests and authorization checks.
- [ ] Verify no free-text bypass path remains in request API.

### Task RW15-011: Request link search and upsert APIs

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestLinkSearchResponse.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestLinkService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestLinkServiceTest.java`

- [ ] Add failing tests for search by display ID/title and canonical upsert behavior.
- [ ] Implement search endpoint for UI picker.
- [ ] Implement full-set upsert with self-link rejection.
- [ ] Run tests for bidirectional retrieval behavior.
- [ ] Verify audit/activity events are emitted for link updates.

### Task RW15-012: UAT/Deployment API endpoints

**Files:**
- Create: `audita-api/api/src/main/java/io/audita/api/controller/RequestUatController.java`
- Create: `audita-api/api/src/main/java/io/audita/api/controller/RequestDeploymentController.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/request/CreateRequestUatRequest.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestUatResponse.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestDeploymentResponse.java`
- Test: `audita-api/api/src/test/java/io/audita/api/controller/RequestWorkflowControllersWebMvcTest.java`

- [ ] Add failing controller tests for UAT/deployment actions.
- [ ] Implement stage endpoints for comments, approvals, promotion.
- [ ] Enforce authorization matrix in controller/service calls.
- [ ] Run WebMvc tests for allow/deny paths.
- [ ] Confirm direct deployment creation endpoint is absent.

---

## Phase D - Frontend UX

### Task RW15-013: UI label rename to Requests

**Files:**
- Modify: `audita-web/layouts/default.vue`
- Modify: `audita-web/components/shared/AppSidebar.vue`
- Modify: `audita-web/pages/change-requests/index.vue`
- Modify: `audita-web/pages/change-requests/new.vue`
- Modify: `audita-web/pages/change-requests/[id].vue`

- [ ] Add/update failing UI tests for label expectations.
- [ ] Rename headings/buttons/nav text to "Requests".
- [ ] Keep all links/routes on `/change-requests`.
- [ ] Run `pnpm test` and route smoke checks.
- [ ] Verify accessibility labels still describe controls accurately.

### Task RW15-014: Departments + workflow mode on create/edit

**Files:**
- Modify: `audita-web/pages/change-requests/new.vue`
- Modify: `audita-web/pages/change-requests/[id].vue`
- Modify: `audita-web/composables/useChangeRequests.ts`
- Modify: `audita-web/types/index.ts`
- Test: `audita-web/tests/change-requests/request-form-workflow.spec.ts`

- [ ] Write failing form tests for workflow mode and department dropdowns.
- [ ] Add request/destination department selectors (active departments only).
- [ ] Add workflow mode selector and conditional field rendering.
- [ ] Run tests/typecheck and verify payload shape.
- [ ] Ensure legacy requests without mode data render safely.

### Task RW15-015: Linked requests picker

**Files:**
- Modify: `audita-web/pages/change-requests/new.vue`
- Modify: `audita-web/pages/change-requests/[id].vue`
- Create: `audita-web/components/cr/RequestLinkPicker.vue`
- Test: `audita-web/tests/change-requests/request-link-picker.spec.ts`

- [ ] Add failing component tests for search/select/remove behavior.
- [ ] Build reusable picker with debounced query.
- [ ] Wire create/edit pages to link upsert API.
- [ ] Run tests and validate chip dedupe behavior.
- [ ] Verify bidirectional link reflection in detail view.

### Task RW15-016: List table with dual status columns

**Files:**
- Modify: `audita-web/pages/change-requests/index.vue`
- Modify: `audita-web/components/cr/CrStatusBadge.vue`
- Create: `audita-web/components/cr/CrCompletionStatusBadge.vue`
- Test: `audita-web/tests/change-requests/request-list-status-columns.spec.ts`

- [ ] Add failing list test for `Approval Status` and `Completion Status` columns.
- [ ] Implement new column rendering and badge component.
- [ ] Add filter update hooks if needed.
- [ ] Run tests and verify pagination unaffected.
- [ ] Confirm display ID prefix shown from API `displayId`.

### Task RW15-017: UAT tab and inline initiation form

**Files:**
- Modify: `audita-web/pages/change-requests/[id].vue`
- Create: `audita-web/components/cr/CrUatPanel.vue`
- Test: `audita-web/tests/change-requests/uat-tab.spec.ts`

- [ ] Add failing tests for UAT tab gating and one-UAT constraint.
- [ ] Build UAT panel with create/edit form and approver controls.
- [ ] Enforce read-only behavior after promotion state.
- [ ] Run tests and verify actor gating in UI.
- [ ] Ensure request tab counts update when UAT comments added.

### Task RW15-018: Deployment tab from promotion-only flow

**Files:**
- Modify: `audita-web/pages/change-requests/[id].vue`
- Create: `audita-web/components/cr/CrDeploymentPanel.vue`
- Test: `audita-web/tests/change-requests/deployment-tab.spec.ts`

- [ ] Add failing tests for hidden deployment create action.
- [ ] Build deployment panel that renders only promoted deployment data.
- [ ] Show inherited approver list and deployment status.
- [ ] Run tests for promoted/unpromoted states.
- [ ] Validate no UI path exists for direct deployment creation.

### Task RW15-019: Stage comment + approval interactions

**Files:**
- Modify: `audita-web/components/cr/CrUatPanel.vue`
- Modify: `audita-web/components/cr/CrDeploymentPanel.vue`
- Modify: `audita-web/composables/useChangeRequests.ts`
- Test: `audita-web/tests/change-requests/request-stage-actions.spec.ts`

- [ ] Add failing tests for UAT/deployment comment and approve/reject actions.
- [ ] Implement composable methods for stage endpoints.
- [ ] Add UI forms and action buttons with pending state handling.
- [ ] Run tests/typecheck/build.
- [ ] Verify error messages map from API responses.

### Task RW15-020: Decompose monolithic request detail page

**Files:**
- Modify: `audita-web/pages/change-requests/[id].vue`
- Create: `audita-web/components/cr/CrCompletionStatusControl.vue`
- Create: `audita-web/components/cr/CrRequestOverviewPanel.vue`
- Test: `audita-web/tests/change-requests/request-detail-decomposition.spec.ts`

- [ ] Add failing smoke test that asserts child components render by tab/state.
- [ ] Extract UAT/deployment/completion controls from page file.
- [ ] Keep behavior parity for existing approver/activity/comments tabs.
- [ ] Run full frontend verification gate.
- [ ] Ensure file size and responsibility boundaries are improved.

---

## Phase E - Quality Gate

### Task RW15-021: Activity stream parity for stage actions

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestUatService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestDeploymentService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestWorkflowActivityTest.java`

- [ ] Add failing tests that assert activity events for all UAT/deployment actions.
- [ ] Emit standardized event types and payload keys.
- [ ] Run service tests and verify event ordering.
- [ ] Validate payload includes request display ID and actor fields.
- [ ] Update activity summary formatter mapping if needed.

### Task RW15-022: Audit trail parity for stage actions

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuditLogService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestUatService.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestDeploymentService.java`
- Test: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestWorkflowAuditTest.java`

- [ ] Add failing tests for audit entries on stage actions.
- [ ] Emit audit logs with actor/context and before/after where applicable.
- [ ] Run tests ensuring no missing audit action types.
- [ ] Verify audit entity type naming is consistent with existing conventions.
- [ ] Spot-check audit payload redaction/no sensitive leak.

### Task RW15-023: Backend transition + auth matrix regression suite

**Files:**
- Create: `audita-api/infrastructure/src/test/java/io/audita/infrastructure/service/RequestWorkflowAuthorizationTest.java`
- Modify: `audita-api/api/src/test/java/io/audita/api/integration/AllSprintsE2ETest.java`

- [ ] Add failing matrix tests for creator/admin/requester+approver/approver/auditor paths.
- [ ] Implement missing guards surfaced by failing tests.
- [ ] Run targeted and full backend test suites.
- [ ] Confirm completion status gates per workflow mode.
- [ ] Add regression case for prefix change not mutating historical display IDs.

### Task RW15-024: Frontend regression suite for tabs/status/linking

**Files:**
- Create: `audita-web/tests/change-requests/requests-workflow-e2e.spec.ts`
- Modify: `audita-web/tests/change-requests/activity-summary.spec.ts`
- Modify: `audita-web/tests/server/api-proxy.spec.ts` (if request payload/response fields change proxy expectations)

- [ ] Add failing frontend tests for dual statuses, conditional tabs, and linking UX.
- [ ] Implement missing UI or mapping fixes from failing tests.
- [ ] Run `pnpm test`, `nuxi typecheck`, and `pnpm build`.
- [ ] Verify compatibility-mode URLs remain unchanged.
- [ ] Capture screenshot/text evidence of UAT/deployment tab behavior.

---

## Subagent Dispatch Template (Copy/Paste)

Use this prompt format per task:

```text
Implement Task <RW15-XXX> from memory-bank/docs/plans/2026-06-04-sprint-15-requests-workflow-executable-plan.md.
Follow TDD strictly: write failing tests first, run and capture failure, implement minimal code, rerun tests.
Only touch listed files unless an unavoidable dependency requires one extra file (explain why).
Return:
1) files changed
2) tests run + outputs (pass/fail)
3) migration/API compatibility risks
4) follow-up needed for next task.
Do not commit.
```

---

## Phase Completion Checkpoints

- **Checkpoint A:** schema + entities compile; migration replay validated.
- **Checkpoint B:** state-machine + auth guards fully enforced in services.
- **Checkpoint C:** APIs stable and backward compatible.
- **Checkpoint D:** UI complete with no route breakage.
- **Checkpoint E:** full regression passes and event parity verified.
