# Audita

Audita is a self-hosted, multi-tenant ITIL/ITSM Change Management platform.

It provides structured, auditable change workflows across creation, approvals,
collaboration, SLA tracking, and immutable audit trails with schema-per-tenant
data isolation.

## License and Usage Terms

This project is source-available.

- You may use, modify, fork, and contribute to this codebase.
- Personal and internal commercial use are allowed.
- You may not resell this software.
- You may not provide paid hosted/managed services where value substantially
  derives from this software.

See [LICENSE](LICENSE) and [LICENSE-APACHE](LICENSE-APACHE) for exact legal
terms.

## Contents

- [Architecture](#architecture)
- [Repository layout](#repository-layout)
- [Requirements](#requirements)
- [Quick start with Docker Compose](#quick-start-with-docker-compose)
- [Manual deployment](#manual-deployment)
- [Production deployment with Docker](#production-deployment-with-docker)
- [Configuration reference](#configuration-reference)
- [Versioning and release process](#versioning-and-release-process)
- [CI and Docker publishing](#ci-and-docker-publishing)
- [Contributing](#contributing)
- [Security baseline](#security-baseline)

## Architecture

| Service | Technology | Default local port |
| --- | --- | --- |
| audita-api | Java 25 + Spring Boot 4 (hexagonal architecture) | 7080 -> 8080 |
| audita-web | Nuxt 3 + Vue 3 + Tailwind CSS | 7000 -> 3000 |
| db | PostgreSQL 17 | 7432 -> 5432 |
| mailhog | MailHog (dev email capture) | 7025 SMTP, 8025 UI |

Tenant model:

- public schema: platform entities
- tenant schema per org slug: org data and workflow records

## Repository layout

```text
audita/
├── audita-api/                  # Backend modules: domain, application, infrastructure, api
├── audita-web/                  # Nuxt frontend
├── docker-compose.yml           # Local full-stack runtime
├── .github/workflows/           # CI and release automation
├── docs/                        # Product and engineering docs
└── memory-bank/                 # Working project context and decision logs
```

## Requirements

- Docker Engine 24+ with Compose plugin
- Java 25 (manual backend run)
- Node.js 22 and pnpm 9 (manual frontend run)

## Quick start with Docker Compose

1. Clone the repository.
2. Create environment values.
3. Start the stack.

```bash
git clone <repo-url>
cd audita

cat > .env <<'EOF'
JWT_SECRET=replace-with-random-32-plus-char-secret
APP_ENCRYPTION_KEY=replace-with-64-hex-char-aes256-key
EOF

docker compose up --build
```

Local endpoints:

- Web: <http://localhost:7000>
- API health: <http://localhost:7080/actuator/health>
- MailHog UI: <http://localhost:8025>

Stop stack:

```bash
docker compose down
```

Reset all persisted volumes:

```bash
docker compose down -v
```

## Manual deployment

### Backend (audita-api)

1. Provision PostgreSQL 17.
2. Set runtime environment variables.
3. Build and run Spring Boot.

```bash
cd audita-api

export DATABASE_URL=jdbc:postgresql://localhost:5432/audita
export DATABASE_USERNAME=audita
export DATABASE_PASSWORD=change-me
export JWT_SECRET=replace-with-random-32-plus-char-secret
export APP_ENCRYPTION_KEY=replace-with-64-hex-char-aes256-key
export APP_BASE_URL=https://your-frontend-domain
export CORS_ALLOWED_ORIGINS=https://your-frontend-domain

./gradlew :api:bootRun --args='--spring.profiles.active=prod'
```

Build artifact:

```bash
./gradlew :api:bootJar
java -jar api/build/libs/audita-api.jar
```

### Frontend (audita-web)

```bash
cd audita-web
pnpm install --frozen-lockfile
NUXT_PUBLIC_API_BASE=https://your-api-domain pnpm build
pnpm preview
```

For production, run behind Nginx, Caddy, or another reverse proxy with TLS.

## Production deployment with Docker

Use production values only. Do not reuse local defaults.

1. Build images.
2. Push to your registry.
3. Run with production environment.

```bash
docker build -t <namespace>/audita-api:<version> ./audita-api
docker build -t <namespace>/audita-web:<version> ./audita-web

docker push <namespace>/audita-api:<version>
docker push <namespace>/audita-web:<version>
```

Deployment hardening requirements:

- TLS termination in front of web and API
- CORS allowlist set to explicit origins
- Strong JWT and encryption keys
- Backups for PostgreSQL and upload volumes
- Persistent centralized logs and metrics

## Configuration reference

Important backend variables:

| Variable | Purpose |
| --- | --- |
| DATABASE_URL | JDBC URL for PostgreSQL |
| DATABASE_USERNAME | DB username |
| DATABASE_PASSWORD | DB password |
| JWT_SECRET | JWT signing secret (32+ chars) |
| JWT_EXPIRY_SECONDS | Access token TTL |
| REFRESH_TOKEN_EXPIRY_DAYS | Refresh token TTL |
| APP_ENCRYPTION_KEY | 64-char hex AES key for sensitive config values |
| APP_BASE_URL | Frontend base URL for links |
| API_BASE_URL | Public API base URL used for externally visible links where applicable |
| FRONTEND_BASE_URL | Deprecated: SSO endpoints are not implemented in current app surfaces |
| CORS_ALLOWED_ORIGINS | Comma-separated explicit origin allowlist |

Important frontend variables:

| Variable | Purpose |
| --- | --- |
| NUXT_PUBLIC_API_BASE | Browser-visible API base path or URL |
| NUXT_API_INTERNAL_BASE | Internal server-side API target for proxy routes |

## Versioning and release process

Versioning model:

- Semantic Versioning tags with a v prefix (example: v0.1.0)
- v0.x series while APIs are still evolving rapidly
- One release tag per dev -> main merge

Historical commits already pushed:

- Do not tag every commit.
- Tag meaningful release milestones only.

Recommended bootstrap command sequence:

```bash
# choose the milestone commit you want as first release baseline
git tag -a v0.1.0 <commit-sha> -m "Release v0.1.0"
git push origin v0.1.0
```

For additional historical milestones, repeat with v0.2.0, v0.3.0, and so on
only at meaningful cut points.

## CI and Docker publishing

Workflow file: [.github/workflows/ci-release.yml](.github/workflows/ci-release.yml)

Behavior:

- Pull requests targeting main run backend and frontend CI checks.
- Pushes to main also run CI checks.
- When dev is merged into main, CI verifies the merged code, then:
  - computes next SemVer tag from existing tags
  - builds and pushes audita-api and audita-web Docker images
  - tags images as latest, vX.Y.Z, vX.Y, and `sha-<git-sha>`
  - creates or reuses git tag and publishes a GitHub release

Published Docker Hub images:

| Service | Image |
| --- | --- |
| API | [skylarng89/audita-api](https://hub.docker.com/r/skylarng89/audita-api) |
| Web | [skylarng89/audita-web](https://hub.docker.com/r/skylarng89/audita-web) |

Image tags per release: `latest`, `vX.Y.Z`, `vX.Y`, `sha-<git-sha>`

Required GitHub secrets:

- DOCKERHUB_USERNAME
- DOCKERHUB_TOKEN

Optional GitHub variables:

- DOCKERHUB_NAMESPACE (default: `skylarng89`)
- DOCKERHUB_API_IMAGE_NAME (default: `audita-api`)
- DOCKERHUB_WEB_IMAGE_NAME (default: `audita-web`)

## Contributing

Contributions are welcome through forks and pull requests.

Basic flow:

1. Fork the repository.
2. Create a feature branch from dev.
3. Open a pull request to dev.
4. Merge dev into main for release publication.

For detailed contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).

By contributing, you agree your contributions are licensed under the same
license terms in [LICENSE](LICENSE).

## Security baseline

Audita implements a security-oriented baseline (encryption at rest for sensitive stored config, encrypted transport via TLS at the reverse proxy, strong password hashing, and role/tenant-bound authorization).

This project is **not** a SOC 2/ISO/PCI/HIPAA attestation. Customers are responsible for assessing suitability to their own requirements and for operational controls in their deployment environment.

- Use strong secrets in every non-local environment.
- Keep CORS origins explicit and minimal.
- Never expose development SMTP endpoints in production.
- Run automated tests and security scans before release.
- Audit role and tenant boundaries when adding new endpoints.

## Customization & support

White-labeling and custom development are available. Contact: `support@upperloftcreations.com`.

Users can raise issues via GitHub, and maintainers welcome pull requests.

## Project documents

- [docs/PRD.md](docs/PRD.md)
- [docs/SRS.md](docs/SRS.md)
- [docs/USER_FLOW.md](docs/USER_FLOW.md)
- [memory-bank/decisions.md](memory-bank/decisions.md)
- [memory-bank/tech-stack.md](memory-bank/tech-stack.md)
- [memory-bank/docs/tasks.md](memory-bank/docs/tasks.md)
