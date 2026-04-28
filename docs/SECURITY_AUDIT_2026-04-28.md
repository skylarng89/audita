# Audita Security Review (Adversarial)

Date: 2026-04-28  
Scope: Backend (Spring Boot), Frontend (Nuxt), auth/session, multi-tenancy, CR collaboration flows  
Method: Manual adversarial review of high-risk code paths with exploit-oriented reasoning

## Executive Summary

The application has strong foundations (JWT signing, token hashing-at-rest, HTML sanitization for comments, role-based endpoint guards), but several high-impact vulnerabilities remain in tenant isolation, token transport, and authorization depth.

Top risks:

1. Critical tenant-boundary weakness via unsanitized schema switching SQL.
2. High token exposure risk from placing SSO access tokens in URL query strings.
3. High broken object-level authorization (BOLA/IDOR) in change request mutation paths.
4. High misconfigured CORS (`*` origin patterns + credentials enabled).
5. High operational security gaps from insecure defaults in runtime configuration.

## Severity Distribution

- Critical: 1
- High: 5
- Medium: 4
- Low: 2

## Detailed Findings

## 1) Critical: Tenant Schema SQL Injection / Tenant Escape Vector

Severity: Critical  
Category: Multi-tenancy isolation, SQL injection  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java](audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java#L31)
- [audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java](audita-api/api/src/main/java/io/audita/api/security/TenantResolutionFilter.java#L33)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/tenant/AuditaTenantIdentifierResolver.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/tenant/AuditaTenantIdentifierResolver.java#L20)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/tenant/AuditaMultiTenantConnectionProvider.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/tenant/AuditaMultiTenantConnectionProvider.java#L38)

Description:
Tenant slug is accepted from `X-Tenant-Slug` and propagated into `SET search_path TO <tenant>, public` via string concatenation. There is no strict tenant identifier validation at the filter boundary before SQL construction.

Why this is dangerous:

- `search_path` controls which schema all subsequent queries execute against.
- Unsanitized schema identifiers in dynamic SQL are a classic tenant breakout surface.
- Even if obvious payloads are blocked by the DB driver/parser, this is still a critical design flaw for isolation guarantees.

Recommended fix:

1. Enforce strict tenant slug regex in the filter (`^[a-z0-9-]{1,100}$`).
2. Resolve slug to a canonical schema name from trusted DB metadata.
3. Quote schema identifiers safely (or use a whitelisted mapping table) before executing `SET search_path`.
4. Reject unknown tenant slugs before ORM/session creation.

Sample hardening:

```java
private static final Pattern TENANT_SLUG = Pattern.compile("^[a-z0-9-]{1,100}$");

String slug = request.getHeader("X-Tenant-Slug");
if (slug != null) {
    String normalized = slug.trim().toLowerCase(Locale.ROOT);
    if (!TENANT_SLUG.matcher(normalized).matches()) {
        throw new AccessDeniedException("Invalid tenant slug");
    }
    TenantContext.setCurrentTenant(normalized);
}
```

```java
String schema = tenantCatalog.requireSchemaForSlug(tenantIdentifier);
try (Statement st = connection.createStatement()) {
    st.execute("SET search_path TO \"" + schema.replace("\"", "\"\"") + "\", public");
}
```

## 2) High: Access Token Leakage via URL Query String in SSO Callback

Severity: High  
Category: Token handling / session security  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/controller/SsoController.java](audita-api/api/src/main/java/io/audita/api/controller/SsoController.java#L99)
- [audita-web/pages/auth/sso-callback.vue](audita-web/pages/auth/sso-callback.vue#L59)
- [audita-web/pages/auth/sso-callback.vue](audita-web/pages/auth/sso-callback.vue#L70)

Description:
SSO callback redirects include `access_token` in query params. Frontend reads it from URL and stores it in app auth state.

Exploit/impact:

- Query tokens can leak through browser history, reverse proxy logs, analytics, screenshot tooling, and accidental sharing.
- Referrer leakage risk increases whenever user navigates away before URL cleanup.

Recommended fix:

1. Never place bearer tokens in URL query.
2. Return to frontend with one-time authorization code and exchange server-side.
3. Keep access/refresh in secure cookies where possible.
4. Immediately clear callback URL using history replace after one-time code processing.

## 3) High: Broken Object-Level Authorization (BOLA/IDOR) on Change Requests

Severity: High  
Category: Authorization  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java](audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java#L65)
- [audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java](audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java#L83)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L95)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L147)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L174)

Description:
Role checks exist, but mutation methods (`update`, `submit`, `cancel`, custom-fields, attachments) do not verify resource ownership/assignment constraints for requester-level users.

Exploit scenario:

- Any authenticated requester with guessed/enumerated UUID can mutate another user’s change request in the same tenant.

Recommended fix:

1. Add policy checks in service layer using authenticated user identity.
2. For requester role: allow mutation only if `createdBy == actor`.
3. Keep admin/super-admin override explicit.
4. Add regression tests for cross-user mutation attempts.

Sample policy guard:

```java
private void assertCanMutate(ChangeRequestEntity cr, UserPrincipal actor) {
    boolean isAdmin = actor.authorities().stream().anyMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

    if (!isAdmin) {
        if (cr.getCreatedBy() == null || !cr.getCreatedBy().getId().equals(actor.userId())) {
            throw new DomainNotPermittedException("FORBIDDEN", "You cannot modify this change request.");
        }
    }
}
```

## 4) High: CORS Misconfiguration (`*` Origin Patterns + Credentials)

Severity: High  
Category: Browser security / cross-origin controls  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java](audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java#L84)
- [audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java](audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java#L87)

Description:
`allowedOriginPatterns` is `*` while credentials are allowed. This creates broad cross-origin trust and makes future cookie-based auth exposure easier to exploit.

Recommended fix:

1. Replace wildcard with explicit production origins from configuration.
2. Separate dev and prod CORS profiles.
3. Include exact allowed headers/methods and enforce strict origin matching.

## 5) High: Insecure Runtime Defaults and Secret Hygiene

Severity: High  
Category: Secrets management / secure defaults  
Evidence:

- [audita-api/api/src/main/resources/application.yml](audita-api/api/src/main/resources/application.yml#L21)
- [audita-api/api/src/main/resources/application.yml](audita-api/api/src/main/resources/application.yml#L83)
- [audita-api/api/src/main/resources/application.yml](audita-api/api/src/main/resources/application.yml#L115)
- [docker-compose.yml](docker-compose.yml#L12)
- [docker-compose.yml](docker-compose.yml#L45)
- [docker-compose.yml](docker-compose.yml#L46)
- [docker-compose.yml](docker-compose.yml#L49)

Description:

- `ddl-auto: update` in runtime config.
- Fallback JWT secret in config.
- Debug logging enabled for app package.
- Compose includes static secrets/passwords.

Impact:

- Risk of accidental prod misconfiguration, weak secret posture, and over-verbose sensitive operational logs.

Recommended fix:

1. Fail startup when required secrets are missing in non-dev profiles.
2. Disable `ddl-auto` outside ephemeral local development.
3. Set production logging to INFO/WARN and add sensitive-field masking.
4. Source secrets from vault/KMS and remove static values from committed compose for non-local environments.

## 6) Medium: Trusting `X-Forwarded-For` Directly Weakens Rate Limiting

Severity: Medium  
Category: Abuse prevention  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/controller/AuthController.java](audita-api/api/src/main/java/io/audita/api/controller/AuthController.java#L141)
- [audita-api/api/src/main/java/io/audita/api/controller/AuthController.java](audita-api/api/src/main/java/io/audita/api/controller/AuthController.java#L143)

Description:
Rate limiting key uses a client IP derived from raw `X-Forwarded-For` header without trusted proxy validation.

Impact:
Attackers can spoof this header and evade per-IP controls.

Recommended fix:

- Honor forwarded headers only from trusted reverse proxies.
- Otherwise use `request.getRemoteAddr()`.
- Consider distributed limiter with IP+device fingerprint and account controls.

## 7) Medium: Public Bootstrap Endpoint Is Open by Design (Takeover Risk if Misused)

Severity: Medium  
Category: Initialization hardening  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java](audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java#L23)
- [audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java](audita-api/api/src/main/java/io/audita/api/controller/PlatformBootstrapController.java#L25)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java#L213)

Description:
Bootstrap is public until first super admin exists. This is intentional for first-run UX, but dangerous without deployment-time guardrails.

Recommended fix:

1. Gate bootstrap behind one-time setup token or install mode flag.
2. Auto-disable endpoint permanently after successful bootstrap.
3. Add network ACL restriction for bootstrap route during provisioning.

## 8) Medium: File Upload Controls Are Insufficient (No Type/Size/Content Enforcement)

Severity: Medium  
Category: File upload security  
Evidence:

- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L332)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L341)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java#L342)

Description:
Upload path accepts arbitrary content and metadata with no hard cap, malware scanning, or MIME/content signature validation.

Recommended fix:

1. Enforce max file size and permitted type allowlist.
2. Verify magic bytes, not only client-declared MIME.
3. Quarantine and scan before making file available.
4. Store outside web root and use signed-download indirection.

## 9) Medium: Internal Storage Path Disclosure in API Response

Severity: Medium  
Category: Information disclosure  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/dto/response/AttachmentResponse.java](audita-api/api/src/main/java/io/audita/api/dto/response/AttachmentResponse.java#L13)
- [audita-api/api/src/main/java/io/audita/api/dto/response/AttachmentResponse.java](audita-api/api/src/main/java/io/audita/api/dto/response/AttachmentResponse.java#L27)

Description:
Attachment response includes server storage path. This leaks internal filesystem layout and may aid lateral attack planning.

Recommended fix:

- Replace `storagePath` exposure with opaque `attachmentId` and signed download endpoint.

## 10) Medium: In-Memory Auth Rate Limiter and SSO State Store Are Not HA-Safe

Severity: Medium  
Category: Availability / abuse resistance  
Evidence:

- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java#L41)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SsoService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SsoService.java#L43)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SsoService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SsoService.java#L86)

Description:
Critical anti-abuse structures are in-memory. In multi-instance deployments, attackers can bypass limits by hopping instances; state map can grow without cleanup pressure.

Recommended fix:

- Move rate limits and OAuth state storage to Redis with TTL and atomic operations.

## 11) Low: Password Policy Is Too Weak for Enterprise Security Baseline

Severity: Low  
Category: Credential policy  
Evidence:

- [audita-api/api/src/main/java/io/audita/api/dto/request/ResetPasswordRequest.java](audita-api/api/src/main/java/io/audita/api/dto/request/ResetPasswordRequest.java#L8)
- [audita-api/api/src/main/java/io/audita/api/dto/request/AcceptInviteRequest.java](audita-api/api/src/main/java/io/audita/api/dto/request/AcceptInviteRequest.java#L9)

Description:
Current rule only enforces minimum length (8). No denylist, complexity, or breached password checks.

Recommended fix:

- Adopt stronger policy and reject known-compromised passwords (HIBP k-anonymity or equivalent).

## 12) Low: Refresh Session Not Bound to Device/Context

Severity: Low  
Category: Session security  
Evidence:

- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java#L118)
- [audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java](audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/AuthService.java#L132)

Description:
Refresh tokens are rotated and hashed, but not bound to device fingerprint, user-agent hash, or IP risk scoring.

Recommended fix:

- Add token family/session metadata and anomaly detection for refresh operations.

## Attack Chains

## Chain A: Cross-Tenant Data Access/Corruption Path

1. Attacker sends crafted or arbitrary tenant slug header.
2. Tenant value is inserted into `search_path` SQL without strong validation.
3. ORM queries execute in attacker-influenced schema context.
4. Attacker reads/modifies data outside intended tenant boundary.

## Chain B: Token Exfiltration Through URL

1. User completes SSO.
2. Access token appears in callback URL query.
3. Token leaks through logs/history/referrer.
4. Attacker reuses token within expiry window to act as user.

## Chain C: Horizontal Privilege Escalation on CR Mutation

1. Requester obtains another CR UUID (UI metadata, logs, etc.).
2. Calls update/submit/cancel endpoints with same tenant and role.
3. Service executes mutation without owner check.
4. Unauthorized business-impacting changes occur.

## Prioritized Remediation Plan

Phase 0 (Immediate, 24-72h):

- Fix tenant slug validation + safe schema switching.
- Remove access token from URL-based SSO callback flow.
- Tighten CORS to explicit origin allowlist.
- Add object-level authorization checks in CR mutation methods.

Phase 1 (This sprint):

- Remove insecure defaults (`ddl-auto`, fallback secrets, debug verbosity in prod).
- Harden upload pipeline (size/type/signature/scan).
- Stop exposing storage paths in API payloads.

Phase 2 (Next sprint):

- Move rate limiting and OAuth state to Redis.
- Strengthen password policy and refresh-session binding.
- Add security tests for BOLA, tenant-boundary, and SSO token handling regressions.

## Security Regression Tests To Add

1. Tenant slug fuzz tests against header and DB schema resolver.
2. BOLA tests: requester cannot mutate CR not created by them.
3. SSO callback test: no bearer token in URL or logs.
4. CORS tests: only configured origins accepted with credentials.
5. Upload tests: blocked on oversize and disallowed signatures.

## Final Note

The most urgent work is tenant isolation correctness and SSO token transport redesign. Those two issues have the highest combined blast radius across confidentiality, integrity, and account takeover risk.
