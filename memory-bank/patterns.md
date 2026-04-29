# Audita — Code Patterns & Conventions

**Last Updated:** 2026-04-23

---

## 1. Backend Patterns (Java / Spring Boot)

### 1.1 Controller Conventions

Controllers are thin — they parse the request, call the application service, and return the response. No business logic in controllers.

```java
@RestController
@RequestMapping("/api/v1/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    @PostMapping
    @PreAuthorize("hasPermission(null, 'cr.create')")
    public ResponseEntity<ChangeRequestResponse> create(
            @Valid @RequestBody CreateChangeRequestRequest request,
            @CurrentUser UserPrincipal actor) {
        var cr = changeRequestService.create(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(cr);
    }
}
```

### 1.2 Service Layer (Application Use Cases)

Application services coordinate domain logic, persistence, and side effects (notifications, audit log).

```java
@Service
@Transactional
@RequiredArgsConstructor
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public ChangeRequestResponse submit(UUID crId, UserPrincipal actor) {
        var cr = repository.findByIdOrThrow(crId);
        cr.submit();  // Domain object owns the state transition logic
        repository.save(cr);
        auditLogService.log(CR_SUBMITTED, cr.getId(), actor);
        notificationService.notifyApproversLater(cr);
        return ChangeRequestResponse.from(cr);
    }
}
```

### 1.3 Domain Entity — State Machine

Business rules live on the domain entity, not in services or controllers.

```java
public class ChangeRequest {
    // State machine: only submit() knows what DRAFT → PENDING_APPROVAL requires
    public void submit() {
        if (this.status != Status.DRAFT) {
            throw new InvalidStateTransitionException("Cannot submit a non-draft CR");
        }
        this.status = Status.PENDING_APPROVAL;
    }
}
```

### 1.4 Error Handling — RFC 7807 Problem Detail

All error responses follow RFC 7807.

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Resource Not Found");
        return detail;
    }
}
```

### 1.5 Tenant Context

Every request resolves the tenant schema before hitting the DB.

```java
// In a Spring filter / interceptor
TenantContext.setCurrentTenant(resolvedTenantSlug);
// Hibernate CurrentTenantIdentifierResolver reads this thread-local
```

### 1.6 Audit Logging Pattern

Every state-changing operation logs to both `activity_stream` (CR-scoped) and `audit_log` (global).

```java
auditLogService.log(
    ActionType.CR_SUBMITTED,
    EntityType.CHANGE_REQUEST,
    cr.getId(),
    actor,
    payload  // before/after JSONB
);
```

### 1.7 Notification Dispatch (Async)

Notifications are dispatched asynchronously to avoid blocking the request thread.

```java
// Naming convention: _later suffix = enqueues work
notificationService.notifyApproversLater(cr);

// The synchronous method called by the async executor:
// notifyApproversNow(cr)
```

### 1.8 Method Visibility (Kotlin DSL Gradle)

Private helper methods follow the ordering: public methods first, private helpers below.

---

## 2. Frontend Patterns (Nuxt 3 / Vue 3 / TypeScript)

### 2.1 Composable Pattern

API calls are wrapped in typed composables. No raw `$fetch` in components.

```typescript
// composables/useChangeRequests.ts
export function useChangeRequests() {
  const { $api } = useNuxtApp();

  async function submit(id: string): Promise<ChangeRequest> {
    return $api<ChangeRequest>(`/change-requests/${id}/submit`, {
      method: "POST",
    });
  }

  return { submit };
}
```

### 2.2 Pinia Store Structure

Stores own loading/error state alongside data.

```typescript
// stores/auth.ts
export const useAuthStore = defineStore("auth", () => {
  const user = ref<User | null>(null);
  const isLoading = ref(false);

  async function login(email: string, password: string) {
    isLoading.value = true;
    try {
      user.value = await $api("/auth/login", {
        method: "POST",
        body: { email, password },
      });
    } finally {
      isLoading.value = false;
    }
  }

  return { user, isLoading, login };
});
```

### 2.3 Route Guards (Middleware)

```typescript
// middleware/role.ts
export default defineNuxtRouteMiddleware((to) => {
  const auth = useAuthStore();
  const requiredRole = to.meta.requiredRole as string | undefined;

  if (requiredRole && auth.user?.role !== requiredRole) {
    return navigateTo("/dashboard");
  }
});
```

### 2.4 API Plugin — Auth Header Injection

```typescript
// plugins/api.ts
export default defineNuxtPlugin(() => {
  const auth = useAuthStore();

  const $api = $fetch.create({
    baseURL: useRuntimeConfig().public.apiBase,
    onRequest({ options }) {
      if (auth.accessToken) {
        options.headers = {
          ...options.headers,
          Authorization: `Bearer ${auth.accessToken}`,
        };
      }
    },
    onResponseError({ response }) {
      if (response.status === 401) {
        auth.logout();
      }
    },
  });

  return { provide: { api: $api } };
});
```

### 2.5 TipTap Mention Extension

User mention queries the backend for active users in the tenant.

```typescript
Mention.configure({
  suggestion: {
    items: async ({ query }) => {
      return $api<User[]>("/users", {
        query: { search: query, status: "ACTIVE" },
      });
    },
  },
});
```

---

## 3. Naming Conventions

### Backend

| Type             | Convention                | Example                           |
| ---------------- | ------------------------- | --------------------------------- |
| Packages         | `lowercase.dot.separated` | `io.audita.domain.cr`             |
| Classes          | `PascalCase`              | `ChangeRequestService`            |
| Methods          | `camelCase`               | `submitChangeRequest`             |
| Constants        | `UPPER_SNAKE_CASE`        | `MAX_FILE_SIZE_MB`                |
| Enums            | `UPPER_SNAKE_CASE` values | `Status.PENDING_APPROVAL`         |
| Async methods    | `_later` suffix           | `notifyApproversLater`            |
| Sync equivalents | `_now` suffix             | `notifyApproversNow`              |
| DB tables        | `snake_case`              | `change_requests`, `cr_approvers` |
| DB columns       | `snake_case`              | `created_at`, `is_required`       |

### Frontend

| Type               | Convention                       | Example                          |
| ------------------ | -------------------------------- | -------------------------------- |
| Components         | `PascalCase`                     | `ChangeRequestCard.vue`          |
| Composables        | `camelCase` with `use` prefix    | `useChangeRequests.ts`           |
| Stores             | `camelCase` with `use` prefix    | `useAuthStore`                   |
| Pages/routes       | `kebab-case`                     | `change-requests/[id].vue`       |
| Types/Interfaces   | `PascalCase`                     | `ChangeRequest`, `UserPrincipal` |
| API response types | `PascalCase` + `Response` suffix | `ChangeRequestResponse`          |

---

## 4. Testing Patterns

### Backend

- Unit test domain entities and application services with Mockito.
- Integration test REST endpoints with Spring Boot Test + MockMvc + TestContainers (PostgreSQL).
- Name tests by behaviour: `should_reject_when_only_required_approver_rejects`.
- Test coverage target: ≥80% on domain and application layers; ≥70% on infrastructure.

### Frontend

- Unit test composables and Pinia stores with Vitest.
- Component tests with `@nuxt/test-utils`.
- E2E tests with Playwright for critical user journeys (login, create CR, approve CR).

---

## 5. Security Patterns

- Never log passwords, tokens, or PII. Use structured logging with field masking.
- All rich-text HTML sanitised server-side before storage. Never render unsanitised HTML on the frontend.
- File uploads: validate MIME type server-side (not just file extension), sanitise filename, reject path traversal attempts.
- `@PreAuthorize` on every controller method — fail-secure by default.
- Rate limiting enforced at the filter layer before controller execution.

---

## 6. Activity Stream / Audit Log Pattern

Every write operation in the application layer must produce an audit entry. Pattern:

```
1. Load entity (with SELECT FOR UPDATE if concurrent modification is possible)
2. Capture before-state
3. Apply domain mutation
4. Persist entity
5. Write activity_stream entry (CR-scoped)
6. Write audit_log entry (global)
7. Dispatch notifications (async)
```

Never skip steps 5–6. Tested via integration tests that assert audit entries are created.

---

## Layer 1 E2E Test Patterns (Testcontainers)

### Void-return endpoints

`deactivate`, `reactivate`, `addMember`, `deleteGroup`, `cancel` all return `void` from the controller. Spring defaults to HTTP 200 with no body. Assertions must use `isIn(200, 204)` — never check the response body.

### `notifications/read-all`

Returns HTTP 204 (No Content), not 200. Always assert `isIn(200, 204)`.

### SSE stream endpoint

`GET /api/v1/notifications/stream` is a long-lived SSE connection. **Never connect to it from tests** — `HttpClient.send()` with `BodyHandlers.discarding()` will block indefinitely. Only test token issuance (`POST /stream-token`) and verify the returned JWT is non-blank.

### SSE stream query param

The stream endpoint uses `?streamToken=xxx` (not `?token=xxx`) — matches the `@RequestParam` name in `NotificationController`.

### StreamTokenResponse field name

`POST /api/v1/notifications/stream-token` returns `{"streamToken":"..."}` — the JSON key is `streamToken`, not `token`.

### Invite token injection

Raw tokens are never stored in DB — only SHA-256 Base64 hash. Inject known tokens via SQL:

```sql
INSERT INTO "{slug}".invite_tokens (id, user_id, token_hash, expires_at, used)
VALUES (gen_random_uuid(), ?::uuid, ?, NOW() + INTERVAL '48 hours', false)
```

Use `Base64.getEncoder().encodeToString(sha256bytes)` to compute the hash (NOT hex encoding).

### Role normalization

After `POST /api/platform/v1/tenants` (provision), roles are seeded with mixed-case names. Spring Security's `hasAnyRole('ADMIN')` requires UPPERCASE. Always call `normalizeRoleNames(slug)` (UPDATE roles SET name = UPPER(name)) before accepting invites.

### Password complexity requirement

All user passwords must be 12+ chars with uppercase, lowercase, digit, and symbol. Example: `"Admin@Acme1!Pass"`.

### Test storage path

Set `audita.storage.local.base-path=/tmp/audita-test-uploads` in `@SpringBootTest(properties=...)` and `mkdir -p /tmp/audita-test-uploads` to allow attachment upload tests to write files.

---

## UI Consistency Pattern (2026-04-29)

- Treat button styling as two layers:
  - Variant (`btn-primary`, `btn-ghost`, `btn-danger`, `btn-secondary`)
  - Size (`btn-sm`, `btn-md`, `btn-lg`)
- To prevent broken visuals when size classes are missed, variant classes include a default base size fallback in global CSS.
- Preferred explicit usage in templates remains `variant + size` (for example `btn-primary btn-md`) for readability and deterministic design intent.

## CR Read Mapping Pattern (2026-04-29)

- If API DTO mapping touches lazy relations (`createdBy`, `actor`, etc.), initialize those relations within service read methods while transaction/session is active.
- For `ChangeRequestService`, read endpoints now initialize creator relation before returning entities used by API response mappers.

## Nuxt Shared Component Naming Pattern (2026-04-29)

- Components under `components/shared/` must be referenced with Nuxt auto-import naming prefix `Shared` in layouts/pages.
- Example mappings:
  - `AppSidebar.vue` → `SharedAppSidebar`
  - `AppUserMenu.vue` → `SharedAppUserMenu`
  - `AppNotificationBell.vue` → `SharedAppNotificationBell`
  - `AppToastContainer.vue` → `SharedAppToastContainer`
