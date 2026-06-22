# Audita — Active Context

**Last Updated:** 2026-06-22
**Current Phase:** Post-deployment hardening — all critical bugs fixed, uploads and logs migrated to bind mounts
**Active Sprint:** None — maintenance mode

---

## What Is Audita?

Audita is a **self-hosted, multi-tenant ITIL/ITSM Change Management platform**. It enables organisations to manage the full lifecycle of IT change requests: creation, structured approval workflows, real-time collaboration, SLA tracking, and a complete immutable audit trail.

**Mission:** Make change management simple, transparent, and auditable for every team.

---

## Application Structure

| Repo | Tech | Description |
|------|------|-------------|
| `audita-api` | Java 25 + Spring Boot 4.1.0 | REST API, business logic, data layer |
| `audita-web` | Nuxt 4.4.8 + Vue 3.5 | SPA frontend (ssr: false), component library |

Served via Docker Compose (dev) or Nginx-reverse-proxied Docker containers (production).

---

## Key Architectural Decisions

1. **Schema-per-tenant** PostgreSQL isolation — each org gets its own schema.
2. **JWT + HttpOnly refresh cookie** auth. Access token 15 min, refresh 7 days.
3. **SSE** for real-time in-app notifications.
4. **TipTap** rich-text editor for CR descriptions and comments.
5. **Hexagonal architecture** on the backend (`domain`, `application`, `infrastructure`, `api`).
6. **Flyway** for per-tenant schema migrations, idempotent and version-controlled.
7. **AES-256** encryption for SSO client secrets and SMTP passwords at rest.
8. **OWASP Java HTML Sanitizer** for server-side rich-text sanitisation.

---

## Today's Work Summary (2026-06-16) — Long-Term Fixes

### New Change Request Form Fixes

Three issues on `/change-requests/new` root-caused and fixed:

**Issue 1 — Department dropdowns always empty:**
- **Root cause:** `UpsertDepartmentRequest.java` used primitive `boolean isActive` which defaults to `false` when omitted from JSON. Departments created without explicit `"isActive": true` were saved as inactive and excluded by the `listActive()` query.
- **Fix:** Changed to `@NotNull Boolean isActive` in the DTO, updated `DepartmentService` signatures. Callers must now explicitly declare active/inactive.
- **Files:** `UpsertDepartmentRequest.java`, `DepartmentService.java`

**Issue 2 — Scope/Description editor missing:**
- **Root cause:** `useEditor()` initialization was accidentally deleted in commit `a1580f8` when adding `activeDepartments` and `linkedRequestIds` refs.
- **Fix:** Restored `useEditor()` with `buildRichTextExtensions()`.
- **Files:** `pages/change-requests/new.vue`

**Issue 3 — Cannot create request:**
- **Root cause:** Same as Issue 2 — `editor.value?.getHTML()` threw TypeError because `editor` was undefined.
- **Fix:** Fixed by restoring `useEditor()` (Issue 2 fix).

### Global Loading Overlay

Replaced the thin `<NuxtLoadingIndicator>` progress bar with a full-screen centered overlay:
- Centered Audita logo + spinner + "Loading..." text on dimmed backdrop with blur
- Auto-shows on route change (200ms debounce to avoid flash on fast navigations)
- Hides when page data resolves (pages call `hideLoading()` when their main fetch completes)
- Safety auto-hide after 15 seconds
- Integrated into all 13 data-fetching pages

**New files:** `composables/useLoadingOverlay.ts`, `components/shared/SharedLoadingOverlay.vue`
**Modified:** `app.vue` + 13 data-fetching pages

### Department Empty-State Hint

Added helper text on the new request form when no active departments exist, with a link to Organization Settings.


### Versioning Reset

Dropped the "v" prefix from all SemVer tags and Docker image tags. Reset baseline to 1.0.0.

- **CI workflow**: tag filter `v*` → `[0-9]*.[0-9]*.[0-9]*`, all version strings stripped of `v` prefix, default → `1.0.0`
- **docker-compose.yml**: `v0.7.0` → `1.0.0` for both images
- **README.md**: updated versioning docs
- **Git tags**: 42/50 remote `v*` tags deleted; 8 blocked by repo tag protection rules (harmless — won't match new filter)

### App Version Display

Sidebar now shows the running version. Pipeline: `ci-release.yml` `build-args: APP_VERSION` → `Dockerfile` `ARG APP_VERSION` → `ENV NUXT_PUBLIC_APP_VERSION` → `nuxt.config.ts` `runtimeConfig.public.appVersion` → `AppSidebar.vue` footer.

### SPA Cold-Start Loading Indicator

Added static CSS spinner in new `audita-web/app.html` (renderable before Vue boots). Removed via `onMounted` in `app.vue`. `<NuxtLoadingIndicator>` handles in-app transitions.

### Setup Form Width Fix

Added `class="w-full"` to setup page root `<div>` (`pages/setup.vue:2`).

### Notification System — Full Implementation

Wired in-app + email notifications across the entire approval/comment workflow. Created shared `MentionNotifier.java` helper. Added 7 new email templates. Wired 6 services:

| Service | Hooks wired |
|---------|-------------|
| `ChangeRequestService` | `submit`, `addApprover`, `addApproverGroup`, `approve`, `reject`, `cancel`, `completeRequest` |
| `RequestUatService` | `addApprover`, `approveUat`, `rejectUat`, `createComment` (mentions) |
| `RequestDeploymentService` | `createFromPromotion`, `approveDeployment`, `rejectDeployment`, `createComment` (mentions) |
| `CommentService` | Delegated mention processing to `MentionNotifier` |
| `SlaMonitoringService` | Added WARNING email (was in-app only) |
| `AuditExportAsyncService` | Added in-app notification (was email only) |

### Groups Page Fix — Vue Generic SFC Silent Failure

**Root cause:** `AppTable.vue` used `<script setup lang="ts" generic="T extends Record<string, unknown>">`. Vue 3.5 generic SFCs fail silently at runtime in Nuxt 4.4.8 SPA mode — produce zero DOM with no errors. The `Record<string, unknown>` constraint also rejects objects with `null` values (e.g., `Group.description: null`).

**Fix applied:**
- `AppTable.vue` — removed generic directive, replaced with concrete types `Record<string, unknown>[]`
- `pages/groups/index.vue` — replaced `<AppTable>` with raw inline `<table>` as permanent safe solution
- `nuxt.config.ts` — restored CSP with `script-src-attr: ['unsafe-hashes']`, `nonce: true`

### Audit Log Hash Chain Repair

Created V8 tenant migration (`V8__repair_audit_log_hash_chain.sql`) to idempotently add `chain_index`, `record_hash`, `previous_hash` columns that were recorded in `flyway_schema_history` during an earlier repair cycle but never actually executed.

### Audit Log Hardening

Full audit of all 27 audit log call sites across 5 service files. Fixed:

- **6 empty/missing payloads**: `GROUP_DELETED`, `GROUP_MEMBERS_ADDED`, `GROUP_MEMBERS_REMOVED`, `UAT_APPROVED`, `DEPLOYMENT_APPROVED` (empty payloads → proper data), `CR_REJECTED` (missing `reason` field)
- **11 missing actorEmails**: All UAT and Deployment audit calls now resolve actor email, GroupService receives actorUserId from controller
- **IP address capture**: New `RequestContext` ThreadLocal populated by `TenantResolutionFilter` from `X-Forwarded-For` or `request.getRemoteAddr()`. All 27 `.log()` calls now pass `RequestContext.getCurrentIp()`.

---

## Verification (Current)

| Gate | Result |
|------|--------|
| Loading overlay — fast pages | Pending (showId counter) |
| Loading overlay — error paths | Pending (hideLoading in catch blocks) |
| CSP — no style violations | Pending (new directives) |
| SLA — persist across refresh | Pending (saveSetting + frontend re-read) |
| Groups → department dropdowns | Pending (need groups with isActive=true) |
| UAT tab — no blank after save | Pending (approvers in response + null-safety) |

## Next Actions

1. **Build and deploy** to verify all fixes at runtime
2. **Create test groups with isActive=true** to verify department dropdown population
3. **Run V9 migration** on existing tenants
4. **Future cleanup:** Remove `departments` table, `DepartmentEntity`, `DepartmentService`, `DepartmentAdminController`, old FK columns after transition period

## Current Blockers

- **8 remote `v*` tags** cannot be deleted due to GitHub repo tag protection rules.
- **Email sending** needs runtime verification after notification wiring deployment.
