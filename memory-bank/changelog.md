# Audita — Changelog

## [0.1.0] — Unreleased (In Development)

### Added (Sprint 1 — 2026-04-27)

- **Authentication stack**: JWT access tokens (15-min, jjwt 0.12.6), SHA-256 hashed refresh tokens (7-day rotating, HttpOnly cookie), BCrypt cost=12
- **AuthService**: login, logout, forgot-password, reset-password, refresh, bootstrap, domain whitelist, sliding-window rate limiting
- **JwtAuthenticationFilter**: validates JWT on every protected request; populates `SecurityContextHolder`
- **TenantResolutionFilter**: sets `TenantContext` from `X-Tenant-Slug` header; lives in `api` module
- **SSO**: Google OIDC + Microsoft Azure AD OAuth2 via `SsoController` + `SsoService`; JIT user provisioning; OAuth account linking; AES-256-GCM encrypted client secrets
- **Platform bootstrap endpoint**: `POST /api/platform/v1/bootstrap` — creates first Super Admin when none exists
- **Auth pages (Nuxt 3)**: sign-in, forgot-password, reset-password (4-segment strength bar), accept-invite, bootstrap/first-run, sso-callback
- **`useAuthStore`** (Pinia): accessToken, user, tenantSlug, `setAuth()`, `clearAuth()`
- **18 unit tests** for `AuthService`: all auth flows, rate limiting, domain whitelist, bootstrap — all passing

### Added (Sprint 0 — 2026-04-27)

- Project documentation: PRD v1.0, SRS v1.0, USER_FLOW v1.0
- UI designs: 40 screens (light + dark) covering all user journeys
- Memory bank initialised: context, tech-stack, design, decisions, patterns
- Sprint plan created: 8 sprints (Sprint 0–7) covering MVP through production-ready release
- `audita-api`: Gradle multi-module (domain / application / infrastructure / api), Spring Boot 4.0.6, Java 25, HikariCP, Hibernate 7 multi-tenancy, Flyway (public + per-tenant), Spring Security scaffold, RFC 7807 global exception handler, Logback structured JSON logging (`logstash-logback-encoder:8.1`)
- `audita-web`: Nuxt 3, Tailwind CSS (custom design tokens), all three layouts, auth/role/tenant middleware, `plugins/api.ts`, `useAuthStore` (Pinia), shared component library (AppButton, AppInput, AppBadge, AppCard, AppModal, AppTable, AppPagination)
- `docker-compose.yml`: PostgreSQL 17, MailHog, Spring Boot API, Nuxt 3 web with healthchecks
- `README.md`: full run instructions (Docker Compose + standalone)
- `.dockerignore` for both repos; `.gitkeep` files in all empty Gradle module source directories
