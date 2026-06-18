# Audita — Technology Stack

**Last Updated:** 2026-06-15

---

## Backend (`audita-api`)

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 25 |
| Framework | Spring Boot | 4.1.0 |
| ORM | Hibernate | 7.4.1 |
| Validation | Hibernate Validator | 9.1.0 |
| Database | PostgreSQL | 18.3 |
| Connection Pool | HikariCP | — |
| Migrations | Flyway | — |
| Auth | JWT (jjwt) | 0.12.6 |
| HTML Sanitizer | OWASP Java HTML Sanitizer | 20260101.1 |
| Email | JavaMailSender + Thymeleaf | — |
| Real-time | SSE (Server-Sent Events) | — |
| Build | Gradle (Kotlin DSL) | — |
| Runtime Image | eclipse-temurin:25 (DHI hardened) | — |

## Frontend (`audita-web`)

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Nuxt | 4.4.8 |
| UI Library | Vue | 3.5.38 |
| State | Pinia | 3.0.4 |
| CSS | Tailwind CSS | 4.3.0 |
| TypeScript | TypeScript | 6.0.3 |
| Rich Text | TipTap | 3.26.1 |
| Date Picker | Flatpickr | 4.6.13 |
| Testing | Vitest | 4.1.8 |
| Security Headers | nuxt-security | 2.6.0 |
| Runtime Image | dhi.io/node:24 | — |

## Infrastructure

| Layer | Technology |
|-------|-----------|
| CI/CD | GitHub Actions |
| Container Registry | Docker Hub (`skylarng89/audita-api`, `skylarng89/audita-web`) |
| Orchestration | Docker Compose (dev/prod) |
| Reverse Proxy | Nginx 1.24.0 |
| Image Tags | `latest`, `X.Y.Z`, `X.Y`, `sha-<git-sha>` |

## Versioning

- Semantic versioning without "v" prefix (e.g., `1.2.0`)
- CI auto-computes version from conventional commits on push to `main`
- Image tag includes version via `build-args: APP_VERSION`
