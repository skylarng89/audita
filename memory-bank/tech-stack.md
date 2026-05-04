# Audita — Technology Stack

**Last Updated:** 2026-05-04

---

## Frontend (`audita-web`)

| Concern       | Technology             | Notes                                                                             |
| ------------- | ---------------------- | --------------------------------------------------------------------------------- |
| Framework     | **Nuxt 3** (Vue 3)     | SSR + SPA hydration; file-system routing                                          |
| Styling       | **Tailwind CSS v4**    | Integrated via `@tailwindcss/vite`; utility-first; dark mode via `class` strategy |
| State         | **Pinia**              | `useAuthStore`, `useNotificationStore`, `useSettingsStore`                        |
| Rich Text     | **TipTap**             | Extensions: StarterKit, Link, Image, Mention, Placeholder                         |
| HTTP Client   | Nuxt `$fetch` wrapper  | Plugin: `plugins/api.ts` with auth headers injected                               |
| Real-time     | **SSE** (EventSource)  | Plugin: `plugins/sse.ts`; notification stream                                     |
| Build Tool    | **pnpm**               | Workspace-aware package manager                                                   |
| Icons         | To be determined       | Recommend: `@iconify/vue` or Heroicons                                            |
| Date handling | `date-fns` or `day.js` | Lightweight; no moment.js                                                         |

### Nuxt Directory Layout

```
audita-web/
├── assets/                    # Global CSS, fonts, images
├── components/
│   ├── cr/                    # Change request components
│   ├── approvers/             # Approver list, decision UI
│   ├── comments/              # Rich text comment thread
│   ├── notifications/         # Bell, notification feed
│   └── shared/                # Buttons, modals, tables, badges
├── composables/
│   ├── useAuth.ts
│   ├── useChangeRequests.ts
│   ├── useNotifications.ts    # SSE connection management
│   └── useToast.ts
├── layouts/
│   ├── default.vue            # Authenticated: sidebar + header
│   ├── auth.vue               # Login / reset password
│   └── platform.vue           # Super Admin layout
├── pages/
│   ├── auth/                  # Sign in, forgot password, reset, accept invite
│   ├── dashboard/
│   ├── change-requests/       # List, detail, create
│   ├── admin/                 # Settings pages (General, Workflow, Custom Fields, etc.)
│   ├── audit-trail/
│   └── platform/              # Super Admin: tenants, provisioning
├── stores/
│   ├── auth.ts
│   ├── notifications.ts
│   └── settings.ts
├── middleware/
│   ├── auth.ts                # Redirect unauthenticated
│   ├── role.ts                # Role-based route guards
│   └── tenant.ts              # Resolve tenant from subdomain
└── plugins/
    ├── api.ts                 # $fetch with Authorization header
    └── sse.ts                 # SSE client
```

---

## Backend (`audita-api`)

| Concern         | Technology                            | Notes                                                          |
| --------------- | ------------------------------------- | -------------------------------------------------------------- |
| Language        | **Java 25**                           | Virtual threads (Project Loom) for SSE concurrency             |
| Framework       | **Spring Boot 4**                     | Auto-configuration, embedded Tomcat                            |
| Security        | **Spring Security 6**                 | Method-level `@PreAuthorize`; JWT filter; OAuth2 Client        |
| ORM             | **Hibernate 7** (Jakarta Persistence) | Per-tenant schema via `CurrentTenantIdentifierResolver`        |
| DB Migrations   | **Flyway**                            | Per-tenant schema migration on startup + tenant creation       |
| Email           | **Spring Mail** + **Thymeleaf**       | HTML templates + plaintext fallback                            |
| Build           | **Gradle 9** (Kotlin DSL)             | Multi-module: `api`, `domain`, `application`, `infrastructure` |
| JSON            | **Jackson**                           | RFC 7807 Problem Detail error responses                        |
| Scheduling      | **Spring `@Scheduled`**               | SLA evaluation job every 5 minutes                             |
| Async           | **Spring `@Async`** thread pool       | Notification dispatch                                          |
| HTML Sanitise   | **OWASP Java HTML Sanitizer**         | Rich-text input from TipTap before storage                     |
| Logging         | **SLF4J + Logback**                   | Structured JSON; MDC: `tenant_id`, `user_id`, `request_id`     |
| Health          | **Spring Boot Actuator**              | `/actuator/health` for container probes                        |
| Connection Pool | **HikariCP**                          | Bundled with Spring Boot                                       |

### Backend Package Architecture (Hexagonal)

```
audita-api/
└── src/main/java/io/audita/
    ├── domain/          # Pure business entities and domain services (no Spring deps)
    ├── application/     # Use cases / application services
    ├── infrastructure/  # DB, email, file storage, SSE, schedulers
    └── api/             # REST controllers, DTOs, exception handlers
```

---

## Database

| Concern           | Technology                          | Notes                                                                |
| ----------------- | ----------------------------------- | -------------------------------------------------------------------- |
| Engine            | **PostgreSQL 16+**                  |                                                                      |
| Multi-tenancy     | Schema-per-tenant                   | `public` schema: tenants, super_admins, sso_configs, allowed_domains |
| Tenant schema     | One per org                         | E.g. `tenant_acme`: users, roles, change_requests, etc.              |
| Tenant resolution | `X-Tenant-Slug` header or subdomain | Thread-local `TenantContext`                                         |
| Money/numeric     | `NUMERIC(19,4)`                     | Not applicable in v1 (no financial data)                             |
| Monetary amounts  | N/A                                 | Not a financial system                                               |

---

## Infrastructure & DevOps

| Concern            | Technology                                                         | Notes                                      |
| ------------------ | ------------------------------------------------------------------ | ------------------------------------------ |
| Containerisation   | **Docker + Docker Compose**                                        | Dev environment                            |
| Orchestration      | **Helm chart (Kubernetes)**                                        | Production                                 |
| CI/CD              | TBD (GitHub Actions recommended)                                   | SAST, DAST, SCA gates                      |
| TLS                | cert-manager (K8s)                                                 | Auto-provisioned Let's Encrypt             |
| File Storage       | **Local filesystem** or **S3-compatible** (AWS S3, MinIO)          | Configurable per org                       |
| Auth encryption    | **AES-256**                                                        | SSO client secrets, SMTP passwords at rest |
| Secrets management | Environment variables / Sealed Secrets / External Secrets Operator | Never in code                              |

---

## Auth Stack Detail

| Component         | Implementation                                                     |
| ----------------- | ------------------------------------------------------------------ |
| Password hashing  | bcrypt, min cost factor 12                                         |
| Access token      | JWT, 15-minute expiry, `Authorization: Bearer` header              |
| Refresh token     | Long-lived (7 days), HttpOnly `SameSite=Strict` cookie             |
| Token rotation    | Refresh token rotated on every use                                 |
| Google SSO        | Spring Security OAuth2 Client, OIDC scopes: `openid email profile` |
| Microsoft SSO     | Azure AD OIDC, `https://login.microsoftonline.com/{tenantId}/v2.0` |
| JIT provisioning  | Auto-create user on first SSO login if email domain permitted      |
| Rate limiting     | 5 login attempts / 15 min / IP; 3 forgot-password / hour           |
| Password reset    | Single-use token, 1-hour expiry                                    |
| Invite acceptance | Secure link, 48-hour expiry                                        |

---

## Dependency Risk Notes

| Library              | Risk                                     | Mitigation                                            |
| -------------------- | ---------------------------------------- | ----------------------------------------------------- |
| TipTap               | Rich-text output may contain XSS vectors | OWASP HTML Sanitizer on server before storage         |
| AWS SDK v2           | Credential exposure                      | Env vars only; never stored in code                   |
| Thymeleaf            | Template injection                       | Use `th:text` not `th:utext` for user-controlled data |
| Spring OAuth2 Client | CSRF on SSO callback                     | `state` parameter validated on every callback         |
