# Audita — Changelog

## [2.0.0] — 2026-06-18

### Added
- **Permission-Based RBAC** — All 88 `@PreAuthorize` annotations switched from `hasRole()` to `@authz.hasPermission()` using 22 permission codes. Custom roles are now functionally enforceable. `AuthorizationLogic` bean handles SUPER_ADMIN wildcard (`*`).
- **Watchers** — New participant type for CR and UAT. View-only with commenting + notifications. Mutually exclusive with approvers. Move watcher↔approver at any time before completion.
- **Deployment Single Assignee** — Replaced multi-approver deployment with single deployer assignee. Deployer marks completion, requester closes CR.
- **Custom Role CRUD** — Admin can create, edit, delete custom roles with permission picker. Overlap validation prevents full-permission and exact-set duplicate roles.
- **Copy URL Button** — Shareable request links with redirect-after-login.
- **File Logging** — Daily-rotated log files in `logs/` directory with env-configurable levels and retention.
- **Permission Catalogue Endpoint** — `GET /api/v1/roles/permissions` for admin UI.
- **Delete Role Endpoint** — `DELETE /api/v1/roles/{id}` with ROLE_IN_USE safety.
- **14 New Audit Action Types** — Watcher, deployment assignee, and role management actions.

### Changed
- **Simplified Roles** — Removed `Approver` system role. System roles: Admin, Requester, Auditor. Existing Approver users migrated to Requester.
- **All Approvers Required** — Removed Required/Optional toggle. `is_required`/`is_ad_hoc` columns removed. Simplified approval evaluation.
- **Read-Only on Completion** — `completionStatus == COMPLETED` blocks all mutations including comments. Core fields locked after submission (DRAFT-only editing).
- **Deployment Status Values** — `PENDING_APPROVAL`/`APPROVED`/`REJECTED` → `PENDING`/`COMPLETED`/`CANCELLED`.
- **Permission-Based Visibility** — `cr.view.all` permission grants global view (Admin, Auditor). Requesters see only requests they're part of (creator, approver, watcher, assignee).
- **Frontend Permission Gating** — Sidebar, middleware, and component UI checks use `auth.hasPermission()` instead of role comparisons.
- **Post-Login Redirect** — Unauthenticated access preserves `?redirect=` param for deep-link navigation after login.

### Removed
- `Approver` system role and all references
- `RequestDeploymentApproverEntity` and `RequestDeploymentApproverRepository`
- `request_deployment_approvers` table
- `is_required` from `cr_approvers` and `request_uat_approvers`
- `is_ad_hoc` from `cr_approvers`
- `updateApproverRequirement` endpoints and service methods (CR + UAT)
- `approveDeployment`/`rejectDeployment` endpoints and service methods

---

## [1.6.0] — 2026-06-17

### Added
- **UAT Requester Sign-Off** — CR requester can sign-off on UAT without being listed as approver. V10 migration adds `requester_signed_off` column. Promote requires requester sign-off; relaxes gate when no required approvers exist.
- **UAT Approver Required/Optional Toggle** — new `PATCH /uat/approvers/{id}/requirement` endpoint. Frontend toggle button replaces static badge in `CrUatPanel.vue`.
- **UAT Approver Sign-Off Buttons** — "Sign-Off" and "Reject" buttons in UAT panel when current user is a pending approver, with rejection reason input.
- **UAT Visual Status** — colored badges (green/red/amber) for approver status, `decidedAt` timestamp, `rejectionReason` display, "Signed Off ✓" requester indicator.
- **Deployment Tab Approvers** — `RequestDeploymentResponse` now includes approvers list with user resolution. `GET /deployment/approvers` endpoint.
- **Logout Re-entrancy Guard** — `loggingOut` flag prevents cascading duplicate `logout()` calls from `refreshSession().catch`.

### Fixed
- **Deployment tab blank after promotion** — `CrDeploymentPanel.vue` crashed on `deployment.approvers.length` (undefined). Added approvers to response DTO + null-safety.
- **Deployment status mismatch** — frontend checked `"PENDING"` but backend sends `"PENDING_APPROVAL"`.
- **Deployment type mismatch** — `changeRequestId` renamed to `requestId` in `Deployment` interface.
- **Logout overlay stuck** — `router.afterEach` auto-hides overlay on `/auth/*` routes.
- **New request form overlay stuck** — added `hideLoading()` in `onMounted` finally block.
- **Group dropdowns empty** — `listActiveGroups()` now extracts `.content` from paginated response.
- **Complete button missing for APPROVAL_ONLY** — `CrCompletionStatusControl.vue` now shows button for approved `APPROVAL_ONLY` requests.
- **429 Rate Limiting** — disabled `nuxt-security` default rate limiter (backend handles auth-specific limits).
- **Auditor defense-in-depth** — service-layer check prevents Auditors from being added as approvers. Frontend candidate lists filter Auditors.

---

## [1.5.0] — 2026-06-16

### Fixed
- **Loading overlay stuck after fast page loads** — race condition between `showTimer` (200ms deferred show) and `hide()`. If page loaded in <200ms, timer fired after hide, re-showing overlay. Fixed with `showId` counter pattern in composable.
- **Loading overlay stuck on API errors** — `hideLoading()` missing in catch/error blocks of 4 pages (change-requests detail, change-requests list, dashboard, admin settings).
- **CSP violations (style-src-elem/attr)** — Tiptap, Vue templates, and browser autofill inline styles blocked. Added `style-src-elem` and `style-src-attr` directives with `'unsafe-inline'`.
- **SLA defaults not persisting** — `TenantService.saveSetting()` used fragile find-then-setValue pattern. Replaced with direct `save()` upsert. Frontend now re-reads from API response.
- **Blank UAT tab after save** — `CrUatPanel.vue:167` crashed on `uat.approvers.length` (undefined after create). Fixed with null-safety (`?.`). Added approvers and creator full name to `RequestUatResponse` DTO.
- **Groups/Departments disconnect** — Groups and Departments were separate entities with no sync. Unified by adding `isActive`/`displayOrder` to Groups, `requestGroupId`/`destinationGroupId` to change requests, and switching form dropdowns to use groups API.

### Added
- **`RequestUatResponse`** — now includes `approvers`, `createdByFullName`, `promotedAt` fields
- **`GET /{id}/uat/approvers`** endpoint with user resolution
- **`listUatApprovers()`** frontend composable function
- **V9 tenant migration** — adds `is_active`/`display_order` to `groups`, adds `request_group_id`/`destination_group_id` to `change_requests`
- **`listActiveGroups()`** composable function calling `GET /api/v1/groups?active=true`
- **Group isActive/displayOrder** fields exposed via API

### Changed
- **Loading overlay** — debounce pattern replaced with `showId` counter for race-proof deferred show
- **CSP** — `style-src-elem: ['unsafe-inline']`, `style-src-attr: ['unsafe-inline']`
- **saveSetting** — simplified from find-then-setValue to direct upsert
- **Change request form** — department dropdowns now source from groups API instead of departments

---

## [1.4.0] — 2026-06-15

### Fixed
- **Department dropdowns always empty on change request form** — `UpsertDepartmentRequest.isActive` was primitive `boolean`, defaulting to `false` when omitted from JSON. Departments created without explicit `"isActive": true` were saved as inactive and excluded by `listActive()`. Fixed by changing to `@NotNull Boolean isActive`.
- **Scope/Description editor missing on new request form** — `useEditor()` initialization was accidentally deleted in commit `a1580f8` when adding department/linked-request refs. Restored the Tiptap editor initialization.
- **Cannot create change request** — same root cause as editor issue. `editor.value?.getHTML()` threw TypeError because `editor` was undefined.

### Added
- **Global full-screen loading overlay** — replaces thin `<NuxtLoadingIndicator>` bar. Centered Audita logo + spinner on dimmed blurred backdrop. Auto-shows on route change (200ms debounce), hides when page data resolves. Integrated into all 13 data-fetching pages.
- **Department empty-state hint** — helpful message on new request form when no active departments exist, with link to Organization Settings.

---

## [1.3.0] — 2026-06-15

### Fixed
- **Audit log payload hardening** — audited all 27 audit log call sites across 5 services:
  - 6 empty/missing payloads fixed (`GROUP_DELETED`, `GROUP_MEMBERS_ADDED`, `GROUP_MEMBERS_REMOVED`, `UAT_APPROVED`, `DEPLOYMENT_APPROVED`, `CR_REJECTED` missing `reason`)
  - 11 missing actor emails resolved (all UAT and Deployment calls, `GroupService` now receives `actorUserId` from controller)
  - IP address captured and logged for all 27 sites via new `RequestContext` ThreadLocal populated by `TenantResolutionFilter` from `X-Forwarded-For` header

### Added
- **`RequestContext.java`** — ThreadLocal IP address holder (mirrors `TenantContext` pattern)
- **`TenantResolutionFilter`** — IP capture via `extractClientIp()` helper

---

## [1.2.0] — 2026-06-15

### Fixed
- **Groups page not rendering** — Vue 3.5 generic SFC (`AppTable.vue`) silently fails at runtime in Nuxt 4.4.8 SPA mode, producing zero DOM. Fixed by removing generic directive and using concrete types. Groups page also uses raw inline `<table>` as permanent safe rendering.
- **Audit log hash chain columns** — V8 tenant migration idempotently adds `chain_index`, `record_hash`, `previous_hash` columns to `audit_log` table (columns were recorded in `flyway_schema_history` but never actually created).
- **CSP restored** with `script-src-attr: ['unsafe-hashes']`, `nonce: true`.
- **Setup form width** — added `class="w-full"` to match login form width.

### Added
- **App version display** in sidebar footer (via Docker `ARG APP_VERSION` → `NUXT_PUBLIC_APP_VERSION` → `runtimeConfig.public.appVersion`).
- **SPA cold-start loading indicator** via static `app.html` CSS spinner (removed on Vue mount).
- **V8 tenant migration** (`V8__repair_audit_log_hash_chain.sql`) — idempotent audit log chain column repair.

---

## [1.1.0] — 2026-06-14

### Changed
- **Versioning reset** — dropped "v" prefix from all SemVer tags and Docker image tags. Baseline at 1.0.0.
- **CI workflow** — tag filter `v*` → `[0-9]*.[0-9]*.[0-9]*`, all version strings stripped of "v" prefix, default → `1.0.0`.
- **docker-compose.yml** — `v0.7.0` → `1.0.0` for both images.
- **42/50 remote `v*` git tags deleted**; 8 blocked by repo tag protection rules.

### Added
- **Full notification system** wired across 6 services:
  - `ChangeRequestService` — 7 hooks (submit, addApprover, addApproverGroup, approve, reject, cancel, completeRequest)
  - `RequestUatService` — 4 hooks (addApprover, approveUat, rejectUat, createComment)
  - `RequestDeploymentService` — 4 hooks (createFromPromotion, approveDeployment, rejectDeployment, createComment)
  - `CommentService` — delegated to `MentionNotifier`
  - `SlaMonitoringService` — added WARNING email
  - `AuditExportAsyncService` — added in-app notification
- **Shared `MentionNotifier`** — extracts `@email` mentions from comment HTML, resolves users, sends in-app + email.
- **8 new `EmailService` methods** — UAT/Deployment approval requests/decisions, SLA warning, CR cancelled/completed, simplified mention overload.
- **7 new email templates** — `uat-approval-request.html`, `uat-approval-decision.html`, `deployment-approval-request.html`, `deployment-approval-decision.html`, `sla-warning.html`, `cr-cancelled.html`, `cr-completed.html`.
- **App version display pipeline** — CI `build-args: APP_VERSION` → Dockerfile `ENV` → nuxt config `runtimeConfig` → sidebar `<p>`.

### Fixed
- **Groups page error handling** — replaced silent `.catch(() => null)` with explicit `async/try/catch` + `loadError` display.
- **Groups data fetching** — aligned with proven users page pattern (direct `useApi()` + inline URL string, explicit `layout: "default"`).

---

## [0.7.0] — 2026-06-06

### Added
- **Sprint 15** — Requests Workflow Expansion (conditional workflow modes, UAT/Deployment lifecycle, immutable display IDs, department master data, bidirectional request links)
- **Groups feature overhaul** — unified page, wizard creation, member management, invite integration, delete safety
- **SSO rebuild** — Google/Microsoft OIDC, callback exchange, tenant SSO config tab
- **Sprint 14** — Security Audit Remediation (21 tasks across 5 phases)

### Fixed
- **E2E test fix** — token version staleness after reactivation
- **Dockerfile corepack** — replaced with `npm install -g pnpm`
- **Auth rate limiter** — only counts failures, success resets counter
- **Case-sensitive email lookup** in login
- **PENDING status guard** in login
