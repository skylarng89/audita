# Audita — Architectural Decisions

**Format:** Append-only. New entries at the bottom.

---

## ADR-030: Drop "v" Prefix from Version Tags and Docker Image Tags

**Date:** 2026-06-14
**Status:** Accepted

**Decision:** Remove the "v" prefix from all SemVer git tags and Docker image tags. Reset baseline to 1.0.0.

**Reasoning:**
- The "v" prefix is redundant — SemVer is already unambiguous without it.
- Container registries sort tags by lexicographic order, where `v1.0.0` and `1.0.0` create separate sort groups.
- Docker image tags without prefix are cleaner for compose files and Helm charts.

**Files changed:** `.github/workflows/ci-release.yml`, `docker-compose.yml`, `README.md`

---

## ADR-031: Static HTML Loading Indicator for SPA Cold Starts

**Date:** 2026-06-14
**Status:** Accepted

**Decision:** Use a static CSS loading spinner in `app.html` (outside the Nuxt mount point) that is removed via `onMounted` in `app.vue`. The `<NuxtLoadingIndicator>` remains for in-app SPA transitions.

**Reasoning:** With `ssr: false`, the Nuxt app does not exist during the initial JS bundle download. Cold page loads show a blank white screen. A static CSS spinner provides instant feedback.

**Files changed:** `audita-web/app.html` (new), `audita-web/app.vue`

---

## ADR-032: useAsyncData Error Handling — Explicit try/catch Over Silent .catch(() => null)

**Date:** 2026-06-14
**Status:** Accepted

**Decision:** In Nuxt 3/4 pages using `useAsyncData`, always use explicit `async () => { try { ... } catch { loadError.value = ...; return fallback } }` instead of `.catch(() => null)`. The fallback must be structurally valid for the expected type.

**Reasoning:** `.catch(() => null)` silently swallows all promise rejections — the page renders empty state with zero feedback. With `ssr: false`, the `loadError` ref is the only error channel.

**Reference:** `pages/users/index.vue` lines 313-341

---

## ADR-033: Shared MentionNotifier for Comment Mention Extraction

**Date:** 2026-06-14
**Status:** Accepted

**Decision:** Extract `@email` mention parsing, user resolution, and notification/email dispatch from `CommentService` into a shared `MentionNotifier` class. `CommentService`, `RequestUatService`, and `RequestDeploymentService` each inject `MentionNotifier` for their `createComment` methods.

**Reasoning:** CommentService had full mention support (regex parsing + in-app + email). UAT and Deployment comments had zero mention support. Extracting the logic into a shared helper avoids duplicating the EMAIL_MENTION_PATTERN regex and 5-line notification call across three services.

**Files created:** `MentionNotifier.java`

---

## ADR-034: Modular Email Templates — One Per Notification Type

**Date:** 2026-06-14
**Status:** Accepted

**Decision:** Each notification type gets its own standalone Thymeleaf HTML template file. Templates are ~15 lines each with no shared conditionals or includes. New templates created for UAT, Deployment, SLA warning, CR cancellation, and CR completion notifications. Existing CR approval request/decision templates (dead code) are now wired.

**Rationale:** Modular templates are independently editable without affecting other notification types. The dead-code `approval-request.html` and `approval-decision.html` templates followed this pattern and were reused.

**Templates created:** `uat-approval-request.html`, `uat-approval-decision.html`, `deployment-approval-request.html`, `deployment-approval-decision.html`, `sla-warning.html`, `cr-cancelled.html`, `cr-completed.html`

---

## ADR-035: AppTable Generic SFC Must Use Concrete Types, Not Vue Generics

**Date:** 2026-06-15
**Status:** Accepted

**Decision:** Do not use Vue 3.5 `<script setup lang="ts" generic="T">` in Nuxt 4.4.8 SPA mode. Generic SFCs fail silently at runtime — the component template produces zero DOM output with no console errors or warnings. Replace with concrete types: `data: Record<string, unknown>[]`, `columns: { key: string; label: string; ... }[]`, `rowKey?: string`.

**Reasoning:**
- The `AppTable.vue` component rendered literally nothing in production builds despite receiving correct data (`groups.length=6` confirmed by debug bar).
- Diagnostics (Network tab, DOM debug bar, raw HTML replacement) systematically eliminated data layer, reactivity layer, CSP, and esbuild stripping as causes.
- Replacing the generic directive with concrete types restored rendering immediately.
- The `Record<string, unknown>` constraint also rejects objects with `null` property values (e.g., `Group.description: null`), causing an additional type mismatch.

**Trade-offs:** Loss of compile-time type safety on row/cell access. Acceptable — the slots and cell templates are runtime-enforced by Vue's slot system.

**Files changed:** `AppTable.vue`

---

## ADR-037: RequestContext ThreadLocal for Client IP Capture in Audit Logs

**Date:** 2026-06-15
**Status:** Accepted

**Decision:** Create `RequestContext` (mirroring `TenantContext` pattern) with a ThreadLocal for the client IP address. Populate it in `TenantResolutionFilter.doFilterInternal()` from the `X-Forwarded-For` header (first proxy IP) or `request.getRemoteAddr()` as fallback. Clear in the finally block alongside `TenantContext.clear()`. All 27 `auditLogService.log()` calls pass `RequestContext.getCurrentIp()` as the ipAddress argument instead of `null`.

**Reasoning:**
- All 27 audit log call sites passed `null` for ipAddress — zero traceability on request origin.
- Spring's `RequestContextHolder` pattern adds framework coupling in infrastructure services; a project-level ThreadLocal is simpler and matches the existing TenantContext approach.
- `X-Forwarded-For` first-IP extraction is the standard pattern for proxied deployments (Nginx sets this header).
- The finally-block clear prevents ThreadLocal leaks in thread-pooled servlet containers.

**Trade-offs:**
- Requires the proxy (Nginx) to set `X-Forwarded-For` correctly. If misconfigured, falls back to `request.getRemoteAddr()`.
- ThreadLocal is not compatible with reactive/async servlet dispatch. Currently acceptable since the app uses synchronous servlets.

**Files created:** `RequestContext.java`
**Files modified:** `TenantResolutionFilter.java`, all 5 service files with `.log()` calls

---

## ADR-038: Global Full-Screen Loading Overlay Replaces NuxtLoadingIndicator

**Date:** 2026-06-15
**Status:** Accepted

**Decision:** Replace the built-in `<NuxtLoadingIndicator>` (thin top progress bar) with a custom full-screen centered overlay showing the Audita logo, a spinner, and "Loading..." text on a dimmed blurred backdrop. Overlay auto-shows on route change with 200ms debounce, hides when each page's main data fetch resolves. Safety auto-hide after 15 seconds.

**Reasoning:**
- The thin blue bar was barely noticeable and gave no indication to users that data was loading.
- A centered overlay with logo provides brand presence during load states and clear visual feedback.
- The 200ms debounce prevents flash on fast navigations.
- Per-page `hideLoading()` integration gives pages full control over when they consider themselves "ready."

**Implementation:**
- `composables/useLoadingOverlay.ts` — shared reactive state with `show()`/`hide()`
- `components/shared/SharedLoadingOverlay.vue` — `<Teleport>` + `<Transition>` component
- `app.vue` — route watcher for auto-show
- 13 data-fetching pages — `hideLoading()` call on data resolve

**Files created:** `useLoadingOverlay.ts`, `SharedLoadingOverlay.vue`

---

## ADR-039: Boxed Boolean with @NotNull for Department isActive Field

**Date:** 2026-06-15
**Status:** Accepted

**Decision:** Change `UpsertDepartmentRequest.isActive` from primitive `boolean` to `@NotNull Boolean`. Update `DepartmentService.create()` and `update()` signatures to match.

**Reasoning:** Primitive `boolean` defaults to `false` when omitted from JSON during Jackson deserialization. This caused departments created via API without explicit `"isActive": true` to be saved as inactive. Inactive departments are filtered out by `listActive()` which is the sole data source for department dropdowns on change request forms. The `@NotNull` constraint forces callers to explicitly choose active/inactive, preventing the silent inactivation bug.

**Files modified:** `UpsertDepartmentRequest.java`, `DepartmentService.java`

---

## ADR-040: Global Full-Screen Loading Overlay Replaces NuxtLoadingIndicator

**Date:** 2026-06-16
**Status:** Accepted

**Decision:** Replace the built-in `<NuxtLoadingIndicator>` (thin top progress bar) with a custom full-screen centered overlay showing the Audita logo, a spinner, and "Loading..." text on a dimmed blurred backdrop. Overlay uses an incrementing `showId` counter pattern to eliminate the race condition where a deferred `show()` fires after a fast page already called `hide()`.

**Reasoning:**
- The thin blue bar was barely noticeable and gave no indication to users that data was loading.
- A centered overlay with logo provides brand presence during load states and clear visual feedback.
- The 200ms debounce uses a `showId` counter — `hide()` increments the counter, invalidating any pending deferred `show()`.

**Implementation:**
- `composables/useLoadingOverlay.ts` — `triggerShow()` captures a snapshot ID; 200ms later, only shows if ID still matches
- `components/shared/SharedLoadingOverlay.vue` — `<Teleport>` + `<Transition>` component
- `app.vue` — route watcher calls `triggerShow()`; onMounted calls `hide()`
- 13 data-fetching pages — `hideLoading()` on data resolve
- 4 pages fixed — added `hideLoading()` to catch/error blocks

**Files created:** `useLoadingOverlay.ts`, `SharedLoadingOverlay.vue`

---

## ADR-041: Unified Groups and Departments — Groups as Single Source

**Date:** 2026-06-16
**Status:** Accepted

**Decision:** Merge the separate Groups and Departments concepts into a unified Groups entity. Groups now carry `isActive` and `displayOrder` fields (previously department-only). Change request forms reference `requestGroupId`/`destinationGroupId` instead of department IDs. The `departments` table and related infrastructure is retained for backward compatibility during transition.

**Reasoning:**
- Groups and Departments were implemented as separate entities with no synchronization, causing confusion and empty dropdowns.
- There was no frontend UI for department management, making it impossible to populate the form dropdowns.
- The `/groups` page already serves as the admin's organizational unit management — extending it to serve departments eliminates redundancy.
- A new migration (`V9__unify_groups_departments.sql`) adds `is_active`/`display_order` to `groups` and FK columns to `change_requests`. Both old (department) and new (group) FK columns coexist.

**Files changed:** 10+ backend files + 5 frontend files

---

## ADR-042: UAT Response Includes Approvers and Creator Full Name

**Date:** 2026-06-16
**Status:** Accepted

**Decision:** Extended `RequestUatResponse` to include `approvers`, `createdByFullName`, and `promotedAt` fields. Added `GET /{id}/uat/approvers` endpoint. Extended `RequestUatApproverResponse` to include `userFullName` and `userEmail` (resolved via batch user lookup). Added `listUatApprovers()` to frontend composable. Fixed blank UAT tab crash by adding null-safety (`uat.approvers?.length`).

**Reasoning:** After UAT creation, the response had no approvers field, causing Vue template render crashes. Approvers were inaccessible via API despite existing in the database. User resolution was done in the controller via new service methods (`loadApproverUsers`, `resolveUserFullName`).

**Files changed:** `RequestUatController.java`, `RequestUatResponse.java`, `RequestUatApproverResponse.java`, `RequestUatService.java`, `useChangeRequests.ts`, `CrUatPanel.vue`

---

## ADR-043: CSP style-src-elem and style-src-attr Directives

**Date:** 2026-06-16
**Status:** Accepted

**Decision:** Added `style-src-elem` and `style-src-attr` CSP directives with `'unsafe-inline'` to `nuxt.config.ts`. This fixes violations from Tiptap runtime styles, Vue dynamic bindings, and browser autofill overlays — which cannot carry SSR nonces in SPA mode. No actual CORS issues exist.

**Files changed:** `nuxt.config.ts`

---

## ADR-044: Simplified saveSetting — Direct Upsert

**Date:** 2026-06-16
**Status:** Accepted

**Decision:** Replaced the fragile find-then-setValue pattern in `TenantService.saveSetting()` with `orgSettingRepository.save(new OrgSettingEntity(key, value))`. Spring Data JPA's `save()` handles upsert via merge — no need for explicit find-then-mutate. SLA defaults now persist reliably.

**Files changed:** `TenantService.java`

---

## ADR-036: Diagnostic Approach for Silent Vue Rendering Failures

**Date:** 2026-06-15
**Status:** Accepted

**Decision:** When a Vue component produces zero DOM with no errors, follow this diagnostic sequence:
1. Add a visible DOM debug bar showing live reactive values (`pending`, `groups.length`, `data.contentLength`)
2. Confirm API response in browser Network tab
3. Replace the component with raw HTML equivalent to isolate it as the failure point
4. Once confirmed, trace back through build pipeline (generic SFCs, CSP, esbuild stripping)

**Reasoning:** This sequence eliminated 6 potential causes (data layer, reactivity, CSP, esbuild, NuxtLoadingIndicator, layout resolution) and isolated the generic SFC compiler as the root cause in under 30 minutes.

---

## ADR-045: Requester Sign-Off on UAT

**Date:** 2026-06-17
**Status:** Accepted

**Decision:** Allow the change request requester to sign-off on UAT without being explicitly listed as an approver. Added `requesterSignedOff` boolean to `RequestUatEntity` (V10 migration). `approveUat()` bypasses `loadApprover()` check when actor is the CR creator. `promoteToDeployment()` requires requester sign-off and relaxes the "at least one required approver" gate — if no required approvers exist, requester sign-off alone suffices.

**Reasoning:** The requester who raised the UAT should be able to acknowledge satisfaction. Adding them as an explicit approver is redundant and clutters the approver list. The sign-off is a formal acknowledgment separate from approver voting.

**Files changed:** `RequestUatEntity.java`, `RequestUatService.java`, `RequestUatResponse.java`, `CrUatPanel.vue`, V10 migration

---

## ADR-046: Logout Re-entrancy Guard

**Date:** 2026-06-17
**Status:** Accepted

**Decision:** Add `loggingOut` boolean flag to Pinia auth store to prevent re-entrant `logout()` calls. When `refreshSession().catch` in `plugins/api.ts` triggers `auth.logout()` while the original logout is still in-flight (due to in-flight API calls getting 401 before `clearAuth()` sets `isAuthenticated = false`), the re-entrant call is silently dropped.

**Reasoning:** The double-logout cascade caused Vue Router navigation collisions that corrupted the `router.afterEach` hook, leaving the loading overlay stuck after sign-out.

**Files changed:** `stores/auth.ts`

---

## ADR-047: Auditor Role Enforcement at Service Layer

**Date:** 2026-06-17
**Status:** Accepted

**Decision:** Add Auditor role validation at the backend service layer in `addApprover()` methods. Previously, the controller `@PreAuthorize` and frontend UI blocked Auditors from calling approve endpoints, but an Admin/Requester could add an Auditor as an approver — giving them approval power through a backdoor. The service now throws `FORBIDDEN` if the target user has the Auditor role. `searchApproverCandidates()` filters out Auditors. Frontend candidate lists also filter as defense-in-depth.

**Reasoning:** Auditors are observers only — they should never be approvers. Adding them as approvers would block requests (Auditors can't approve/reject, but the system would wait for their vote). Service-layer enforcement is the single point of truth.

**Files changed:** `ChangeRequestService.java`, `RequestUatService.java`, `[id].vue`, `CrUatPanel.vue`

---

## ADR-048: Disable nuxt-security Rate Limiter

**Date:** 2026-06-17
**Status:** Accepted

**Decision:** Disable the `nuxt-security` module's default rate limiter (`rateLimiter: false` in `nuxt.config.ts`). The Spring Boot backend already handles auth-specific rate limiting (`InMemoryRateLimitService` wired into login/forgot-password endpoints). The Nuxt-layer limiter (150 req/5min/IP) was blocking normal SPA usage — the CR detail page alone fires 12+ parallel API calls.

**Files changed:** `nuxt.config.ts`

---

## ADR-049: Permission-Based RBAC Replacing Role-Based Authorization

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** Replace all 88 `@PreAuthorize` annotations using `hasRole()`/`hasAnyRole()` with `@authz.hasPermission(authentication, 'permission.code')` via a custom `AuthorizationLogic` Spring bean. The existing permission infrastructure (permissions table, role_permissions join, JWT `permissions` claim, `UserPrincipal` authorities) was already provisioned since V1 but never enforced. This change activates it.

SUPER_ADMIN receives a `*` wildcard `SimpleGrantedAuthority` in `UserPrincipal.ofSuperAdmin()`, bypassing all permission checks. The `AuthorizationLogic.hasPermission()` method checks for `*` first, then exact permission match.

The `Approver` system role was removed. System roles are now: Admin, Requester, Auditor. Custom roles remain admin-defined and are now functionally enforceable (previously inert because `hasRole()` never matched custom role names).

4 new permissions added: `cr.view.all` (global view access for Admin/Auditor), `cr.manage_participants` (add/remove approvers/watchers/assignees), `uat.signoff` (UAT sign-off), `deployment.execute` (mark deployment completed). Total: 22 permissions.

**Files changed:** `AuthorizationLogic.java` (new), `UserPrincipal.java`, `RoleHierarchy.java`, all 16 controllers, `V1__create_tenant_schema.sql`

---

## ADR-050: Watchers — View-Only Participants with Commenting

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** Introduce a watcher concept for change requests and UAT stages. Watchers can view the request, view attachments, add comments, and receive notifications (in-app + email) for status changes and new comments. They cannot approve/reject, edit the request, or manage participants.

Watchers are mutually exclusive with approvers on the same request — a user cannot be both. The requester can move a watcher to approver or an approver to watcher at any time before completion (only PENDING approvers can be demoted — those who already voted are part of the audit trail).

Auditors cannot be added as watchers (they already have global view access via `cr.view.all`).

New tables: `cr_watchers`, `request_uat_watchers` (both with `is_sample` flag for sample data cleanup).

**Files changed:** `CrWatcherEntity.java` (new), `RequestUatWatcherEntity.java` (new), `CrWatcherRepository.java` (new), `RequestUatWatcherRepository.java` (new), `ChangeRequestService.java`, `RequestUatService.java`, `ChangeRequestController.java`, `RequestUatController.java`, V1/V3 migrations

---

## ADR-051: Deployment Single Assignee Model

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** Replace the deployment multi-approver model with a single assignee. The requester assigns one user to handle the deployment. That user clicks "Mark Deployment Completed" to mark the deployment as done. The requester then clicks "Mark Complete" on the CR to close it.

The `request_deployment_approvers` table and `RequestDeploymentApproverEntity` were deleted. The `request_deployments` table now has an `assignee_id` column (nullable FK to users, `ON DELETE SET NULL`). Deployment status values changed from `PENDING_APPROVAL`/`APPROVED`/`REJECTED` to `PENDING`/`COMPLETED`/`CANCELLED`.

The assignee can be changed at any time while status is PENDING. Only the assignee (or Admin) can mark the deployment as completed. Auditors cannot be assigned as deployers.

**Files changed:** `RequestDeploymentEntity.java`, `RequestDeploymentService.java`, `RequestDeploymentController.java`, `RequestDeploymentResponse.java`, `AssignDeployerRequest.java` (new), V3 migration, deleted `RequestDeploymentApproverEntity.java` + `RequestDeploymentApproverRepository.java`

---

## ADR-052: All Approvers Required — No Optional Toggle

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** Remove the Required/Optional toggle for approvers. All approvers are required. The `is_required` column was removed from `cr_approvers` and `request_uat_approvers`. The `is_ad_hoc` column was also removed from `cr_approvers`.

The approval evaluation logic in `ChangeRequestEntity.evaluateApprovalClosure()` was simplified: if any approver rejects → REJECTED, if all approvers approve → APPROVED, otherwise stays PENDING_APPROVAL. No more required/optional distinction.

The `updateApproverRequirement` endpoints and service methods were deleted. The frontend toggle UI was removed.

**Files changed:** `CrApproverEntity.java`, `RequestUatApproverEntity.java`, `ChangeRequestEntity.java`, `ChangeRequestService.java`, `RequestUatService.java`, `ChangeRequestController.java`, `RequestUatController.java`, V1/V3 migrations

---

## ADR-053: Read-Only Boundary on Completion

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** A change request becomes fully read-only when `completionStatus == COMPLETED`. All mutation methods in `ChangeRequestService`, `RequestUatService`, and `RequestDeploymentService` call `assertNotCompleted()` which throws `InvalidStateTransitionException("REQUEST_COMPLETED")`. Comments are also locked when completed.

Core fields (title, description, priority, etc.) are locked after submission — only editable in DRAFT status. Participant management (approvers, watchers, assignee) continues until completion.

The completion flow is manual for both workflow modes:
- APPROVAL_ONLY: requester clicks "Mark Complete" after all approvers approve
- DELIVERY_PIPELINE: deployer marks deployment completed, then requester clicks "Mark Complete"

**Files changed:** `ChangeRequestService.java`, `RequestUatService.java`, `RequestDeploymentService.java`, `CrCompletionStatusControl.vue`

---

## ADR-054: File Logging with Daily Rotation

**Date:** 2026-06-18
**Status:** Accepted

**Decision:** Add a `FILE` RollingFileAppender to `logback-spring.xml` active in all Spring profiles. Log files are written to `logs/audita-api.log` (relative to API working directory), rotated daily and at 50MB size boundary, with gzipped archives. Max history 30 days, total size cap 1GB.

Log levels are env-configurable: `LOG_LEVEL_ROOT` (default INFO), `LOG_LEVEL_APP` (default DEBUG for `io.audita` package), `LOG_LEVEL_FLYWAY` (default INFO). File rotation parameters also env-configurable: `LOG_FILE_MAX_SIZE`, `LOG_FILE_MAX_HISTORY`, `LOG_FILE_TOTAL_SIZE_CAP`.

The existing `CONSOLE_JSON` (non-dev) and `CONSOLE_PLAIN` (dev) appenders are preserved. The FILE appender is added alongside them in all profiles.

**Files changed:** `logback-spring.xml`, `application.yml`, `.env.example`, `.gitignore`, `audita-api/logs/.gitkeep`
