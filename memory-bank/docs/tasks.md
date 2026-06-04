<!-- markdownlint-disable MD060 -->

# Audita - Developer Task List

**Project:** Audita - Multi-Tenant ITIL/ITSM Change Management Platform
**Version:** 0.6.3
**Last Updated:** 2026-06-04
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
| AUTH-004 | Implement Google and Microsoft OIDC SSO | High | 🔴 Not Started | Developer 1 | SSO removed from app; provider research + re-planning required |
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
| UX10-001 | Implement mobile navigation drawer (hamburger menu) | High | ✅ Completed | Developer 2 | Responsive drawer with keyboard and focus support |
| UX10-002 | Fix page header layout collapse on small screens | High | ✅ Completed | Developer 2 | Header actions wrap cleanly on mobile |
| UX10-003 | Collapse sidebar to icon rail on medium breakpoints | Medium | ✅ Completed | Developer 2 | Sidebar rail mode with persisted state |
| UX10-004 | Collapse CR filters into mobile pill panel | Medium | ✅ Completed | Developer 2 | Mobile filter dropdown with active-count badge |
| UX10-005 | Stack CR detail action buttons on mobile | Medium | ✅ Completed | Developer 2 | Prevented horizontal overflow in detail header |
| UX10-006 | Align `AppButton.vue` with CSS token system | High | ✅ Completed | Developer 2 | Variant and size parity across usage surfaces |
| UX10-007 | Replace auth raw button classes with shared AppButton | Low | ✅ Completed | Developer 2 | Auth screens now use standardized button component |
| UX10-008 | Wire CR list pagination to `AppPagination` | Medium | ✅ Completed | Developer 2 | Replaced ad-hoc next/prev control block |
| UX10-009 | Add SLA indicator column in CR list | High | ✅ Completed | Developer 2 | Deadline visibility added to list view |
| UX10-010 | Fix CR filter dropdown display labels | Low | ✅ Completed | Developer 2 | Human-readable filter text normalized |
| UX10-011 | Add clear-filters reset action | Medium | ✅ Completed | Developer 2 | One-click filter reset with data reload |
| UX10-012 | Replace plain empty-state text with illustrated state | Low | ✅ Completed | Developer 2 | Better guidance for empty and filtered states |
| UX10-013 | Add skeleton loading rows for CR list | Low | ✅ Completed | Developer 2 | Reduced perceived load jank |
| UX10-014 | Replace ad-hoc tab buttons with accessible tablist | High | ✅ Completed | Developer 2 | ARIA-compliant tab interactions on CR detail |
| UX10-015 | Show badge counts on CR detail tabs | Low | ✅ Completed | Developer 2 | Approver/activity/comment counts exposed |
| UX10-016 | Replace affected-systems comma input with tag UI | Medium | ✅ Completed | Developer 2 | Tokenized input with add/remove controls |
| UX10-017 | Add sticky save/cancel bar in CR edit mode | Medium | ✅ Completed | Developer 2 | Always-visible edit actions while scrolling |
| UX10-018 | Add destructive-action confirmation for reject/decline | High | ✅ Completed | Developer 2 | Confirm modal with reason capture |
| UX10-019 | Add show/hide toggles for password fields | High | ✅ Completed | Developer 2 | Sign-in/reset/accept-invite parity |
| UX10-020 | Normalize page title (`h1`) sizing system | Medium | ✅ Completed | Developer 2 | Consistent typography hierarchy |
| UX10-021 | Move sign-in error banner above form inputs | Low | ✅ Completed | Developer 2 | Error visibility and flow improved |
| UX10-022 | Normalize approval-type select option labels | Low | ✅ Completed | Developer 2 | User-friendly wording in selects |
| UX10-023 | Add toast auto-dismiss progress bar | Medium | ✅ Completed | Developer 2 | Visual timeout feedback on notifications |
| UX10-024 | Surface accessible dark-mode toggle control | Medium | ✅ Completed | Developer 2 | Keyboard and ARIA-ready control in header |
| UX10-025 | Remove non-functional global search UI | High | ✅ Completed | Developer 2 | Dead control removed to avoid confusion |
| UX10-026 | Add ARIA labels to icon-only controls | Medium | ✅ Completed | Developer 2 | Screen-reader naming coverage improved |
| WCAG-001 | Add skip-to-content link and visible focus target | High | ✅ Completed | Developer 2 | Keyboard-first navigation support |
| WCAG-002 | Ensure unique, descriptive page titles across routes | High | ✅ Completed | Developer 2 | Title consistency for assistive tech |
| WCAG-003 | Add explicit label/input associations on forms | High | ✅ Completed | Developer 2 | Eliminated orphaned input controls |
| WCAG-004 | Add autocomplete attributes on auth/profile inputs | Medium | ✅ Completed | Developer 2 | Browser assist and accessibility support |
| WCAG-005 | Add focus trap behavior in modal/dialog surfaces | High | ✅ Completed | Developer 2 | Prevented focus escape in dialogs |
| WCAG-006 | Add `aria-live` regions for async status feedback | Medium | ✅ Completed | Developer 2 | Toast/form/system updates announced |
| WCAG-007 | Improve keyboard operability for interactive controls | High | ✅ Completed | Developer 2 | Tab/enter/escape flows validated |
| WCAG-008 | Improve color contrast on key UI states | High | ✅ Completed | Developer 2 | Text/action contrast adjusted to AA |
| WCAG-009 | Add scroll-margin handling for anchor/deep-link focus | Medium | ✅ Completed | Developer 2 | Anchored content not hidden under chrome |
| WCAG-010 | Ensure all interactive targets meet 24x24 minimum | Medium | ✅ Completed | Developer 2 | Target-size compliance for WCAG 2.5.8 |

## Sprint 11: Session Hardening, RBAC Expansion & Workflow Polish

### Auth/RBAC/Workflow

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SESS-001 | Add configurable refresh-cookie secure flag for local dev parity | High | ✅ Completed | Developer 1 | Prevented localhost session-loss in HTTP dev |
| SESS-002 | Add refresh-cookie path fix for reliable logout revocation | High | ✅ Completed | Developer 1 | Refresh cookie now available to logout endpoint |
| SESS-003 | Implement cold-start session restore endpoint (`/auth/session`) | High | ✅ Completed | Developer 1 | Non-rotating restore flow using refresh cookie |
| SESS-004 | Enforce 401-only token refresh retry on frontend API client | High | ✅ Completed | Developer 2 | Removed over-broad refresh behavior |
| SESS-005 | Add API contract compatibility header and enforcement | High | ✅ Completed | Developer 1 | `X-Audita-Api-Contract` server/client validation |
| SESS-006 | Add token-free cross-tab auth session synchronization | Medium | ✅ Completed | Developer 2 | BroadcastChannel/localStorage event sync |
| SESS-007 | Fail closed on tenant mismatch during restore/logout flows | High | ✅ Completed | Developer 2 | Tenant guard in middleware/session helpers |
| SESS-008 | Remove JS-readable access-token persistence pathway | High | ✅ Completed | Developer 2 | Session recovered via HttpOnly cookie path |
| SESS-009 | Replace reflection-based security config authorization wiring | Medium | ✅ Completed | Developer 1 | Public Spring Security APIs now used |
| SESS-010 | Add focused security authorization regression tests | High | ✅ Completed | Developer 1 | Route authorization behavior locked |
| SESS-011 | Add focused frontend session regression tests | High | ✅ Completed | Developer 2 | Session/tenant/contract/sync tests added |
| RBAC-001 | Auto-add approver/auditor/admin users on CR create | High | ✅ Completed | Developer 1 | Default role-based population implemented |
| RBAC-002 | Keep submit-time approver population idempotent | High | ✅ Completed | Developer 1 | Duplicate-avoidance logic enforced |
| RBAC-003 | Add `user_roles` many-to-many schema migration with backfill | High | ✅ Completed | Developer 1 | Legacy role mapping preserved |
| RBAC-004 | Support multi-role assignment in invite/update APIs | High | ✅ Completed | Developer 1 | `roleIds` contract support introduced |
| RBAC-005 | Implement effective-role precedence model | Medium | ✅ Completed | Developer 1 | Compatibility role derived by hierarchy |
| RBAC-006 | Extend JWT claims with all roles and permissions | High | ✅ Completed | Developer 1 | Security authorities expanded |
| RBAC-007 | Add custom role creation endpoint for admins | High | ✅ Completed | Developer 1 | Admin-managed custom roles supported |
| RBAC-008 | Add custom role permission update endpoint | High | ✅ Completed | Developer 1 | Mutable permission set for custom roles |
| RBAC-009 | Add overlap/duplicate permission prevention checks | High | ✅ Completed | Developer 1 | Role-permission integrity guardrails |
| UXR-001 | Restore localhost session persistence on browser refresh | High | ✅ Completed | Developer 1 | Solved dev auth cold-start regressions |
| UXR-002 | Allow assigned non-auditor approvers to approve/reject | High | ✅ Completed | Developer 1 | Controller gate aligned with service rules |
| UXR-003 | Fix CR comments 500 and lazy relation mapping issues | High | ✅ Completed | Developer 1 | DTO mapping stability restored |
| UXR-004 | Center reject modal and restore full overlay behavior | Medium | ✅ Completed | Developer 2 | Modal interaction polish and consistency |
| UXR-005 | Add rich-text controls and pre-create attachment queue | High | ✅ Completed | Developer 2 | Better authoring + attachment UX on create |
| UXR-006 | Show recorded votes and readable activity stream details | High | ✅ Completed | Developer 2 | Decision visibility and context improved |

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

## Sprint 14: SSO Research & Provider Selection (Planning)

### SSO Scope & Options

| Task ID | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SSO-001 | Inventory existing SSO/OIDC codepaths and endpoints | High | 🟡 In Progress | Developer 1 | Confirm removal scope + remaining references |
| SSO-002 | Research Google OIDC + required scopes/claims for JIT | High | 🔴 Not Started | Developer 1 | Validate least-privilege approach |
| SSO-003 | Research Microsoft Entra ID OIDC (single-tenant vs multi-tenant) | High | 🔴 Not Started | Developer 2 | Capture tenant/issuer URL patterns |
| SSO-004 | Research generic OpenID Connect support (generic IdP) | High | 🔴 Not Started | Developer 1 | Assess JWKS validation + claim mapping strategy |
| SSO-005 | Research Okta OIDC integration effort and config model | Medium | 🔴 Not Started | Developer 2 | Identify required admin/app settings |
| SSO-006 | Evaluate SSO option ordering to avoid app bloat | High | 🔴 Not Started | Developer 1 | Choose smallest useful provider set for v1 |

### Auth Flow & Domain Mapping (Assumptions)

| Task ID | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| SSO-007 | Decide account model: SSO users must exist (no invite-first) | High | 🔴 Not Started | Developer 1 | Align with B requirement |
| SSO-008 | Define tenant resolution from IdP claims/org identifiers | High | 🔴 Not Started | Developer 2 | Use IdP-provided fields for tenant selection (A) |
| SSO-009 | Draft provider-agnostic JIT/JWT linking rules by email | High | 🔴 Not Started | Developer 1 | Email match as primary key |
| SSO-010 | Define security controls: state/CSRF, JWKS validation, secret handling | High | 🔴 Not Started | Developer 2 | Re-apply encryption + remove token leakage risks |
| SSO-011 | Produce minimal implementation plan per chosen providers | High | 🔴 Not Started | Developer 1 | Include config UIs and backend endpoints list |

## Post-Sprint 1: Reliability, UX & Rich-Text Hardening

### Stability Improvements

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS1-001 | Fix settings PATCH UUID parsing for `autoApproverDefaults` | High | ✅ Completed | Developer 2 | Switched to string parse + explicit validation |
| PS1-002 | Allow `content-length` in Nuxt API proxy header forwarding | High | ✅ Completed | Developer 2 | Prevented body handling regressions on settings save |
| PS1-003 | Add tolerant map parsing for tenant settings PATCH payloads | High | ✅ Completed | Developer 2 | Reduced strict deserialization failure risk |
| PS1-004 | Add idempotent migration to repair missing `refresh_tokens` table | High | ✅ Completed | Developer 1 | Drift-safe schema repair migration added |
| PS1-005 | Require tenant header for auth session/refresh/logout recovery paths | High | ✅ Completed | Developer 1 | Fail-closed session endpoint behavior |
| PS1-006 | Propagate tenant header consistently in frontend auth/session flows | High | ✅ Completed | Developer 2 | Session/logout edge-path stability improved |
| PS1-007 | Remove explicit Hibernate dialect config to stop warning noise | Medium | ✅ Completed | Developer 1 | Eliminated `HHH90000025` noise |
| PS1-008 | Enable Caffeine `recordStats` to stop metrics binding warnings | Medium | ✅ Completed | Developer 1 | Fixed `CaffeineCacheMetrics` warning |
| PS1-009 | Remove pageable collection `@EntityGraph` causing fetch warning | Medium | ✅ Completed | Developer 1 | Eliminated pagination+fetch warning pattern |
| PS1-010 | Add targeted logger-level tuning for noisy infra categories | Low | ✅ Completed | Developer 1 | Reduced non-actionable runtime logs |
| PS1-011 | Improve SSE lifecycle teardown handling in web client | Medium | ✅ Completed | Developer 2 | Intentional disconnect handling added |
| PS1-012 | Extract shared rich-text extension config composable | High | ✅ Completed | Developer 2 | Unified TipTap extension setup |
| PS1-013 | Expand rich-text toolbar capability set | High | ✅ Completed | Developer 2 | Headings/lists/code/link controls added |
| PS1-014 | Add `.rich-content` CSS rules for render fidelity | High | ✅ Completed | Developer 2 | Read-mode spacing/typography normalization |
| PS1-015 | Normalize outbound anchor attributes in backend sanitizer | High | ✅ Completed | Developer 1 | Enforced secure rel/target attributes |
| PS1-016 | Apply sanitizer in change-request create/update flows | High | ✅ Completed | Developer 1 | Server-side rich-text safety hardened |
| PS1-017 | Normalize rendered rich-text links in frontend | High | ✅ Completed | Developer 2 | Client-side rendering consistency guard |
| PS1-018 | Add backend rich-text sanitizer regression tests | Medium | ✅ Completed | Developer 1 | `HtmlSanitizerTest` coverage expanded |
| PS1-019 | Add frontend rich-text rendering regression tests | Medium | ✅ Completed | Developer 2 | `rich-text.spec.ts` coverage added |

## Post-Sprint 2: Approver UX Polish + Activity Stream + CI Trivy Fix

### Approver Polish

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS2-001 | Default newly selected approvers to optional requirement | High | ✅ Completed | Developer 2 | Required toggled only when explicitly selected |
| PS2-002 | Add per-approver required/optional toggle on saved list | High | ✅ Completed | Developer 2 | Immediate requirement mutation support |
| PS2-003 | Implement backend requirement-toggle endpoint | High | ✅ Completed | Developer 1 | `PATCH /approvers/{id}/requirement` |
| PS2-004 | Exclude CR creator from approver candidate list | High | ✅ Completed | Developer 2 | Self-approval prevention in UX |
| PS2-005 | Add dirty-tracking and change-applied confirmation banner | High | ✅ Completed | Developer 2 | Snapshot-based change detection |
| PS2-006 | Add reorder transition animations for approver list | Medium | ✅ Completed | Developer 2 | FLIP-style move/enter/exit polish |
| PS2-007 | Improve activity summary for approver reorder events | Medium | ✅ Completed | Developer 2 | Replaced raw count field with readable summary |
| PS2-008 | Unblock CI Trivy job via scoped CVE ignore rationale | High | ✅ Completed | Developer 1 | Added `.trivyignore` for non-exploitable base-image CVE |

## Post-Sprint 3: Mention UX + Comment Deep-Link + Validator Scope Fix

### Mention Flow

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS3-001 | Add backend mention user-search endpoint for comment editor | High | ✅ Completed | Developer 1 | Server query path for mention suggestions |
| PS3-002 | Integrate TipTap mention autocomplete popup UX | High | ✅ Completed | Developer 2 | Keyboard-friendly mention selection |
| PS3-003 | Add mention deep-link (`commentId`) into outbound emails | Medium | ✅ Completed | Developer 1 | Email now points to exact comment |
| PS3-004 | Auto-scroll and highlight deep-linked comment on CR detail | Medium | ✅ Completed | Developer 2 | In-page anchor and highlight behavior |
| PS3-005 | Preserve redirect target through login/auth middleware | High | ✅ Completed | Developer 2 | Logged-out deep-links resume after sign-in |
| PS3-006 | Disable `xssValidator` for `/api/**` proxy scope | High | ✅ Completed | Developer 2 | Prevented false-positive 400 on mention payload |
| PS3-007 | Extend backend sanitizer allowlist for mention span attributes | High | ✅ Completed | Developer 1 | Safe mention markup persistence |
| PS3-008 | Add backend mention/sanitizer regression coverage | Medium | ✅ Completed | Developer 1 | Comment service tests updated |
| PS3-009 | Add frontend build and typecheck verification for mention flow | Medium | ✅ Completed | Developer 2 | `nuxi build` and typecheck gates pass |

## Post-Sprint 4: DHI Hardened Runtime + Docker Build Reliability

### Container Hardening

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS4-001 | Enforce numeric non-root runtime ownership in API image | High | ✅ Completed | Developer 1 | `COPY --chown=65532:65532` runtime model |
| PS4-002 | Remove shell/tool-dependent runtime healthcheck assumptions | Medium | ✅ Completed | Developer 1 | Hardened image compatibility retained |
| PS4-003 | Harden Gradle wrapper network timeout configuration | Medium | ✅ Completed | Developer 1 | Reduced transient Docker build failures |
| PS4-004 | Validate local hardened compose stack startup | High | ✅ Completed | Developer 1 | API and web services boot successfully |
| PS4-005 | Verify API health endpoint returns 200 in hardened stack | High | ✅ Completed | Developer 1 | Actuator readiness confirmed |
| PS4-006 | Run focused backend regression tests for latest workflow semantics | Medium | ✅ Completed | Developer 1 | Security/service tests green |

## Post-Sprint 5: Approver Flexibility + Activity Summary Coverage

### Workflow Flexibility

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| PS5-001 | Always auto-apply configured default approvers on CR create/submit | High | ✅ Completed | Developer 1 | No longer gated by workflow checkbox |
| PS5-002 | Allow approver add/remove/reorder during `PENDING_APPROVAL` | High | ✅ Completed | Developer 1 | Open-request mutation flexibility expanded |
| PS5-003 | Block removal of approvers who already voted | High | ✅ Completed | Developer 1 | `APPROVER_DECISION_LOCKED` safeguard |
| PS5-004 | Emit audit events for approver mutation actions | High | ✅ Completed | Developer 1 | Add/group-add/remove/reorder/requirement-change parity |
| PS5-005 | Clarify admin settings UX for default approver semantics | Medium | ✅ Completed | Developer 2 | Explicit wording in settings UI |
| PS5-006 | Extract reusable frontend activity summary helper | Medium | ✅ Completed | Developer 2 | Centralized summary formatting logic |
| PS5-007 | Add dedicated tests for activity summary variants | Medium | ✅ Completed | Developer 2 | `activity-summary.spec.ts` coverage added |

## Post-Sprint 6: Web Docker Policy Parity + pnpm Config Cleanup

### Container Build Reliability

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| PS6-001 | Copy `pnpm-workspace.yaml` before `pnpm install` in web Docker build | High | ✅ Completed | Developer 2 | Fixes container lockfile policy parity and `ERR_PNPM_MINIMUM_RELEASE_AGE_VIOLATION` |
| PS6-002 | Keep hardened runtime model with `dhi.io/node:24` runtime and numeric non-root user | High | ✅ Completed | Developer 1 | Runtime stage remains hardened with `USER 65532:65532` |
| PS6-003 | Migrate deprecated pnpm settings out of `package.json` into workspace config | Medium | ✅ Completed | Developer 2 | `supportedArchitectures` moved to `pnpm-workspace.yaml`; deprecated warning removed |

## Sprint 14: Security Audit Remediation (Week 23)

### Phase A — Critical Security

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| SA14-001 | Add setup-token guard on `/api/platform/v1/setup` endpoint | High | 🔴 Not Started | Developer 1 | Require `X-Setup-Token` header; auto-generate one-time token at first boot |
| SA14-002 | Integrate DOMPurify for `v-html` rich-text sanitization | High | 🔴 Not Started | Developer 2 | Client-side DOMPurify + SSR jsdom fallback; strict allowlist matching TipTap output |
| SA14-003 | Validate redirect targets are same-origin paths | High | 🔴 Not Started | Developer 2 | `isSafeRedirect()` utility; reject `//` and external URLs in all auth flows |

### Phase B — High-Severity Auth & Session

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| SA14-004 | Add rate limiting to super admin login path | High | 🔴 Not Started | Developer 1 | `enforceRateLimit("sa-login:" + ip + ":" + email, 5, 15min)` |
| SA14-005 | Implement JWT token versioning for revocability | High | 🔴 Not Started | Developer 1 | `tokenVersion` claim + per-user DB column + Caffeine-cached validation in filter |
| SA14-006 | Enforce domain whitelist in SSO JIT-provisioning path | High | 🔴 Not Started | Developer 1 | Check `TenantAllowedDomain` in `resolveOrProvisionUser` before provisioning |
| SA14-007 | Enable CSRF protection for cookie-scoped auth endpoints | High | 🔴 Not Started | Developer 1 | Custom `CsrfTokenRequestHandler` exempting bearer-token routes |

### Phase C — High-Severity Infrastructure

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| SA14-008 | Make idempotency key check-then-act atomic | High | 🔴 Not Started | Developer 1 | `INSERT ON CONFLICT DO NOTHING` + `RETURNING`; single transaction with resource creation |
| SA14-009 | Fix `@Async` self-invocation in `AuditExportService` | High | 🔴 Not Started | Developer 1 | Extract `generateAsync` into separate `@Service` bean or use `@Lazy` self-injection |
| SA14-010 | Set `JPA_DDL_AUTO=validate` for production profiles | High | 🔴 Not Started | Developer 1 | Startup validation rejecting `update` when profile is not `dev` |
| SA14-011 | Harden Docker infrastructure (port binding, healthchecks, image pinning) | High | 🔴 Not Started | Developer 2 | `127.0.0.1` binding, actuator healthchecks, version-pinned images, fix DB healthcheck host |

### Phase D — Medium-Severity Security

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| SA14-012 | Configure security response headers and CSP hardening | Medium | 🔴 Not Started | Developer 2 | HSTS, X-Frame-Options, nosniff, CSP nonce-based `script-src`, restrict `connect-src` |
| SA14-013 | Harden tenant isolation (context leaks, status validation, password reset scoping) | Medium | 🔴 Not Started | Developer 1 | SSO callback finally block, tenant status check in JWT filter, require tenant slug on password reset |
| SA14-014 | Prevent CSV injection and stream audit export output | Medium | 🔴 Not Started | Developer 1 | Prefix `=+\-@\t\r` with `'`; `StreamingResponseBody` with paginated queries |
| SA14-015 | Fix exception handler information leakage and X-Forwarded-For hardening | Medium | 🔴 Not Started | Developer 1 | Generic error messages for `HttpMessageNotReadableException`; `ForwardedHeaderFilter` with trusted proxy |
| SA14-016 | Migrate in-memory SSO state and rate limiting to Redis | Medium | 🔴 Not Started | Developer 1 | Redis-backed `pendingStates`/`pendingExchanges` with TTL; Bucket4j or sliding window for rate limits |

### Phase E — Performance & Architecture

| Task ID   | Task | Priority | Status | Assigned To | Notes |
| --------- | ---- | -------- | ------ | ----------- | ----- |
| SA14-017 | Remediate N+1 queries with batch fetching and EntityGraph | Medium | 🔴 Not Started | Developer 1 | `@EntityGraph` on user/CR/comment/activity repositories; `@BatchSize(20)` on lazy collections |
| SA14-018 | Add Caffeine cache for tenant resolution | Medium | 🔴 Not Started | Developer 1 | 60s TTL cache for slug-to-subdomain mapping; eliminate per-request JDBC |
| SA14-019 | Implement SSE exponential backoff and fix event listener leaks | Medium | 🔴 Not Started | Developer 2 | Backoff: 1s→60s, factor 2, ±500ms jitter; `useEventListener` composable with auto-cleanup |
| SA14-020 | Enforce audit log immutability at database level | Medium | 🔴 Not Started | Developer 1 | `REVOKE UPDATE/DELETE`, trigger raising exception on mutation, `REQUIRES_NEW` propagation |
| SA14-021 | Decompose monolithic components (CR detail, settings page) | Medium | 🔴 Not Started | Developer 2 | Extract `CrApproverPanel`, `CrCommentThread`, `CrActivityStream`, `SettingsGeneral`, `SettingsWorkflow`, etc. |

## Sprint 15: Requests Workflow Expansion (Week 24)

Execution plan: `memory-bank/docs/plans/2026-06-04-sprint-15-requests-workflow-executable-plan.md`

### Phase A - Data Contracts

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| RW15-001 | Add tenant migration for request display ID and dual statuses | High | ✅ Completed | Developer 1 | Add `display_id`, `approval_status`, `completion_status`, `workflow_mode` columns with backfill strategy |
| RW15-002 | Add department master table and request department foreign keys | High | ✅ Completed | Developer 1 | Create `departments` table + `request_department_id` and `destination_department_id` on requests |
| RW15-003 | Add bidirectional request links schema with canonical pair uniqueness | High | ✅ Completed | Developer 1 | Add `request_links` with no-self-link constraint and ordered UUID uniqueness |
| RW15-004 | Add UAT and deployment tables with approvers and comments | High | ✅ Completed | Developer 1 | Create `request_uat`, `request_deployments`, and stage-scoped approver/comment tables |

### Phase B - Workflow Engine

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| RW15-005 | Implement immutable request display ID generator using org settings prefix/sequence | High | ✅ Completed | Developer 1 | Use `request.id_prefix` + `request.id_sequence`; persist generated display ID once |
| RW15-006 | Implement request workflow mode guardrails for approval-only and delivery-pipeline modes | High | ✅ Completed | Developer 1 | Enforce mode-specific transitions for UAT/deployment/completion actions |
| RW15-007 | Implement UAT lifecycle service with promotion guards and read-only lock after promotion | High | ✅ Completed | Developer 1 | Require parent request approval status `APPROVED`; required approvers gate promotion |
| RW15-008 | Implement deployment lifecycle with auto-approver inheritance and done detection | High | ✅ Completed | Developer 1 | Merge main+UAT approvers with dedupe and required flag preservation |

### Phase C - API Surface

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| RW15-009 | Expand request DTOs/controllers with approval and completion statuses | High | ✅ Completed | Developer 1 | Add compatibility-safe fields without breaking `/api/v1/change-requests` path |
| RW15-010 | Add department admin endpoints under settings domain | High | ✅ Completed | Developer 1 | CRUD-like management for active/inactive/reorder department data |
| RW15-011 | Add request link search and link upsert endpoints | Medium | ✅ Completed | Developer 1 | Query by display ID/title and save canonical bidirectional link sets |
| RW15-012 | Add UAT/deployment endpoints for comments, approvals, and promotion | High | ✅ Completed | Developer 1 | Stage-scoped endpoints with explicit authorization rules |

### Phase D - Frontend UX

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| RW15-013 | Rename UI copy/navigation labels from Change Requests to Requests | Medium | ✅ Completed | Developer 2 | Keep route compatibility but update labels/headings/buttons and menu entries |
| RW15-014 | Add department and workflow mode fields to request create/edit surfaces | High | ✅ Completed | Developer 2 | Source department options from admin settings; no free text fallback |
| RW15-015 | Add linked requests multi-search picker in request create/edit | High | ✅ Completed | Developer 2 | Search by display ID/title and render selected link chips |
| RW15-016 | Add Approval Status and Completion Status columns to request list | High | ✅ Completed | Developer 2 | Keep existing filters and pagination behavior stable |
| RW15-017 | Add UAT tab with initiate/edit forms and required/optional approver controls | High | ✅ Completed | Developer 2 | One UAT per request, inline form UX, and approver assignment flow |
| RW15-018 | Add deployment tab rendering from promoted UAT only | High | ✅ Completed | Developer 2 | No direct deployment creation path in UI |
| RW15-019 | Add stage comment and approval interactions for UAT and deployment | High | ✅ Completed | Developer 2 | Use stage-specific API contracts and keep mention UX parity |
| RW15-020 | Refactor oversized request detail page into UAT/deployment focused components | Medium | ✅ Completed | Developer 2 | Extract `CrUatPanel`, `CrDeploymentPanel`, and completion status control components |

### Phase E - Quality Gate

| Task ID  | Task | Priority | Status | Assigned To | Notes |
| -------- | ---- | -------- | ------ | ----------- | ----- |
| RW15-021 | Emit activity stream events for all UAT and deployment actions | High | ✅ Completed | Developer 1 | Ensure stage events include request display ID and actor context |
| RW15-022 | Emit audit trail entries for all UAT and deployment actions | High | ✅ Completed | Developer 1 | Mirror activity coverage with before/after payload where applicable |
| RW15-023 | Add backend regression tests for workflow transitions and authorization matrix | High | ✅ Completed | Developer 1 | Cover approval-only vs delivery-pipeline, promotion guards, completion rules |
| RW15-024 | Add frontend regression tests for requests tabs, status rendering, and linking UX | High | ✅ Completed | Developer 2 | Cover list dual-status columns, UAT/deployment tabs, and link search selection |

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
| Sprint 14      | 21          | 21          | 0           | 0         | 0%         |
| Sprint 15      | 24          | 0           | 0           | 24        | 100%       |
| Post-Sprint 1  | 19          | 0           | 0           | 19        | 100%       |
| Post-Sprint 2  | 8           | 0           | 0           | 8         | 100%       |
| Post-Sprint 3  | 9           | 0           | 0           | 9         | 100%       |
| Post-Sprint 4  | 6           | 0           | 0           | 6         | 100%       |
| Post-Sprint 5  | 7           | 0           | 0           | 7         | 100%       |
| Post-Sprint 6  | 3           | 0           | 0           | 3         | 100%       |
| **TOTAL**      | **292**     | **21**      | **0**       | **271**   | **93%**    |

### Progress by Developer

| Developer   | Assigned Tasks | Not Started | In Progress | Completed | Progress % |
| ----------- | -------------- | ----------- | ----------- | --------- | ---------- |
| Developer 1 | 156            | 13          | 0           | 143       | 92%        |
| Developer 2 | 136            | 8           | 0           | 128       | 94%        |

## Recent Implementations

### License Normalization — Switched to Apache 2.0 (Completed 2026-05-25)

**Overview**: Replaced custom source-available license (Commons Clause + Apache 2.0 base) with canonical Apache License 2.0. Aligned README and CONTRIBUTING docs.

**Files Created/Modified**:

- `LICENSE` — replaced 32-line source-available text with 201-line canonical Apache 2.0 license.
- `README.md` — updated license section to state "Apache 2.0" and removed resale restrictions.
- `CONTRIBUTING.md` — added inbound=outbound contributor licensing statement.
- `LICENSE-APACHE` — removed stale "no-resale condition" wording; now points to canonical `LICENSE`.

**Key Changes**:

- Project is now truly open source (OSI-compliant Apache 2.0).
- Commercial redistribution is permitted under Apache 2.0 terms.
- Contributor license is explicit: submissions are licensed under Apache 2.0.

**Verification**: `git diff --check -- LICENSE README.md CONTRIBUTING.md LICENSE-APACHE` — no whitespace or patch issues.

### Social Media Launch Kit (Completed 2026-05-25)

**Overview**: Prepared platform-specific copy and posting strategy for Audita public launch.

**Files Created**:

- `social-media-assets/README.md` — launch kit with copy, schedule, and engagement tips.

**Key Changes**:

- LinkedIn copy: playful/irreverent hook ("Your change approval process is just a shared Google Sheet with extra steps").
- Twitter/X copy: punchy hot-take format with feature pills.
- Reddit/HN copy: honest builder narrative with "Show HN" framing.
- 7-day posting sequence defined; hashtag strategy included.
- CTA across all platforms: star the repo (`github.com/skylarng89/audita`).

**Note**: Auto-generated image assets were discarded; user will provide own platform screenshots.

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

### Post-Sprint 7 CI Fix + Flyway Hardening + Unified Baseline (Completed 2026-05-25)

**Overview**: Fixed CI test failures, hardened Flyway migrations for replica-safe containerized deployments, and unified the tenant baseline into a single script for public launch readiness.

**Files Created/Modified**:

- `audita-web/server/utils/apiProxy.ts` - removed `"x-forwarded-host"` from `ALLOWED_HEADERS`.
- `audita-web/middleware/tenant.ts` - fixed to call `logout()` on authenticated tenant mismatch.
- `audita-web/tests/middleware/tenant.spec.ts` - test expectation aligned.
- `audita-api/infrastructure/src/main/java/io/audita/infrastructure/tenant/TenantMigrationStartupRunner.java` - added PostgreSQL advisory lock + opt-out property.
- `audita-api/api/src/main/resources/application.yml` - added `audita.migration.startup.enabled` property.
- `audita-api/infrastructure/src/main/resources/db/migration/tenant/V1__create_tenant_schema.sql` - **rewritten** with complete unified baseline (all former V1-V10 content).
- Deleted: `V2__seed_roles_and_permissions.sql`, `V3__create_groups.sql`, `V4__add_refresh_token_context.sql`, `V5__add_audit_indexes.sql`, `V6__add_user_roles.sql`, `V7__add_idempotency_keys.sql`, `V8__add_audit_export_requests.sql`, `V9__ensure_refresh_tokens_table.sql`, `V10__repair_refresh_tokens_table.sql`.

**Key Changes**:

- API proxy now strips client-spoofable `x-forwarded-host` (security boundary).
- Tenant middleware enforces logout on cross-tenant navigation for authenticated users.
- Startup tenant migrations use PostgreSQL advisory locks for replica safety.
- Single unified `V1` baseline eliminates 10-script replay and "already exists" warnings for new tenants.
- Future schema changes follow as V2, V3, etc.

**Test Coverage**: `cd audita-web && pnpm test` (47/47 pass); `cd audita-api && ./gradlew :api:test` (pass); `cd audita-api && ./gradlew :infrastructure:test` (pass).
