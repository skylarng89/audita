# Audita — Code Patterns & Conventions

**Last Updated:** 2026-06-15

---

## 1. Backend Patterns (Java / Spring Boot)

### 1.1 Controller Conventions
Controllers are thin — parse request, call application service, return response. No business logic in controllers. `@PreAuthorize` on every endpoint.

### 1.2 Service Layer
Application services coordinate domain logic, persistence, and side effects (notifications, audit log). `@Transactional`, `@RequiredArgsConstructor`.

### 1.3 Async Job Naming
`_later` suffix = enqueues work. `_now` suffix = synchronous equivalent called by the job.

### 1.4 Audit Logging
Every write operation produces both `activity_stream` (CR-scoped) and `audit_log` (global) entries.

---

## 2. Frontend Patterns (Nuxt 4 / Vue 3)

### 2.1 Composable Pattern
API calls wrapped in typed composables. No raw `$fetch` in components. Import `useApi()` for direct calls when needed.

### 2.2 useAsyncData Error Handling
Always use explicit `async/try/catch` with a `loadError` ref — never `.catch(() => null)`:
```typescript
const { data, pending, refresh } = await useAsyncData("key", async () => {
    loadError.value = ""
    try {
      return await api(`/endpoint?param=${val}`)
    } catch (err) {
      loadError.value = resolveApiErrorMessage(err, "Failed.")
      return { content: [], totalElements: 0 }  // structurally valid fallback
    }
  }, { watch: [dep] })
```

### 2.3 Data Fetching — Inline URL Over query Parameter
Prefer inline URL string construction over `$fetch` `query` parameter option:
```typescript
// Preferred (proven pattern)
api(`/api/v1/groups?page=${page - 1}&size=${size}`)

// Avoid (causes issues with useApi type cast chain)
api("/api/v1/groups", { query: { page, size } })
```

### 2.4 Page Metadata
Always specify explicit `layout` in `definePageMeta`:
```typescript
definePageMeta({ layout: "default", middleware: ["auth"] })
```

### 2.5 SPA Cold-Start Loading Indicator
Static CSS spinner in `app.html` (outside Nuxt mount point), removed in `app.vue` `onMounted`. `<NuxtLoadingIndicator>` for in-app transitions.

### 2.6 App Version Display Pipeline
`ci-release.yml build-args` → `Dockerfile ARG APP_VERSION` → `ENV NUXT_PUBLIC_APP_VERSION` → `nuxt.config.ts runtimeConfig.public.appVersion` → sidebar `<p v-if="appVersion">v{{ appVersion }}</p>`.

---

## 3. Diagnostic Patterns

### 3.1 Silent Vue Rendering Failure
When a component produces zero DOM with no console errors:
1. Add visible DOM debug bar with live reactive values
2. Confirm API response in Network tab
3. Replace component with raw HTML equivalent
4. If raw HTML renders, the component is the failure point — check generic SFCs, CSP, esbuild stripping

### 3.2 Generic SFC Avoidance
Do NOT use Vue 3.5 `<script setup lang="ts" generic="T">` in Nuxt 4 SPA mode. Use concrete types `Record<string, unknown>[]` instead. Generic SFCs fail silently at runtime.

---

## 4. Notification System Patterns

### 4.1 Shared MentionNotifier
```java
@Service
public class MentionNotifier {
    public Set<UserEntity> processMentions(String body, UUID commenterId,
                                            String resourceTitle, String resourceLink)
}
```

### 4.2 Notification Wiring
Every state-changing operation in CR/UAT/Deployment services follows:
1. Load entity + validate state
2. Apply domain mutation + persist
3. Write activity_stream + audit_log entries
4. `notificationService.createAndPush(recipientId, type, title, body, link)` — in-app
5. `emailService.sendXxxEmail(recipientEmail, ...)` — email (fires in-app notification types plus email for all events)

---

## 5. Audit Log Patterns

### 5.1 Audit Log Call Site Requirements
Every `auditLogService.log()` call must include:
- `actionType` — descriptive constant (e.g., `CR_APPROVED`, `GROUP_MEMBERS_ADDED`)
- `entityType` — lowercase snake_case (e.g., `change_request`, `group`)
- `entityId` — UUID of the affected entity
- `actorId` — UUID of the acting user (never null if known)
- `actorEmail` — resolved via `resolveActorEmail()` or entity reference
- `payload` — meaningful `Map<String, Object>` with relevant fields (never null, never empty)
- `ipAddress` — `RequestContext.getCurrentIp()`

### 5.2 IP Address Capture
Client IP is captured in `TenantResolutionFilter` from `X-Forwarded-For` (first proxy IP) with `request.getRemoteAddr()` fallback. All services use `RequestContext.getCurrentIp()`. The ThreadLocal is cleared in the filter's finally block alongside `TenantContext.clear()`.

### 5.3 Actor Email Resolution
Services with `userRepository` injected should use:
```java
private String resolveActorEmail(UUID actorUserId) {
    if (actorUserId == null) return null;
    return userRepository.findById(actorUserId).map(UserEntity::getEmail).orElse(null);
}
```
This pattern is used in `ChangeRequestService`, `RequestUatService`, `RequestDeploymentService`, and `GroupService`.
