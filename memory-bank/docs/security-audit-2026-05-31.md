# Audita — Security & Architecture Audit Report (Revised)

**Date:** 2026-05-31  
**Auditor:** Principal Architect (automated deep-scan)  
**Reviewed by:** Claude (severity re-rating and additional risk notes, 2026-05-31)  
**Scope:** Full codebase — `audita-api` (Java 25 / Spring Boot 4), `audita-web` (Nuxt 3 / Vue 3), infrastructure, CI/CD  
**Findings:** 52 unique (1 Critical, 13 High, 26 Medium, 12 Low)

> **Review note:** Three severity ratings have been adjusted and two additional risk callouts added versus the original audit. Changes are marked **[revised]**.

---

## 1. Security Vulnerabilities

### [Critical] Setup Endpoint Unauthenticated — First-Run Hijack

**Location:** `audita-api/api/.../PlatformBootstrapController.java:75-95`, `SecurityConfig.java:77`  
**Issue:** `/api/platform/v1/setup` is `permitAll` with no setup-token guard (unlike `/bootstrap`). Before the first tenant exists, anyone who discovers the API URL can create the initial organisation and admin account, hijacking the platform.  
**Root cause:** Setup endpoint designed for single-tenant first-run convenience but lacks the token protection applied to bootstrap.  
**Recommended fix:** Require the `X-Setup-Token` header on `/setup`, same as `/bootstrap`. Alternatively, auto-generate a one-time setup token at first boot and log it to stdout.

---

### [Critical] XSS via `v-html` Without HTML Sanitization

**Location:** `audita-web/pages/change-requests/[id].vue:308,832`, `composables/richText.ts:37-57`  
**Issue:** Two `v-html` bindings render rich-text content (CR descriptions and comments). `normalizeRichTextHtml()` only modifies `<a>` tag attributes — it does not strip `<script>`, `<iframe>`, event handlers, `javascript:` URIs, etc. The SSR fallback regex path interpolates `href` values without escaping, enabling attribute injection.  
**Root cause:** No HTML sanitization library (DOMPurify) used. TipTap's `getHTML()` output is trusted but API manipulation bypasses the editor.  
**Recommended fix:** Integrate DOMPurify (`dompurify` + `jsdom` for SSR) with a strict config allowing only TipTap's output elements. Apply on both client render and before API submission.

> **Review note:** This XSS vector must be considered in conjunction with the access token stored in Pinia reactive state (see Section 5). The combined attack chain is: XSS → `useAuthStore().accessToken` → full account takeover. Both findings must be remediated together; fixing one without the other leaves the chain open.

---

### [Critical] Open Redirect via Unvalidated `redirect` Query Parameter

**Location:** `audita-web/pages/auth/sign-in.vue:194-195`, `composables/useAuth.ts:20-21`, `middleware/auth.ts:20`  
**Issue:** `route.query.redirect` is passed directly to `navigateTo()` without validation. An attacker crafts `/auth/sign-in?redirect=https://evil.com` — after login, the user is redirected to the attacker's site.  
**Root cause:** No same-origin or relative-path validation on the redirect target. Nuxt's `navigateTo` accepts external URLs.  
**Recommended fix:** Validate that `redirect` starts with `/` and does not start with `//`. Reject or ignore external URLs. Implement a `isSafeRedirect()` utility shared across all auth flows.

---

### [High] Super Admin Login Has No Rate Limiting

**Location:** `audita-api/infrastructure/.../AuthService.java:118-130`  
**Issue:** `loginSuperAdmin()` does not call `enforceRateLimit()`, unlike `loginTenantUser()` which enforces 5 attempts / 15 min. The highest-privilege account is exposed to unlimited brute-force.  
**Root cause:** Rate limiting added to tenant login path only; super admin path developed independently.  
**Recommended fix:** Add `enforceRateLimit("sa-login:" + clientIp + ":" + email, 5, Duration.ofMinutes(15))` at the top of `loginSuperAdmin()`. Extend method signature to accept `clientIp`.

---

### [High] JWT Access Tokens Not Revocable

**Location:** `audita-api/infrastructure/.../JwtService.java` (entire file), `api/.../JwtAuthenticationFilter.java:57-61`  
**Issue:** No mechanism to revoke JWT access tokens before their 15-minute expiry. When a user is suspended, deactivated, or has roles changed, the existing JWT remains valid. During this window, the user retains their previous access level.  
**Root cause:** Stateless JWT validation — `isValid()` checks only signature + expiry, not user status.  
**Recommended fix:** Add a `tokenVersion` claim to JWT. Store current version per-user in DB. Validate version in `JwtAuthenticationFilter` (cache with Caffeine, 60s TTL). Increment version on role change, suspension, or deactivation.

---

### [High] SSO State and Exchange Code Stores Not Distributed

**Location:** `audita-api/infrastructure/.../SsoService.java:55-58`  
**Issue:** OAuth2 state parameters (`pendingStates`) and frontend exchange codes (`pendingExchanges`) stored in `ConcurrentHashMap`. In multi-instance deployment, callback can hit different instance than initiator, causing legitimate SSO failures. Exchange codes can be replayed on a different instance.  
**Root cause:** In-memory stores chosen for simplicity; no distributed backing.  
**Recommended fix:** Replace with Redis (with TTL) or a shared database table. Alternatively, use sticky sessions at the load balancer for the OAuth callback path.

---

### [High] `@Async` Self-Invocation Silently Fails — Audit Export Runs Synchronously

**Location:** `audita-api/infrastructure/.../AuditExportService.java:84,108-109`  
**Issue:** `queueExport()` calls `generateAsync()` on `this` (self-invocation). Spring's proxy-based AOP cannot intercept self-calls, so `@Async` is silently ignored. The export runs synchronously, blocking the HTTP thread. Additionally, `TenantContext.setCurrentTenant()` at line 110 overwrites the caller's context, and the `finally` block clears it — corrupting the request's tenant scoping.  
**Root cause:** Spring `@Async` requires proxy invocation. Self-invocation bypasses the proxy entirely.  
**Recommended fix:** Extract `generateAsync` into a separate `@Service` bean, or inject the bean's own proxy via `@Lazy` self-injection. Consider an explicit `TaskExecutor` or message queue for export jobs.

> **Additional risk note:** A large export blocking the HTTP thread pool under concurrent load is also a denial-of-service vector, not merely a performance issue. This elevates urgency beyond the async correctness fix.

---

### [High] Idempotency Key Check-Then-Act Not Atomic

**Location:** `audita-api/infrastructure/.../IdempotencyService.java:31-42,44-62`, `api/.../ChangeRequestController.java:64-83`  
**Issue:** The flow is: (1) check if key exists, (2) create resource, (3) record key. Between steps 1 and 3, a concurrent request with the same key can pass the check and create a duplicate resource. The unique constraint prevents duplicate keys but not duplicate resources.  
**Root cause:** Check-then-act pattern without atomic locking. Key recorded _after_ resource creation.  
**Recommended fix:** Use `INSERT ... ON CONFLICT DO NOTHING` with `RETURNING` to atomically claim the idempotency key before creating the resource. Wrap in a single transaction: insert key → create resource → commit. On conflict, return the existing resource ID.

---

### [High] SSO JIT-Provisioned Users Bypass Domain Whitelist

**Location:** `audita-api/infrastructure/.../SsoService.java:272-304`  
**Issue:** `resolveOrProvisionUser` JIT-provisions users from SSO without checking the tenant's `TenantAllowedDomain`. The regular login path (`AuthService.checkDomainWhitelist`) enforces this. An attacker with a Google/Microsoft account whose domain is not whitelisted can gain access via SSO.  
**Root cause:** Domain whitelist check only in password-based login path, not SSO callback.  
**Recommended fix:** Add domain whitelist validation in `resolveOrProvisionUser` before provisioning. Reject SSO login if the email domain is not in the tenant's allowed domains.

---

### [High] CSRF Protection Disabled With Cookie-Scoped Refresh Token ~~[High]~~ **[revised: Medium]**

**Location:** `audita-api/api/.../SecurityConfig.java:53`, `audita-web/nuxt.config.ts:20`  
**Issue:** CSRF disabled globally on both backend and frontend. The refresh token is delivered as an HttpOnly cookie scoped to `/api/v1/auth`. The `/api/v1/auth/refresh` and `/api/v1/auth/session` endpoints read from this cookie. `SameSite=Strict` is already in place and provides strong cross-site submission mitigation.  
**Root cause:** Pragmatic decision to simplify API at the cost of one defense layer.  
**Revised severity rationale:** With `SameSite=Strict` enforced, the residual CSRF risk is low. High is disproportionate. Reclassified to Medium as defense-in-depth gap.  
**Recommended fix:** Enable CSRF for cookie-scoped auth endpoints only, using a custom `CsrfTokenRequestHandler` that exempts bearer-token-authenticated endpoints.

---

### [High] `JPA_DDL_AUTO=update` — Dangerous Schema Management in Production

**Location:** `.env:39,48`, `.env.example:39,48`  
**Issue:** `JPA_DDL_AUTO=update` tells Hibernate to automatically modify the database schema. In production, this can silently drop columns, alter types, or destroy data — bypassing Flyway migration management which is also configured.  
**Root cause:** Development convenience setting left as default for all environments.  
**Recommended fix:** Set `JPA_DDL_AUTO=validate` for production. Use `update` only in local dev profiles. Add startup validation that rejects `update` when `SPRING_PROFILES_ACTIVE` is not `dev`.

---

### [High] Unpinned Docker Image Tag (`:latest`) in Production Compose

**Location:** `docker-compose.yml:50`  
**Issue:** `image: skylarng89/audita-api:latest` — mutable tag means deployments are non-reproducible, a compromised Docker Hub account could push malicious images, and rollback to a known-good version is impossible.  
**Root cause:** Compose file written for convenience, not production deployment.  
**Recommended fix:** Pin to version tag or SHA digest: `skylarng89/audita-api:v0.6.0` or `@sha256:...`.

---

### [High] PostgreSQL Port Exposed to Host Network

**Location:** `docker-compose.yml:15`  
**Issue:** `ports: "7432:5432"` binds the database to all host interfaces (`0.0.0.0`). Combined with default `postgres/postgres` credentials, the database is accessible from any network.  
**Root cause:** Port mapping added for developer convenience without restricting to localhost.  
**Recommended fix:** Bind to localhost only: `"127.0.0.1:7432:5432"`, or remove port mapping entirely.

---

### [High] Database Healthcheck Connects to Wrong Host

**Location:** `docker-compose.yml:17-24`  
**Issue:** Healthcheck uses `${DB_HOST}` and `${DB_PORT}` which resolve to an external PostgreSQL instance (`10.0.80.7:7010`), not the container's own database. Reports healthy when container DB is down; reports unhealthy when external DB is unreachable.  
**Root cause:** Healthcheck reuses application-level env vars pointing to external DB.  
**Recommended fix:** Change to `-h localhost -p 5432` to healthcheck the container's own PostgreSQL process.

---

## 2. Architectural Weaknesses

### [Medium] Hexagonal Architecture Violation — API Directly Depends on Infrastructure

**Location:** `audita-api/api/build.gradle.kts:19`  
**Issue:** `api` module has `implementation(project(":infrastructure"))`. Controllers directly import infrastructure service classes instead of application-layer ports. The `application` module defines only 8 port interfaces while 16 service classes live in `infrastructure`.  
**Root cause:** Port/adapter pattern started but not completed. Most business services implemented directly in infrastructure.  
**Recommended fix:** Define port interfaces in `application` for each infrastructure service. Controllers depend on ports only. Infrastructure implements ports. Alternatively, accept the pragmatic architecture and rename the module.

---

### [Medium] Business Logic Concentrated in Infrastructure Module

**Location:** `audita-api/infrastructure/.../service/*` (16 service classes)  
**Issue:** `AuthService` (420 lines), `ChangeRequestService` (1062 lines), `TenantService` (494 lines), `SsoService` (363 lines) — all in infrastructure. The `application` module contains only port interfaces with no implementations. This inverts the hexagonal architecture.  
**Root cause:** Organic growth without architectural enforcement.  
**Recommended fix:** Move service implementations to `application` module, depending on infrastructure only through port interfaces. Or formally adopt "infrastructure-as-application" and document the decision.

---

### [High] Audit Log Not Immutable at Database Level ~~[Medium]~~ **[revised: High]**

**Location:** `audita-api/infrastructure/.../db/migration/tenant/V1__create_tenant_schema.sql:233-243`  
**Issue:** `audit_log` table has no DB-level triggers or rules preventing UPDATE/DELETE. Immutability enforced only at application layer (no setters on entity). A compromised application or SQL injection could modify or delete audit records.  
**Root cause:** Application-layer enforcement without database-layer backing.  
**Revised severity rationale:** For an audit-trail product, the trustworthiness of audit records is the core value proposition. Application-layer-only immutability is insufficient — any SQL injection, compromised service account, or direct DB access can silently rewrite history. This warrants High.  
**Recommended fix:** Add `REVOKE UPDATE, DELETE ON audit_log FROM <app_role>`. Create a trigger that raises an exception on UPDATE/DELETE attempts. Add row-level security policies if needed.

---

### [Medium] Audit Log Transaction Propagation — Coupled to Caller

**Location:** `audita-api/infrastructure/.../AuditLogService.java:47`  
**Issue:** `@Transactional` on `log()` uses default propagation (`REQUIRED`), joining the caller's transaction. If the caller rolls back, the audit entry is also rolled back. For a true audit trail, writes should persist independently.  
**Root cause:** Default transaction propagation.  
**Recommended fix:** Use `@Transactional(propagation = Propagation.REQUIRES_NEW)` so audit writes commit independently.

> **Caution:** `REQUIRES_NEW` ensures audit entries persist even when the parent transaction rolls back — but this means an event that ultimately did not occur (e.g. a failed change request update) will still appear in the audit log. Implement a status or outcome field on audit entries so failed operations are recorded accurately rather than misleadingly.

---

### [Medium] SSO Callback Sets TenantContext Without Cleanup in Error Path

**Location:** `audita-api/infrastructure/.../SsoService.java:143`  
**Issue:** `handleCallback` sets `TenantContext.setCurrentTenant(tenantSlug)` without a `finally { TenantContext.clear(); }`. If the method throws after setting context, the TenantContext leaks to the thread pool, potentially causing subsequent requests to operate in the wrong tenant schema.  
**Root cause:** Missing finally block. The `TenantResolutionFilter` clears context for HTTP requests, but the service method itself is unsafe.  
**Recommended fix:** Wrap in try/finally with `TenantContext.clear()`. Better: use a `TaskDecorator` pattern that propagates and clears tenant context automatically.

---

### [Medium] TenantContext Not Restored After Provision

**Location:** `audita-api/infrastructure/.../TenantService.java:249-276`  
**Issue:** `provision()` sets TenantContext and clears it in finally, but does not restore the previous value. If called from a context where a tenant is already active, the original context is lost.  
**Root cause:** `TenantContext.clear()` instead of save/restore pattern.  
**Recommended fix:** Save previous context before setting, restore in finally: `String prev = TenantContext.getCurrentTenant(); try { ... } finally { TenantContext.setCurrentTenant(prev); }`.

---

### [Medium] Forgot-Password and Reset-Password Lack Tenant Context Validation

**Location:** `audita-api/api/.../AuthController.java:127-140`, `infrastructure/.../AuthService.java:170-208`  
**Issue:** Password reset flow does not require or validate tenant slug. `userRepository.findByEmail()` executes against whatever schema is active. Without tenant context, query runs against `public` schema (no users table). With leaked context, could target wrong tenant.  
**Root cause:** Password reset not designed with multi-tenant schema isolation in mind.  
**Recommended fix:** Require `X-Tenant-Slug` header on all password reset endpoints. Validate tenant exists and is active before processing.

---

### [Medium] Tenant Status Not Validated in Authentication Filter

**Location:** `audita-api/api/.../JwtAuthenticationFilter.java:69-111`  
**Issue:** JWT filter extracts `tenantSlug` and checks mismatch, but never validates tenant is still `ACTIVE`. A suspended tenant's JWT tokens continue to work until expiry.  
**Root cause:** No tenant status check in authentication filter chain.  
**Recommended fix:** Add tenant status validation in `JwtAuthenticationFilter`. Cache active tenant status with short TTL (Caffeine, 60s) to avoid DB hit per request.

---

### [Medium] CSV Injection in Audit Trail Export

**Location:** `audita-api/api/.../AuditTrailController.java:83-98`, `infrastructure/.../AuditExportService.java:164-181`  
**Issue:** CSV export escapes commas/quotes/newlines but does not neutralize formula injection characters (`=`, `+`, `-`, `@`, `\t`, `\r`). A CR title like `=CMD|'/C calc'!A0` achieves RCE when CSV opened in Excel.  
**Root cause:** CSV output does not prefix dangerous characters.  
**Recommended fix:** Prefix any field starting with `=`, `+`, `-`, `@`, `\t`, or `\r` with a single quote (`'`). Apply to all user-controlled fields in CSV output.

---

### [Medium] Audit Export Download Endpoint Unauthenticated

**Location:** `audita-api/api/.../AuditTrailController.java:138-151`, `SecurityConfig.java:82`  
**Issue:** `/api/v1/audit-trail/exports/download/{token}` is `permitAll`. Access controlled solely by the download token (32-byte random). Token sent via email (unencrypted in transit) and stored in DB. If leaked, anyone downloads the audit export without authentication.  
**Root cause:** Token-based access without authentication layer.  
**Recommended fix:** Require bearer token authentication in addition to the download token. Or require the download token to be presented as a header rather than in the URL path.

---

## 3. Performance Bottlenecks

### [Medium] N+1 Queries Across Multiple Entity Types

**Location:**

- `RoleEntity.java:24` — `permissions` EAGER fetch (3 extra queries per user)
- `UserService.java:66-69` — `Hibernate.initialize()` per user in list (40 queries per page)
- `ChangeRequestService.java:229-232` — `initializeCreator()` per CR (20 queries per page)
- `CommentService.java:114-127` — `initializeAuthor()` cascading lazy loads (150 queries for 50 comments)
- `ActivityStreamRepository.java:14` — no `@EntityGraph` on activity list

**Issue:** Manual `Hibernate.initialize()` calls in loops instead of batch fetching. Each entity triggers separate SQL queries for lazy associations.  
**Root cause:** Lazy-loaded associations initialized one-by-one without batch-fetching strategy.  
**Recommended fix:** Use `@EntityGraph(attributePaths = {...})` on repository methods. Add `@BatchSize(size = 20)` on lazy collections. Change `RoleEntity.permissions` to `FetchType.LAZY` with explicit `JOIN FETCH` where needed.

---

### [Medium] Unbounded List Queries — No Pagination

**Location:** `ActivityStreamRepository.java:14`, `CommentRepository.java:16`, `ChangeRequestRepository.java:56-67`  
**Issue:** Activity stream, comments, and SLA breach queries return unbounded `List<>` without pagination. For CRs with hundreds of entries, entire result set loaded into memory.  
**Root cause:** Repository methods return `List<>` instead of `Page<>` with `Pageable`.  
**Recommended fix:** Add `Pageable` parameter to repository methods. Implement server-side pagination for activity stream and comments. For SLA queries, add reasonable limits.

---

### [Medium] Synchronous CSV Export Loads All Audit Entries Into Memory

**Location:** `audita-api/api/.../AuditTrailController.java:79-98`, `infrastructure/.../AuditLogService.java:77-89`  
**Issue:** Synchronous CSV export and async export both load all matching audit entries into a `List<>`, then build entire CSV in memory. For large date ranges, causes OOM.  
**Root cause:** No streaming or chunked processing.  
**Recommended fix:** Stream results using `StreamingResponseBody` with paginated DB queries. Deprecate synchronous endpoint and direct users to async export flow. Use `ResponseBodyEmitter` for chunked CSV output.

---

### [Medium] Audit Export Download Reads Entire File Into Memory

**Location:** `audita-api/api/.../AuditTrailController.java:146`  
**Issue:** `Files.readAllBytes(filePath)` loads entire export file into memory. For large exports, causes `OutOfMemoryError` — also a DoS vector.  
**Root cause:** Simple implementation not accounting for file size.  
**Recommended fix:** Stream using `InputStreamResource` with `Files.newInputStream(filePath)`, matching the attachment download pattern in `ChangeRequestController`.

---

### [Medium] Tenant Resolution Filter Opens Raw JDBC Connection Per Request

**Location:** `audita-api/api/.../TenantResolutionFilter.java:160-171,178-190`  
**Issue:** `findSlugBySubdomain()` and `tenantExists()` each open a new JDBC connection from the pool. Every HTTP request triggers 1-2 raw JDBC connections before business logic. Under high load, can exhaust HikariCP pool (max 20).  
**Root cause:** Raw JDBC used instead of cached repository lookup.  
**Recommended fix:** Cache tenant slug-to-subdomain mappings in Caffeine with 60s TTL. Eliminates DB query for majority of requests.

---

### [Medium] SSE Reconnection Without Exponential Backoff

**Location:** `audita-web/plugins/sse.client.ts:63-74`  
**Issue:** SSE reconnects after fixed 5-second delay with no backoff, jitter, or retry limit. If backend is down, client hammers it every 5 seconds indefinitely.  
**Root cause:** Simple reconnection logic without resilience patterns.  
**Recommended fix:** Implement exponential backoff with jitter: initial 1s, max 60s, factor 2, random jitter ±500ms. Add max retry limit with user notification.

---

### [Medium] Event Listener Leaks in Multiple Components

**Location:**

- `pages/change-requests/new.vue:562-569`
- `components/shared/AppNotificationBell.vue:111-115`
- `components/shared/AppUserMenu.vue:100-104`
- `plugins/sse.client.ts:89-97`
- `plugins/auth-sync.client.ts:36-50`

**Issue:** `document.addEventListener` registered in `onMounted` but never removed in `onUnmounted`. In SPA navigation, leaked listeners accumulate, each holding closure references to component instances.  
**Root cause:** Inconsistent cleanup discipline.  
**Recommended fix:** Add `onUnmounted(() => document.removeEventListener(...))` for every `addEventListener` in `onMounted`. Create a `useEventListener` composable that auto-cleans.

---

## 4. Dependency & Infrastructure Risks

### [Medium] Internal Network Topology Leaked in `.env.example`

**Location:** `.env.example:18-20`  
**Issue:** Committed to git with real private IP (`10.0.80.7`), non-standard port (`7010`), and full JDBC connection string. Leaks infrastructure details to anyone with repository access.  
**Root cause:** Example file derived from real `.env` without sanitizing infrastructure values.  
**Recommended fix:** Replace with placeholders: `DB_HOST=localhost`, `DB_PORT=5432`, `DB_URL=jdbc:postgresql://localhost:5432/audita`.

---

### [Medium] `AUTH_COOKIE_SECURE=false` as Default

**Location:** `.env:49`, `.env.example:49`  
**Issue:** Auth cookies sent over HTTP (unencrypted). If carried to production, cookies vulnerable to MITM interception.  
**Root cause:** Correct for local dev but no environment-specific override.  
**Recommended fix:** Add startup validation that rejects `false` when profile is not `dev`. Default to `true` in production Helm values.

---

### [Medium] `SPRING_PROFILES_ACTIVE=dev` as Default

**Location:** `.env:50`, `.env.example:50`  
**Issue:** Dev profile is the default. Production deployment forgetting to override this activates dev-mode features (verbose errors, relaxed security).  
**Root cause:** Dev convenience default.  
**Recommended fix:** Default to `prod` or require explicit profile selection at startup (fail if not set).

---

### [Medium] GitHub Actions Not SHA-Pinned

**Location:** `.github/workflows/ci-release.yml` (30+ action references)  
**Issue:** All actions referenced by mutable version tags (`@v6`, `@v4`, etc.). Compromised maintainer account could move tag to malicious code.  
**Root cause:** Using version tags is conventional but not supply-chain safe.  
**Recommended fix:** Pin all actions to full commit SHAs with version comment. Use Dependabot to keep SHAs updated.

---

### [Medium] API and Web Services Have No Healthchecks

**Location:** `docker-compose.yml:49-118` (api), `121-155` (web)  
**Issue:** Neither service defines a healthcheck. Web depends on API without `condition: service_healthy`, starting before Spring Boot is ready.  
**Root cause:** Healthchecks only on database service.  
**Recommended fix:** Add healthchecks: API via `/actuator/health`, web via HTTP check. Update `depends_on` to use `condition: service_healthy`.

---

### [Medium] `Dockerfile.dev` pnpm Version Mismatch and Missing `--ignore-scripts`

**Location:** `audita-web/Dockerfile:7` (pnpm@11.2.2), `Dockerfile.dev:9` (pnpm@10.11.0)  
**Issue:** Different pnpm versions between dev and production cause dependency resolution divergence. Dev Dockerfile missing `--ignore-scripts`, allowing arbitrary postinstall scripts.  
**Root cause:** Dev Dockerfile not updated when production was revised.  
**Recommended fix:** Align pnpm versions. Add `--ignore-scripts` to dev Dockerfile.

---

## 5. Security — Medium Severity

### [Medium] Exception Handler Leaks Internal Details

**Location:** `audita-api/api/.../GlobalExceptionHandler.java:137-146`  
**Issue:** `HttpMessageNotReadableException` handler returns `ex.getMostSpecificCause().getMessage()` — Jackson errors expose class names, field types, module structure.  
**Root cause:** Handler designed to be helpful but exposes implementation details.  
**Recommended fix:** Return generic "Malformed request body" for `HttpMessageNotReadableException`. Log full cause server-side.

---

### [Medium] X-Forwarded-For IP Spoofing for Rate Limit Bypass

**Location:** `audita-api/api/.../AuthController.java:203-214`  
**Issue:** When `trustForwardedHeaders` is `true`, client IP extracted from `X-Forwarded-For` — trivially spoofable if API not behind trusted proxy that overwrites the header. Attacker rotates header value to bypass IP-based rate limiting.  
**Root cause:** Trusting client-supplied forwarded headers without validation.  
**Recommended fix:** Use Spring's `ForwardedHeaderFilter` with trusted proxy configuration. Configure load balancer to overwrite (not append) the header.

---

### [Medium] In-Memory Rate Limiting Not Distributed

**Location:** `audita-api/infrastructure/.../AuthService.java:53,358-381`  
**Issue:** Rate limiting uses `ConcurrentHashMap`. In multi-instance deployment, attacker distributes attempts across instances, multiplying effective limit. Code acknowledges this limitation.  
**Root cause:** In-memory data structure with no distributed coordination.  
**Recommended fix:** Replace with Redis-based rate limiting (Bucket4j with Redis, or Redis sliding window) for production HA deployments.

---

### [Medium] SSE Stream Token Lacks Tenant Isolation Validation

**Location:** `audita-api/api/.../NotificationController.java:84-93`  
**Issue:** Stream token contains `tenantSlug` but it is never validated against active tenant context. SSE endpoint is `permitAll`. Notifications could leak across tenants if stream token's tenant doesn't match request context.  
**Root cause:** Stream token's `tenantSlug` claim never extracted or validated.  
**Recommended fix:** Extract `tenantSlug` from stream token claims and set/validate against `TenantContext` before subscribing.

---

### [Medium] No Security Response Headers Configured

**Location:** `audita-api/api/.../SecurityConfig.java:46-64`  
**Issue:** No `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security`, `Content-Security-Policy`, or `Referrer-Policy` headers. Spring Security's `http.headers()` not called.  
**Root cause:** Headers configuration omitted from security config.  
**Recommended fix:** Add `http.headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(Customizer.withDefaults()).httpStrictTransportSecurity(Customizer.withDefaults()))`.

---

### [Medium] CSP `script-src` Allows `'unsafe-inline'`

**Location:** `audita-web/nuxt.config.ts:32`  
**Issue:** CSP includes `'unsafe-inline'` in `script-src`, weakening XSS protection. Combined with `v-html` XSS vectors, exploitation is straightforward. `nonce: false` means no nonce-based CSP either.  
**Root cause:** Nuxt 3 SPA mode typically requires `'unsafe-inline'` for hydration.  
**Recommended fix:** Enable nonce-based CSP (`nonce: true` in nuxt-security config). Remove `'unsafe-inline'` once nonces are working.

---

### [Medium] CSP `connect-src` Overly Permissive

**Location:** `audita-web/nuxt.config.ts:34`  
**Issue:** `connect-src` allows `https:`, `wss:`, `ws:` — application can connect to any HTTPS/WSS origin. Nullifies data exfiltration protection.  
**Root cause:** Broad wildcard instead of specific API backend domain.  
**Recommended fix:** Restrict to actual API origin(s): `connect-src: ["'self'", "https://api.audita.io"]`.

---

### [High] Access Token Stored in JS-Accessible Pinia State ~~[Medium]~~ **[revised: High]**

**Location:** `audita-web/stores/auth.ts:68-77,116-131`, `plugins/api.ts:110`  
**Issue:** Access token in Pinia reactive state is accessible to any JavaScript on the page. Combined with the `v-html` XSS vectors (see Critical findings), an attacker can read `useAuthStore().accessToken` and exfiltrate it for full account takeover.  
**Root cause:** Token stored in JS-accessible state for convenience; HttpOnly cookie only for refresh token.  
**Revised severity rationale:** In isolation this finding is Medium. But as part of the XSS → token exfiltration attack chain, it directly enables full account compromise. Until DOMPurify is integrated and the XSS risk is eliminated, this should be treated as High. Both must be resolved in the same sprint.  
**Recommended fix:** Consider storing access token in a closure variable (not reactive state) or using a Web Worker for API calls. Alternatively, accept the risk only after DOMPurify integration is confirmed complete and tested.

---

### [Medium] CSRF Protection Disabled With Cookie-Scoped Refresh Token **[revised from High]**

**Location:** `audita-api/api/.../SecurityConfig.java:53`, `audita-web/nuxt.config.ts:20`  
**Issue:** CSRF disabled globally. The refresh token is delivered as an HttpOnly cookie scoped to `/api/v1/auth`. `SameSite=Strict` is in place and provides the primary cross-site submission defence.  
**Root cause:** Pragmatic decision to simplify API.  
**Revised severity rationale:** With `SameSite=Strict` enforced, residual CSRF risk is low. High was disproportionate; reclassified to Medium as a defence-in-depth gap.  
**Recommended fix:** Enable CSRF for cookie-scoped auth endpoints only, using a custom `CsrfTokenRequestHandler` that exempts bearer-token-authenticated endpoints.

---

### [Medium] Attachment Download Bypasses API Proxy

**Location:** `audita-web/composables/useChangeRequests.ts:186-213`  
**Issue:** `downloadAttachment` uses `config.public.apiBase` to construct direct URL to backend, bypassing Nuxt proxy. Bearer token sent directly from browser to backend. Exposes backend URL if `apiBase` is absolute.  
**Root cause:** `fetch()` for file downloads cannot easily go through Nuxt `$fetch` proxy.  
**Recommended fix:** Route attachment downloads through a Nuxt server API route that proxies to the backend, keeping the token server-side.

---

## 6. Code Quality Issues

### [High] 2161-Line Monolithic Component

**Location:** `audita-web/pages/change-requests/[id].vue`  
**Issue:** Single-file component contains entire CR detail page: tabs, edit/view modes, approver management (drag-and-drop, batch add, search), attachments, activity stream, comments with rich text and @mentions, reject modal. ~1200 lines of script logic.  
**Root cause:** Feature accretion without refactoring into sub-components.  
**Recommended fix:** Extract into: `CrDetailTabs`, `CrApproverPanel`, `CrAttachmentManager`, `CrActivityStream`, `CrCommentThread`, `CrRejectModal`. Extract composables: `useApprovers`, `useAttachments`, `useComments`.

---

### [Medium] 1179-Line Settings Component

**Location:** `audita-web/pages/admin/settings/index.vue`  
**Issue:** Combines org profile, feature flags, security defaults, workflow defaults, SLA defaults, auto-approver config, audit export defaults, custom fields CRUD, and sample data management in one component.  
**Root cause:** Multiple admin features consolidated without decomposition.  
**Recommended fix:** Extract each settings section into its own component: `SettingsGeneral`, `SettingsWorkflow`, `SettingsSla`, `SettingsCustomFields`, etc.

---

### [Medium] Duplicated Dark Mode Toggle Logic

**Location:** `layouts/default.vue:430-441,451-458`, `components/shared/AppUserMenu.vue:86-91,107-112`  
**Issue:** Dark mode toggle and persistence duplicated between layout and user menu. Both independently read/write `localStorage("color-scheme")`. Separate `isDark` refs get out of sync.  
**Root cause:** No shared composable for theme management.  
**Recommended fix:** Create `useTheme()` composable with shared reactive state. Both components consume the same composable.

---

### [Medium] Duplicated Password Strength Indicator

**Location:** `pages/auth/reset-password.vue:226-248`, `accept-invite.vue:247-265`, `setup.vue:247-268`, `platform/bootstrap.vue:138-160`  
**Issue:** Password strength scoring, labeling, and rendering copy-pasted across four pages with variations in scoring thresholds and color classes.  
**Root cause:** No shared component for password strength.  
**Recommended fix:** Create `PasswordStrengthMeter.vue` component with standardized scoring algorithm.

---

### [Medium] Inconsistent Role Checking in Middleware

**Location:** `middleware/admin-only.ts:5`, `middleware/role.ts:6-9`, `pages/audit-trail/index.vue:183-185`  
**Issue:** Three different role-checking patterns: hardcoded string comparison (`"Admin"`), `meta.requiredRole`, and inline `isAdmin || isAuditor`. If role string changes, middleware breaks silently.  
**Root cause:** No centralized role-checking utility.  
**Recommended fix:** Create `useRoleGuard(requiredRoles)` composable. All middleware and pages use the same guard. Use auth store getters (`isAdmin`) instead of string comparison.

---

### [Medium] Inconsistent Error Handling Patterns

**Location:** `composables/useSampleData.ts:39-42`, `pages/change-requests/[id].vue:679`, `pages/change-requests/new.vue:689`  
**Issue:** Mix of `error instanceof Error ? error.message : fallback`, `error: any` type annotations, and silent `catch {}` blocks. Inconsistent user-facing error messages.  
**Root cause:** No enforced error handling convention.  
**Recommended fix:** Standardize on `resolveApiErrorMessage()` utility. Enforce `unknown` type for catch variables. Never silently swallow errors.

---

## 7. Low Severity Findings

### [Low] Setup Token Timing Attack

**Location:** `PlatformBootstrapController.java:55`  
**Recommended fix:** Use `MessageDigest.isEqual()` for constant-time comparison.

### [Low] Unrestricted `class` Attribute on Sanitized `span` Elements

**Location:** `CommentService.java:46-49`  
**Recommended fix:** Restrict `class` to whitelist (`mention`, `mention-suggestion`).

### [Low] Missing Indexes on Token Hash Columns

**Location:** `V1__create_tenant_schema.sql:59-83`  
**Recommended fix:** Add indexes on `password_reset_tokens.token_hash` and `invite_tokens.token_hash`.

### [Low] Single-Thread SSE Heartbeat Executor

**Location:** `NotificationService.java:36-40`  
**Recommended fix:** Use `newScheduledThreadPool(2)` or scale based on connection count.

### [Low] SSE Stream Token in URL Query Parameter

**Location:** `sse.client.ts:25`  
**Recommended fix:** Inherent `EventSource` limitation. Accept risk; ensure tokens are short-lived and rotated.

### [Low] Sample Data Password Exposed in Client Template

**Location:** `admin/settings/index.vue:507-508`  
**Recommended fix:** Move to server-side constant or documentation only.

### [Low] Notification Link Navigation Without Validation

**Location:** `AppNotificationBell.vue:93`  
**Recommended fix:** Validate `n.link` is a safe internal path before `navigateTo`.

### [Low] Redundant Auth Middleware

**Location:** `middleware/auth.ts` vs `middleware/auth.global.ts`  
**Recommended fix:** Remove named `auth` middleware; rely on global middleware only.

### [Low] `setInterval` Without Unmount Cleanup

**Location:** `[id].vue:2088-2109`  
**Recommended fix:** Clear interval in `onUnmounted`.

### [Low] Unbounded Notification Store Growth

**Location:** `stores/notifications.ts:22-27`  
**Recommended fix:** Cap at 100 items; evict oldest.

### [Low] PII (Personal Email) in Package Metadata and Docker Labels

**Location:** `audita-web/package.json:8`, `audita-web/Dockerfile:21`  
**Recommended fix:** Replace with organisational email.

### [Low] Minimal `.dockerignore` Files

**Location:** `audita-api/.dockerignore`, `audita-web/.dockerignore`  
**Recommended fix:** Add `.git`, `.env`, `*.md`, `.sonarlint/`, test files.

---

## Summary

| Severity     | Count | Key Themes                                                                                                                                                                          |
| ------------ | ----- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Critical** | 3     | Unauthenticated setup endpoint, XSS via v-html, open redirect                                                                                                                       |
| **High**     | 13    | Missing rate limits, JWT irrevocability, non-atomic idempotency, SSO bypasses, dangerous DDL auto-update, exposed infrastructure, audit log DB mutability, access token in JS state |
| **Medium**   | 26    | Architecture violations, N+1 queries, memory-bound exports, CSP weaknesses, duplicated code, tenant isolation gaps, CSRF (defence-in-depth)                                         |
| **Low**      | 12    | Timing attacks, missing indexes, cleanup gaps, minor information disclosure                                                                                                         |

### Severity Changes vs. Original Audit

| Finding                             | Original | Revised | Reason                                                                     |
| ----------------------------------- | -------- | ------- | -------------------------------------------------------------------------- |
| Audit Log Not Immutable at DB Level | Medium   | High    | Core product value proposition; application-layer enforcement insufficient |
| Access Token in Pinia State         | Medium   | High    | Directly enables XSS → full account takeover chain                         |
| CSRF Protection Disabled            | High     | Medium  | `SameSite=Strict` provides primary mitigation; High was disproportionate   |

### Priority Remediation Order

1. **Immediate (Critical):** Setup endpoint auth guard, DOMPurify integration, redirect validation
2. **Sprint 1 (High):** Super admin rate limiting, JWT token versioning, idempotency atomicity, SSO domain whitelist, `JPA_DDL_AUTO=validate`, Docker port binding; **also: audit log DB immutability triggers, Pinia access token closure/Worker migration (pair with DOMPurify)**
3. **Sprint 2 (Medium):** N+1 query batch fetching, Caffeine tenant cache, CSP nonce-based policy, component decomposition, `REQUIRES_NEW` audit log propagation (with outcome field), distributed rate limiting/SSO stores
4. **Backlog (Low):** Timing-safe comparisons, missing indexes, cleanup lifecycle hooks
