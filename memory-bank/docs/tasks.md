# Audita - Developer Task List

**Project:** Audita - Multi-Tenant ITIL/ITSM Change Management Platform
**Version:** 0.6.3
**Last Updated:** 2026-05-24
**Team Size:** 2-3 Developers

## Task Status Legend

- 🔴 **Not Started** - Task has not been started
- 🟡 **In Progress** - Task is currently being worked on
- ✅ **Completed** - Task is finished and tested

## Sprint 0: Foundation & Scaffolding (Week 1-2)

### Platform Bootstrap

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| INIT-001 | Initialize Gradle multi-module backend scaffold | High | ✅ Completed | Developer 1 | Java 25 + Spring Boot 4 baseline established |
| INIT-002 | Configure PostgreSQL, Flyway, and tenant-aware JPA wiring | High | ✅ Completed | Developer 1 | Schema-per-tenant baseline with migration flow |
| INIT-003 | Add structured logging and health endpoint baseline | Medium | ✅ Completed | Developer 1 | MDC-aware logs and actuator health enabled |
| INIT-004 | Initialize Nuxt frontend with pnpm + TypeScript + Tailwind | High | ✅ Completed | Developer 2 | SSR frontend baseline complete |
| INIT-005 | Build shared UI primitives and auth/tenant middleware | High | ✅ Completed | Developer 2 | Core reusable components + route guards added |
| INIT-006 | Create docker-compose local platform stack | High | ✅ Completed | Developer 1 | API + Web + DB local reproducible environment |

## Sprint 1: Authentication & Platform Bootstrap (Week 3-4)

### Auth Foundation

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| AUTH-001 | Implement bootstrap super-admin flow | High | ✅ Completed | Developer 1 | Platform first-run bootstrap endpoint and UI |
| AUTH-002 | Implement login, refresh, and logout flows | High | ✅ Completed | Developer 1 | JWT + refresh-cookie lifecycle shipped |
| AUTH-003 | Implement forgot/reset password with rate limiting | High | ✅ Completed | Developer 1 | Recovery flow hardened with token safeguards |
| AUTH-004 | Implement Google and Microsoft OIDC SSO | High | ✅ Completed | Developer 1 | SSO init/callback + JIT provisioning |
| AUTH-005 | Build auth pages and auth store integration | High | ✅ Completed | Developer 2 | Sign-in/forgot/reset/accept-invite flows complete |
| AUTH-006 | Add auth regression coverage across critical paths | High | ✅ Completed | Developer 1 | Controller/service tests for auth core |

## Sprint 2: Multi-Tenancy, Users & Groups (Week 5-6)

### Tenant Core

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| TEN-001 | Implement tenant provisioning and lifecycle APIs | High | ✅ Completed | Developer 1 | Tenant create/list/update/deactivate flows |
| TEN-002 | Implement user invite and lifecycle management | High | ✅ Completed | Developer 1 | Invite/list/update/deactivate/reactivate |
| TEN-003 | Implement group CRUD and group membership management | High | ✅ Completed | Developer 1 | Group/member domain and APIs complete |
| TEN-004 | Implement role listing and tenant role integration | Medium | ✅ Completed | Developer 1 | Role exposure integrated into admin flows |
| TEN-005 | Build platform/admin pages for tenants, users, groups | High | ✅ Completed | Developer 2 | Operational admin surfaces delivered |

## Sprint 3: Change Request Core (Week 7-8)

### Change Request Domain

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| CR-001 | Implement change-request create/read/update list endpoints | High | ✅ Completed | Developer 1 | Core CR API and persistence model finalized |
| CR-002 | Implement approver assignment and approval workflow state machine | High | ✅ Completed | Developer 1 | Required/optional semantics integrated |
| CR-003 | Implement custom field persistence for change requests | Medium | ✅ Completed | Developer 1 | Custom field values stored and retrieved |
| CR-004 | Implement CR details UI with tabbed workflow surfaces | High | ✅ Completed | Developer 2 | Details/approvers/activity tabs delivered |
| CR-005 | Integrate description rich-text authoring baseline | Medium | ✅ Completed | Developer 2 | TipTap-based authoring introduced |

## Sprint 4: Collaboration, Notifications & SLA (Week 9-10)

### Collaboration Slice

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| COL-001 | Implement comments API and mention extraction | High | ✅ Completed | Developer 1 | Mention persistence and notifications |
| COL-002 | Implement notifications list/read/read-all APIs | High | ✅ Completed | Developer 1 | Notification lifecycle endpoints complete |
| COL-003 | Implement SSE notification stream | High | ✅ Completed | Developer 1 | Real-time notification delivery available |
| COL-004 | Implement SLA warning/breach scheduler and events | High | ✅ Completed | Developer 1 | SLA automation and event logging |
| COL-005 | Integrate comments and notification bell in frontend | Medium | ✅ Completed | Developer 2 | Collaboration UX fully wired |

## Sprint 5: Hardening, Release Readiness & E2E (Week 11)

### Reliability Gate

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| HARD-001 | Stabilize critical CR lifecycle integration paths | High | ✅ Completed | Developer 1 | E2E critical-path regressions fixed |
| HARD-002 | Add identity and DTO mapping regression coverage | High | ✅ Completed | Developer 1 | Principal and lazy-loading failures locked down |
| HARD-003 | Complete release-gate verification across API and web | High | ✅ Completed | Developer 1 | Backend tests and frontend build gates passed |
| HARD-004 | Refactor auth/bootstrap controllers to port-driven boundaries | Medium | ✅ Completed | Developer 1 | Hexagonal layering strengthened |

## Sprint 6: Audit Trail & Admin Configuration

### Audit/Admin

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| AUD-001 | Implement audit trail coverage for key CR workflow events | High | ✅ Completed | Developer 1 | Immutable event history expanded |
| AUD-002 | Enable admin configuration surfaces for tenant operations | Medium | ✅ Completed | Developer 2 | Admin-facing settings/config UX improved |
| AUD-003 | Remove residual mock-data pathways and enforce live data flows | Medium | ✅ Completed | Developer 1 | Data fidelity tightened |

## Sprint 7: File Security, Custom Fields UX & CR Edit

### Security + UX

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SEC-010 | Implement 3-layer file upload validation | High | ✅ Completed | Developer 1 | Signature + extension + MIME validation |
| SEC-011 | Add path traversal guard in attachment reads | High | ✅ Completed | Developer 1 | Normalization + prefix enforcement |
| UX-701 | Implement filename normalization strategy | Medium | ✅ Completed | Developer 1 | Safe storage naming with retained display names |
| UX-702 | Build admin custom-fields management page | High | ✅ Completed | Developer 2 | Full CRUD for custom-field definitions |
| UX-703 | Redesign CR detail read-only/edit mode | High | ✅ Completed | Developer 2 | Explicit edit mode and safer save flow |

## Sprint 8: Admin Settings Activation & SLA Defaults

### Tenant Settings

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SET-001 | Add persisted workflow/SLA defaults to tenant settings API | High | ✅ Completed | Developer 1 | Settings now tenant-persisted |
| SET-002 | Activate admin settings save flow for workflow/SLA | High | ✅ Completed | Developer 2 | UI save path with validation and dirty-state |
| SET-003 | Apply tenant SLA defaults at runtime | High | ✅ Completed | Developer 1 | CR deadline/warning logic now tenant-aware |
| SET-004 | Add GET/PATCH regression coverage for tenant settings | High | ✅ Completed | Developer 1 | Controller/service tests complete |

## Sprint 9: CR List Scalability

### Pagination

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| CRL-001 | Implement server-side pagination with explicit navigation controls | High | ✅ Completed | Developer 2 | `size=50` and predictable next/prev UX |

## Sprint 10: UX & WCAG 2.2 Compliance Overhaul

### UI/Accessibility

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| UX10-001 | Implement mobile navigation drawer and responsive shell improvements | High | ✅ Completed | Developer 2 | Mobile nav and responsive layout fixes |
| UX10-002 | Consolidate button/component usage and pagination UX consistency | High | ✅ Completed | Developer 2 | Shared component standardization completed |
| UX10-003 | Improve CR list/detail usability and feedback states | High | ✅ Completed | Developer 2 | Empty states, skeletons, sticky actions, confirmations |
| UX10-004 | Complete WCAG 2.2 AA compliance pass across pages/components | High | ✅ Completed | Developer 2 | Accessibility pass and regressions closed |

## Sprint 11: Session Hardening, RBAC Expansion & Workflow Polish

### Auth/RBAC/Workflow

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SESS-001 | Implement cold-start session restore and contract validation | High | ✅ Completed | Developer 1 | `/api/v1/auth/session` + `X-Audita-Api-Contract` |
| SESS-002 | Tighten refresh/logout semantics and cross-tab sync | High | ✅ Completed | Developer 2 | 401-only refresh and token-free sync |
| RBAC-001 | Implement multi-role user assignment and role hierarchy behavior | High | ✅ Completed | Developer 1 | Role precedence and compatibility role retained |
| RBAC-002 | Add custom role creation/permission update endpoints with overlap checks | High | ✅ Completed | Developer 1 | System roles immutable; overlap prevented |
| WF-001 | Auto-populate approvers and improve CR workflow collaboration UX | High | ✅ Completed | Developer 2 | Auto-assignment, richer CR detail interaction |

## Sprint 12: Launch Readiness

### Release Gate

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| LAUNCH-001 | Run Sonar scan and dependency audit | High | ✅ Completed | Developer 1 | Completed with no critical/blocker issues |
| LAUNCH-002 | Add smoke test for critical end-to-end flow | Medium | ✅ Completed | Developer 2 | Login -> create CR -> submit -> approve |
| LAUNCH-003 | Cut v0.6.0 release tag and publish changelog | High | ✅ Completed | Developer 1 | Release published |
| HIST-012 | Preserve remaining Sprint 12 baseline tasks | Medium | ✅ Completed | Developer 2 | 6/6 complete total |

## Sprint 13: Engineering Best Practices Hardening

### Security/Observability

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| BP13-001 | Pin GitHub Actions to immutable SHAs and least-privilege permissions | High | ✅ Completed | Developer 1 | `.github/workflows/ci-release.yml` hardened |
| BP13-002 | Add CI security gates for dependency, image, and SAST scans | High | ✅ Completed | Developer 1 | Blocking security jobs enabled |
| BP13-003 | Generate and publish SBOM artifacts | Medium | ✅ Completed | Developer 1 | SPDX artifacts generated/uploaded |
| BP13-004 | Add backend OpenTelemetry and Prometheus metrics | High | ✅ Completed | Developer 1 | OTel and metrics export enabled |
| BP13-005 | Add readiness/liveness probes and tighten actuator access | Medium | ✅ Completed | Developer 1 | Health probe exposure tightened |
| BP13-006 | Implement API idempotency-key support for retriable mutations | High | ✅ Completed | Developer 1 | `X-Idempotency-Key` persisted/replayed |
| BP13-007 | Harden Nuxt API proxy forwarding and validation | Medium | ✅ Completed | Developer 2 | Header allowlist and validation added |
| BP13-008 | Enforce frontend CSP and security headers via `nuxt-security` | Medium | ✅ Completed | Developer 2 | Runtime security headers enforced |

## Post-Sprint 1: Reliability, UX & Rich-Text Hardening

### Stability Improvements

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS1-001 | Preserve completed Post-Sprint 1 tasks | High | ✅ Completed | Developer 1 | 19/19 complete |

## Post-Sprint 2: Approver UX Polish + Activity Stream + CI Trivy Fix

### Approver Polish

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS2-001 | Preserve completed Post-Sprint 2 tasks | High | ✅ Completed | Developer 2 | 8/8 complete |

## Post-Sprint 3: Mention UX + Comment Deep-Link + Validator Scope Fix

### Mention Flow

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS3-001 | Preserve completed Post-Sprint 3 tasks | High | ✅ Completed | Developer 2 | 9/9 complete |

## Post-Sprint 4: DHI Hardened Runtime + Docker Build Reliability

### Container Hardening

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS4-001 | Preserve completed Post-Sprint 4 tasks | High | ✅ Completed | Developer 1 | 6/6 complete |

## Post-Sprint 5: Approver Flexibility + Activity Summary Coverage

### Workflow Flexibility

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS5-001 | Preserve completed Post-Sprint 5 tasks | High | ✅ Completed | Developer 1 | 7/7 complete |

## Post-Sprint 6: Web Docker Policy Parity + pnpm Config Cleanup

### Container Build Reliability

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| PS6-001 | Copy `pnpm-workspace.yaml` before `pnpm install` in web Docker build | High | ✅ Completed | Developer 2 | Fixes container lockfile policy parity and `ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION` |
| PS6-002 | Keep hardened runtime model with `dhi.io/node:24` runtime and numeric non-root user | High | ✅ Completed | Developer 1 | Runtime stage remains hardened with `USER 65532:65532` |
| PS6-003 | Migrate deprecated pnpm settings out of `package.json` into workspace config | Medium | ✅ Completed | Developer 2 | `supportedArchitectures` moved to `pnpm-workspace.yaml`; deprecated warning removed |

## Progress Tracking

### Overall Progress by Sprint

| Sprint         | Total Tasks | Not Started | In Progress | Completed | Progress % |
| -------------- | ----------- | ----------- | ----------- | --------- | ---------- |
| Sprint 0       | 19          | 0           | 0           | 19        | 100%       |
| Sprint 1       | 22          | 0           | 0           | 22        | 100%       |
| Sprint 2       | 19          | 0           | 0           | 19        | 100%       |
| Sprint 3       | 21          | 0           | 0           | 21        | 100%       |
| Sprint 4       | 10          | 0           | 0           | 10        | 100%       |
| Sprint 5       | 8           | 0           | 0           | 8         | 100%       |
| Sprint 6       | 7           | 0           | 0           | 7         | 100%       |
| Sprint 7       | 8           | 0           | 0           | 8         | 100%       |
| Sprint 8       | 4           | 0           | 0           | 4         | 100%       |
| Sprint 9       | 1           | 0           | 0           | 1         | 100%       |
| Sprint 10      | 36          | 0           | 0           | 36        | 100%       |
| Sprint 11      | 26          | 0           | 0           | 26        | 100%       |
| Sprint 12      | 6           | 0           | 0           | 6         | 100%       |
| Sprint 13      | 8           | 0           | 0           | 8         | 100%       |
| Post-Sprint 1  | 19          | 0           | 0           | 19        | 100%       |
| Post-Sprint 2  | 8           | 0           | 0           | 8         | 100%       |
| Post-Sprint 3  | 9           | 0           | 0           | 9         | 100%       |
| Post-Sprint 4  | 6           | 0           | 0           | 6         | 100%       |
| Post-Sprint 5  | 7           | 0           | 0           | 7         | 100%       |
| Post-Sprint 6  | 3           | 0           | 0           | 3         | 100%       |
| **TOTAL**      | **247**     | **0**       | **0**       | **247**   | **100%**   |

### Progress by Developer

| Developer   | Assigned Tasks | Not Started | In Progress | Completed | Progress % |
| ----------- | -------------- | ----------- | ----------- | --------- | ---------- |
| Developer 1 | 129            | 0           | 0           | 129       | 100%       |
| Developer 2 | 118            | 0           | 0           | 118       | 100%       |

## Recent Implementations

### Post-Sprint 6 Web Docker Policy Parity (Completed 2026-05-24)

**Overview**: Fixed web container build failures in policy-gated install step and removed deprecated pnpm config drift.

**Files Created/Modified**:

- `audita-web/Dockerfile` - include `pnpm-workspace.yaml` in early install layer and keep runtime hardening semantics.
- `audita-web/package.json` - removed deprecated `pnpm` config block.
- `audita-web/pnpm-workspace.yaml` - canonical source for `supportedArchitectures` and minimum release-age exclusions.

**Key Changes**:

- Containerized install now sees workspace policy config before lockfile verification.
- Eliminated `ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION` caused by missing workspace policy file in Docker context layer.
- Removed deprecated pnpm warning by migrating settings to supported workspace config.

**Test Coverage**: `docker build -t audita-web:scan -f audita-web/Dockerfile audita-web` passing with lockfile policy check and full `nuxt build` completion.

### Post-Sprint 5 Approver Mutation Flexibility (Completed 2026-05-23)

**Overview**: Expanded open-CR approver mutation behavior with vote-safe constraints and richer activity summaries.

**Files Created/Modified**:

- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java` - pending-approval approver mutation support with vote-safe remove guard.
- `audita-web/composables/activitySummary.ts` - extracted readable activity summary logic.
- `audita-web/tests/change-requests/activity-summary.spec.ts` - regression coverage for new summaries.

**Key Changes**:

- Add/remove/reorder/requirement updates allowed in `PENDING_APPROVAL`.
- Removal blocked when approver already voted (`APPROVER_DECISION_LOCKED`).
- Human-readable summary coverage for approver mutation activity events.

**Test Coverage**: `cd audita-api && ./gradlew :infrastructure:test --tests "io.audita.infrastructure.service.ChangeRequestServiceSecurityTest" --no-daemon`; `cd audita-web && pnpm -s nuxi typecheck`; `cd audita-web && pnpm test`.
