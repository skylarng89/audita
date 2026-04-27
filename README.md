# Audita

**Self-hosted, multi-tenant ITIL/ITSM Change Management platform.**

Audita gives organisations a structured, auditable workflow for managing IT change requests — from creation through approval, SLA tracking, and immutable audit trail — across fully isolated tenants.

---

## Architecture

| Service      | Technology                                       | Port            |
| ------------ | ------------------------------------------------ | --------------- |
| `audita-api` | Java 25 + Spring Boot 4 (hexagonal architecture) | `8080`          |
| `audita-web` | Nuxt 3 + Vue 3 + Tailwind CSS                    | `3000`          |
| `db`         | PostgreSQL 17                                    | `5432`          |
| `mailhog`    | MailHog (dev SMTP catcher)                       | `1025` / `8025` |

The API uses a **schema-per-tenant** isolation model — each organisation gets its own PostgreSQL schema, migrated by Flyway on provisioning. The frontend resolves the active tenant from the subdomain (`acme.audita.io`).

```
audita/
├── audita-api/          # Spring Boot 4 — domain, application, infrastructure, api modules
├── audita-web/          # Nuxt 3 frontend
├── docker-compose.yml   # Full-stack dev environment
└── docs/                # PRD, SRS, USER_FLOW
```

---

## Prerequisites

| Tool                         | Minimum version  |
| ---------------------------- | ---------------- |
| Docker + Docker Compose      | 24.x / v2 plugin |
| Java (for standalone API)    | 25               |
| Node.js (for standalone web) | 22               |
| pnpm (for standalone web)    | 9                |

---

## Quick start — Docker Compose (recommended)

The fastest way to run the entire stack with a single command:

```bash
git clone <repo-url>
cd audita
docker compose up --build
```

Services will start in dependency order (PostgreSQL → API → web). Once healthy:

| URL                                   | Description                       |
| ------------------------------------- | --------------------------------- |
| http://localhost:3000                 | Audita web UI                     |
| http://localhost:8080/actuator/health | API health check                  |
| http://localhost:8025                 | MailHog — inspect outgoing emails |

### Stop and remove containers

```bash
docker compose down
```

To also remove the database and uploads volumes (full reset):

```bash
docker compose down -v
```

### Environment variables

The Compose file ships with safe defaults for local development. **Do not use these values in production.**

| Variable                    | Default                            | Description                                       |
| --------------------------- | ---------------------------------- | ------------------------------------------------- |
| `DATABASE_URL`              | `jdbc:postgresql://db:5432/audita` | JDBC connection URL                               |
| `DATABASE_USERNAME`         | `audita`                           | PostgreSQL username                               |
| `DATABASE_PASSWORD`         | `secret`                           | PostgreSQL password                               |
| `JWT_SECRET`                | `dev-secret-…`                     | HS256 signing key — **must be changed**           |
| `JWT_EXPIRY_SECONDS`        | `900`                              | Access token TTL (15 min)                         |
| `REFRESH_TOKEN_EXPIRY_DAYS` | `7`                                | Refresh token TTL                                 |
| `APP_ENCRYPTION_KEY`        | `0000…` (64 hex chars)             | AES-256 key for SSO secrets — **must be changed** |
| `APP_BASE_URL`              | `http://localhost:3000`            | Frontend base URL (used in email links)           |
| `NUXT_PUBLIC_API_BASE`      | `http://api:8080`                  | API base URL seen by the web container            |

To override any variable without modifying the Compose file, create a `.env` file in the project root:

```dotenv
JWT_SECRET=your-very-long-random-secret-at-least-32-chars
APP_ENCRYPTION_KEY=<64 random hex characters>
```

---

## Standalone — API only (`audita-api`)

### Prerequisites

- Java 25 installed and on `PATH`
- PostgreSQL 17 running locally (or adjust `DATABASE_URL`)

### Run

```bash
cd audita-api

# Start PostgreSQL via Docker (if you don't have one)
docker run -d \
  --name audita-db \
  -e POSTGRES_DB=audita \
  -e POSTGRES_USER=audita \
  -e POSTGRES_PASSWORD=secret \
  -p 5432:5432 \
  postgres:17-alpine

# Build and run with the dev Spring profile (coloured console logging)
./gradlew :api:bootRun --args='--spring.profiles.active=dev'
```

The API starts on **http://localhost:8080**. Flyway will apply the `public` schema baseline migration automatically on first boot.

### Build a fat JAR

```bash
./gradlew :api:bootJar
java -jar api/build/libs/audita-api.jar
```

### Run tests

```bash
./gradlew test
```

### Environment variables (standalone)

Set these before running, or pass as `--spring.datasource.url=…` arguments:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/audita
export DATABASE_USERNAME=audita
export DATABASE_PASSWORD=secret
export JWT_SECRET=change-me-in-production-must-be-at-least-32-chars-long
export APP_ENCRYPTION_KEY=<64 hex chars>
export APP_BASE_URL=http://localhost:3000
```

For local email testing, start MailHog:

```bash
docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog
```

---

## Standalone — Web only (`audita-web`)

### Prerequisites

- Node.js 22+
- pnpm 9+ (`npm install -g pnpm` or `corepack enable && corepack prepare pnpm@latest --activate`)

### Install dependencies

```bash
cd audita-web
pnpm install
```

### Development server

```bash
pnpm dev
```

Opens on **http://localhost:3000**. API requests are proxied to `http://localhost:8080` by default (configured in `nuxt.config.ts`).

To point at a different API:

```bash
NUXT_PUBLIC_API_BASE=http://localhost:8080 pnpm dev
```

### Tenant resolution in development

The frontend resolves the active tenant from the subdomain. For local development without a real subdomain, append `?tenant=<slug>` to any URL:

```
http://localhost:3000?tenant=acme
```

This sets the `X-Tenant-Slug` header on all API requests for the duration of that navigation session.

### Build for production

```bash
pnpm build
pnpm preview   # Preview the production build locally
```

### Lint and type-check

```bash
pnpm lint
pnpm typecheck
```

---

## Database

### Schema design

- `public` schema — platform-wide tables: `tenants`, `super_admins`, `tenant_allowed_domains`, `tenant_sso_configs`
- `<tenant_slug>` schema — per-tenant tables: `users`, `roles`, `change_requests`, `approvers`, `comments`, `notifications`, `audit_logs`, etc.

### Migrations

Migrations are managed by **Flyway** and run automatically on startup:

- `audita-api/infrastructure/src/main/resources/db/migration/public/` — public schema baseline
- `audita-api/infrastructure/src/main/resources/db/migration/tenant/` — per-tenant schema (applied when a new org is provisioned)

### Connect directly (Docker Compose)

```bash
docker exec -it audita-db psql -U audita -d audita
```

---

## Logs

The API emits structured JSON logs in non-dev environments. Every log line includes:

```json
{
  "@timestamp": "2026-04-27T12:00:00.000Z",
  "level": "INFO",
  "logger_name": "io.audita.service.AuthService",
  "msg": "User login successful",
  "tenant_id": "acme",
  "user_id": "usr_01J…",
  "request_id": "req_7f3a…"
}
```

In the `dev` Spring profile, logs are coloured and human-readable. Activate it with:

```bash
./gradlew :api:bootRun --args='--spring.profiles.active=dev'
```

---

## Project documentation

| Document                                  | Location                                               |
| ----------------------------------------- | ------------------------------------------------------ |
| Product Requirements (PRD)                | [docs/PRD.md](docs/PRD.md)                             |
| Software Requirements Specification (SRS) | [docs/SRS.md](docs/SRS.md)                             |
| User Flow                                 | [docs/USER_FLOW.md](docs/USER_FLOW.md)                 |
| Architecture decisions                    | [memory-bank/decisions.md](memory-bank/decisions.md)   |
| Technology stack                          | [memory-bank/tech-stack.md](memory-bank/tech-stack.md) |
| Sprint task list                          | [memory-bank/docs/tasks.md](memory-bank/docs/tasks.md) |

---

## Security notes

- `JWT_SECRET` must be at least 32 characters and randomly generated in production
- `APP_ENCRYPTION_KEY` must be a cryptographically random 64-character hex string (256-bit AES key)
- The default PostgreSQL password `secret` must be changed before any internet-facing deployment
- MailHog is a development tool — replace with a real SMTP provider in production
- The `?tenant=` query param for tenant resolution is only active in development builds (`import.meta.dev`)
