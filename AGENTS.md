# AGENTS.md — Engineering & AI Code Generation Guide

**Version:** 2026.1 | **Ref:** NIST CSF 2.0, OWASP Top 10, PCI-DSS v4.0  
**Updated:** 2026-03-26

---

## Core Principle

Write code like a trusted staff-level engineer who owns this system long-term.  
Every line is intentional, clear, and easy to change.

**Priorities (in order):** Correctness → Safety/Security → Maintainability → Scalability → DX

Document tradeoff decisions in `memory-bank/docs/decisions.md`.

---

## Agent Behaviour

- Act as both **Project Manager and Software Architect**: plan before implementing
- **Show the current task checklist** at the start of each response on multi-step tasks
- Maintain `memory-bank/tasks.md` and `memory-bank/plan.md` throughout; never let them drift
- Batch related file reads/writes/edits; lazy-load only relevant memory-bank files
- Use **Context7 MCP** for library/language docs; **Brave Search MCP** for current web info
- On session start: scan for all `memory-bank/` folders; ingest and synthesise
- If no memory-bank exists, ask the user whether to create one
- On "Update session": locate and update **all** memory-bank folders in the workspace

---

## Code Quality

- Functions ≤30 lines (soft max 50); nesting ≤3 levels
- Names reveal intent: `activeUserIndex` not `data`, `retryBackoffMs` not `val`
- One clear responsibility per module/file
- Comments explain **why** and tradeoffs — not what the code does
- Document assumptions, invariants, and constraints; link to tickets/decisions where non-obvious
- No noise comments, no over-generalisation, no defensive code for impossible states
- Prefer pure functions; avoid boolean flag parameters — split into two named functions instead
- Functions must have consistent return types — never return `string | null | undefined` arbitrarily
- Call transactional methods via an injected dependency instead of directly via 'this'.
- Replace generic exceptions with specific library exceptions or a custom exception.
- MD036/no-emphasis-as-heading: Emphasis used instead of a heading
- MD040/fenced-code-language: Fenced code blocks should have a language specified

---

## Code Style

### Conditionals

Prefer expanded conditionals over guard clauses — guard clauses obscure flow when nested.

```ruby
# Ruby example — principle applies across languages
# Preferred
def process(ids)
  if ids
    records.find(ids)
  else
    []
  end
end

# Guard clause acceptable only when:
# (1) it's at the very top of the method, AND
# (2) the remaining body is non-trivial (several lines)
def after_commit(recording)
  return if recording.parent.was_created?

  if recording.was_created?
    broadcast_new(recording)
  else
    broadcast_change(recording)
  end
end
```

### Method & Member Ordering

Order class members consistently:

1. Class/static methods
2. Public methods — `initialize`/constructor first
3. Private/protected methods

Order methods **vertically by invocation order** — a method should appear before the methods it calls. This makes call flow readable top-to-bottom without jumping around.

### Naming Conventions

- **Bang methods** _(Ruby)_: use `!` only when a non-bang counterpart exists. Do not use `!` to signal destructive or dangerous operations — that's not its purpose.
- **Async suffixes** _(any language with job/queue patterns)_: use `_later` for methods that enqueue work; use `_now` for the synchronous equivalent called by the job.

### Visibility Modifiers _(Ruby)_

No blank line after the modifier; indent content beneath it.

```ruby
class SomeClass
  def public_method
  end

  private
    def private_method_1
    end

    def private_method_2
    end
end
```

Exception — module with only private methods: mark `private` at top, add one blank line after, no indent.

```ruby
module SomeModule
  private

  def some_private_method
  end
end
```

### REST Resource Modelling _(web frameworks)_

Model endpoints as CRUD on resources. When an action doesn't map to a standard verb, introduce a new resource — don't add custom actions.

```ruby
# Bad
resources :cards do
  post :close
end

# Good
resources :cards do
  resource :closure
end
```

### Controller & Domain Layer _(MVC frameworks)_

Keep controllers thin — they orchestrate, they don't implement. Business logic lives in the domain model.

```ruby
# Simple case: direct Active Record call is fine
def create
  @comment = @card.comments.create!(comment_params)
end

# Complex case: intention-revealing model API
def create
  @card.gild
end
```

Services/form objects are acceptable when justified — don't treat them as mandatory infrastructure.

### Async Jobs _(job queue patterns — Rails, Spring, etc.)_

Job classes should be shallow — delegate logic to the domain model, don't implement it in the job.

```ruby
# Job just calls the model
class Event::RelayJob < ApplicationJob
  def perform(event)
    event.relay_now   # logic lives here, not in the job
  end
end
```

---

## Testing & Error Handling

- ≥80% coverage on critical modules; ≥70% elsewhere
- Name tests by behaviour: `should_reject_expired_token`; regression test every bug fix
- Fail fast with context: what failed, why, how to recover
- Typed/structured errors; differentiate user, system, and programmer errors
- Structured logging (key-value); never log secrets/PII; appropriate log levels throughout

---

## Performance

- Lazy load; Brotli/Gzip; CDN-served static assets
- Code splitting, tree-shaking, minification — non-negotiable
- HTTP/3 + binary protocols (Protobuf/gRPC) for high-frequency internal calls
- Targets: LCP < 2.5s (web); API p95 < 100ms

---

## Security Baseline

### Identity & Encryption

- FIDO2/Passkeys everywhere — no SMS OTP; UEBA for behavioural drift detection
- AES-256 at rest; TLS 1.3 in transit (including internal service-to-service)
- Zero Trust — verify every identity and device; no implicit trust
- Validate, sanitise, and type all inputs server-side; never trust client data
- Secrets in hardware/cloud vaults (KMS, Vault) — never hardcoded or logged

### Platform

- Frontend: strict CSP, HSTS, `X-Frame-Options: DENY`, SameSite + HttpOnly cookies; SRI hashes on all 3rd-party assets
- Backend/API: RBAC/ABAC with least-privilege; rotate JWTs frequently; WAF + adaptive rate limiting + egress filtering
- Mobile: Secure Enclave/Keystore biometrics; certificate pinning; root/jailbreak detection; minimal permissions

### Pre-ship Security Checklist

- [ ] All external input validated, sanitised, typed
- [ ] Secrets in env/config stores — never in code or logs
- [ ] Auth middleware on all protected routes; resource-level authorisation enforced
- [ ] No sensitive data in logs, metrics, or error messages
- [ ] Dependencies pinned; risky libs noted in `tech-stack.md`
- [ ] TLS in transit; encryption at rest where applicable
- [ ] Secure cookie defaults; CSRF protection where relevant
- [ ] No unsafe eval, dynamic code execution, or direct shell calls unless justified

---

## Resilience & Integrity

- Circuit Breakers + Bulkheads + exponential backoff with jitter
- ACID transactions + idempotency keys on all state-mutating endpoints
- Multi-zone/region with automated health-checked failover
- Backups: 3-2-1-1 — 3 copies, 2 media types, 1 offsite, 1 air-gapped/immutable

---

## CI/CD & Observability

- **Pipeline gates:** SAST + DAST + SCA must pass before merge/deploy
- **Deployments:** Canary or Blue-Green only; automated rollback on error-threshold breach
- **Tracing:** OpenTelemetry end-to-end; structured JSON logs with correlation IDs
- **Alerting:** Monitor error rate and latency trends — not just uptime
- **SBOM:** Generate and attach to every build artifact

---

## Incident Response

- SOAR playbooks for automated isolation of compromised nodes
- Pre-approved emergency protocols; quarterly breach simulation drills
- Human escalation path documented and tested

---

## ⚠️ Financial Engineering — Non-Negotiables

> These are the patterns most engineers skip. Each is a proven source of financial loss, audit failures, or exploitable vulnerabilities.

### 1. Money Representation

**Never use `float`/`double` for monetary values.** IEEE 754 cannot exactly represent most decimal fractions — reconciliation failures at scale are inevitable.

| Language   | Correct Type                                   |
| ---------- | ---------------------------------------------- |
| Java       | `BigDecimal`                                   |
| JS/TS      | `decimal.js`, `big.js`, or integer minor units |
| C#         | `decimal` (128-bit)                            |
| Python     | `decimal.Decimal`                              |
| Go         | `shopspring/decimal`                           |
| PostgreSQL | `NUMERIC(precision, scale)` — never `FLOAT`    |

- Store amounts as **integer minor units** where possible
- Use `HALF_EVEN` (Banker's rounding) for regulatory compliance
- Always store currency alongside every amount — never infer it

---

### 2. Atomicity & Transaction Integrity

Every financial mutation must be atomic. Partial success = corrupted data.

- Wrap multi-step operations (debit + credit + ledger) in a **single DB transaction**
- Distributed flows: use the **Saga pattern** with compensating transactions
- Always `SELECT ... FOR UPDATE` before reading and mutating — never bare `SET balance = balance - X`
- Validate pre-conditions (e.g. sufficient balance) **inside** the same transaction

```sql
BEGIN;
  SELECT balance FROM accounts WHERE id = $1 FOR UPDATE;
  UPDATE accounts SET balance = balance - $2 WHERE id = $1;
  INSERT INTO ledger_entries (...) VALUES (...);
COMMIT;
```

**Rollback on failure is mandatory:**

- Wrap transaction body in try/catch; explicitly ROLLBACK on any exception
- Never swallow exceptions inside a transaction
- Log before rollback: operation, user ID, amount, correlation ID, error

```js
BEGIN;
try {
  // all financial operations
  COMMIT;
} catch (error) {
  ROLLBACK;
  log.error({ op, userId, amount, correlationId, error });
  throw error;
}
```

**Framework gotchas:**

| Framework               | Gotcha                                                                | Fix                                              |
| ----------------------- | --------------------------------------------------------------------- | ------------------------------------------------ |
| Spring `@Transactional` | Only rolls back on `RuntimeException` — checked exceptions **commit** | Add `rollbackFor = Exception.class`              |
| Django ORM              | Catching exceptions without re-raising doesn't rollback               | Re-raise or use `transaction.set_rollback(True)` |
| Sequelize               | Unhandled rejections may not rollback managed transactions            | Always `await t.rollback()` in catch             |
| GORM                    | Returned errors don't rollback; panics do                             | Use `db.Transaction()` helper                    |

---

### 3. Race Condition Prevention (TOCTOU)

The window between checking a balance and acting on it is the attack surface.

- Never perform check → act as separate DB calls — must be atomic
- Use **database row locks** (`SELECT FOR UPDATE`) — application-level locks are insufficient
- Use **distributed locks** (Redis `SETNX` / Redlock) when coordinating across services
- Apply **serialisable isolation** where full consistency is required
- Flag concurrent identical requests from the same user within <100ms
- Write explicit race condition tests with parallel requests (`k6`, `hey`, concurrent `curl`)

---

### 4. Idempotency

Network retries are inevitable. Without idempotency, retries create duplicate transactions.

- Every state-mutating endpoint must accept and honour an idempotency key
- Client generates key (UUID v4); server stores result with TTL (24–72 hrs)
- On duplicate key: return the **original response** — do not re-execute
- Lock the idempotency record atomically before processing (`SET NX` in Redis)
- Scope by: `user_id + operation_type + idempotency_key`
- Apply to: payments, transfers, refunds, wallet credits, ledger writes, webhook deliveries

```js
Header: X-Idempotency-Key: <client-generated UUID v4>
```

---

### 5. Double-Entry Bookkeeping

- Every transaction = two ledger entries: one debit, one credit — always balanced
- Sum of all ledger entries must always equal zero — enforce as an invariant
- Ledger is **append-only** — no updates, no deletes; reversals are new entries
- Stored balance fields are **caches** — always reconcilable against the ledger
- Run automated reconciliation jobs on schedule; alert on any discrepancy > 0

---

### 6. Velocity Controls & Abuse Prevention

- Enforce per-transaction, hourly, and daily limits — per user and per account
- Enforce frequency limits: max N transactions per minute per account
- All limits are server-side — never trust client-supplied limits or amounts
- Flag and hold (do not silently fail) breached transactions — always log them
- Apply progressive friction on anomalous patterns: step-up auth, manual review queue

---

### 7. Immutability & Audit Trail

- Transactions, ledger entries, and audit logs are **append-only** — deny UPDATE/DELETE at DB level for the app service account
- Every record must carry: `created_at`, `created_by`, `correlation_id`, `source_ip`, `request_id`
- Store **pre-state and post-state** on every balance mutation
- Use **event sourcing** where practical: current state = replay of all events
- Audit log retention: minimum 7 years for regulated financial data

---

### 8. SOLID in Fintech

| Principle                     | What It Prevents                                                                     |
| ----------------------------- | ------------------------------------------------------------------------------------ |
| **S** — Single Responsibility | Don't mix payment processing, ledger, notifications, and reconciliation in one class |
| **O** — Open/Closed           | New payment providers/currencies extend — never modify — existing transaction logic  |
| **L** — Liskov Substitution   | All payment gateway implementations must be interchangeable                          |
| **I** — Interface Segregation | Reconciliation services must not depend on payment capture interfaces                |
| **D** — Dependency Inversion  | Business logic depends on abstractions — never on concrete SDKs or DB drivers        |

---

### 9. Concurrency & State Safety

- Financial service methods must be **stateless** — all state lives in the DB
- Never cache account balances without an explicit TTL and invalidation strategy
- Optimistic locking (version columns) for low-contention; pessimistic (`FOR UPDATE`) for high-contention writes
- Queue-based flows: require **exactly-once delivery** (Kafka transactions, SQS deduplication IDs)
- Never generate financial reference IDs in the application layer — use atomic DB sequences or distributed ID generators (Snowflake, ULID)

---

### 10. Failure Handling & Partial State Recovery

- Define the **failure state** for every financial operation before the success path
- Use the **Outbox pattern**: write event to outbox table in the same transaction as state change; publish asynchronously
- Treat any external payment API call without a confirmed response as **potentially succeeded** — always query status before retrying
- Expose a **status check endpoint** for every async financial operation
- Dead-letter all failed events; alert immediately; require explicit manual resolution
- No transaction remains unresolved beyond a defined SLA timeout

---

### 11. Numeric Boundary & Overflow Safety

- Validate min/max bounds on all monetary inputs before processing
- Use `NUMERIC(19, 4)` or wider in DB; know your ceiling and test at it
- Explicitly reject: zero-value transactions, negative amounts, non-numeric input, currency mismatches
- Validate in the application layer first — do not rely solely on DB constraints

---

### 12. Sensitive Financial Data Handling

- Mask account numbers, card PANs, and balances in all logs, errors, and stack traces
- API responses return masked or tokenised forms — never raw account numbers or card data
- Field-level encryption for PAN, BVN, NIN, account numbers at rest
- Enforce need-to-know: payment processors receive tokens, not full card data
- Tokenise as early as possible — keep raw card data out of your system to minimise PCI-DSS scope

---

## Financial Engineering Pre-Ship Checklist

- [ ] Monetary values use correct decimal/integer type — zero float/double
- [ ] Balance mutations wrapped in atomic transactions with row-level locking
- [ ] Idempotency key accepted, stored, and honoured on all mutating endpoints
- [ ] Double-entry ledger entries created for every transaction
- [ ] Race condition test written (concurrent requests) and passing
- [ ] Velocity and frequency limits enforced server-side
- [ ] Audit trail is append-only with full context fields
- [ ] Failure and partial-state recovery path defined, implemented, and tested
- [ ] No sensitive financial data in logs, errors, or API responses
- [ ] New operation type covered by the reconciliation job
- [ ] SOLID boundaries respected — no mixed concerns in transaction logic
- [ ] Numeric boundary tests: zero, negative, max, non-numeric, currency mismatch

---

## Industry-Specific Requirements

| Sector         | Non-Negotiable                                                         |
| -------------- | ---------------------------------------------------------------------- |
| **Finance**    | PCI-DSS v4.0; ledger reconciliation; transaction velocity limits       |
| **E-Commerce** | Server-side price validation; inventory locking; bot mitigation        |
| **Health**     | PHI encryption; role-contextual audit logs; break-glass access         |
| **SaaS**       | Tenant data isolation; SCIM provisioning; customer-managed keys (CMEK) |

---

## 2026 Non-Negotiables

- [ ] FIDO2 on all auth flows — SMS OTP is phishable by AI agents
- [ ] Air-gapped immutable backup — only reliable ransomware recovery path
- [ ] SOAR automation — human response speed cannot match AI-speed attacks
- [ ] SBOM + SCA in every pipeline — supply chain is the new perimeter
- [ ] Zero Trust enforced — no lateral movement on credential compromise

---

## AI Defense Stack

| Layer       | Control                                    |
| ----------- | ------------------------------------------ |
| Perimeter   | Cloudflare WAF + Bot Management            |
| Identity    | FIDO2 + UEBA                               |
| Application | RASP (Runtime Self-Protection)             |
| Endpoint    | Behavioral EDR (CrowdStrike / SentinelOne) |
| Network     | ZTNA                                       |
| Detection   | AI-augmented SIEM + automated SOAR         |

---

## Memory Bank

Single source of truth for project context, continuity, and decisions.

```yaml
memory-bank/
├── context.md           # Active state: current tasks, blockers, focus
├── decisions.md         # Append-only: architectural and non-obvious decisions
├── patterns.md          # Code idioms, abstractions, naming and testing patterns
├── design.md            # UI/UX, design tokens, component system, flows
├── changelog.md         # Semantic version history with dates
├── tech-stack.md        # Languages, frameworks, libs, versions, tools
├── sessions/            # Session logs (one file per day: YYYY-MM-DD.md)
└── docs/
    ├── tasks.md          # Task lists, roadmaps, work breakdowns (see format below)
    ├── specs/            # Feature specs, API contracts, user stories
    ├── designs/          # UX flows, component inventories, design rationale
    ├── decisions/        # Long-form explanations linked from decisions.md
    └── misc/             # Uncategorised (curate periodically)
```

---

### `tasks.md` Format

Every `tasks.md` must follow this structure exactly so any AI agent can parse, update, and reason over it consistently.

#### File Header

```markdown
# <Project Name> - Developer Task List

**Project:** <Project name and short descriptor>
**Version:** <semver>
**Last Updated:** <YYYY-MM-DD>
**Team Size:** <N> Developers
```

#### Status Legend

Always include this block verbatim — agents use it as the reference for status transitions.

```markdown
## Task Status Legend

- 🔴 **Not Started** — Task has not been started
- 🟡 **In Progress** — Task is currently being worked on
- ✅ **Completed** — Task is finished and tested
```

#### Task Tables

Group tasks under sprint and sub-feature headings. Each group gets its own table.

```markdown
## Sprint <N>: <Sprint Name> (<Week Range>)

### <Feature or Module Name>

| Task ID    | Task                        | Priority | Status         | Assigned To | Notes                           |
| ---------- | --------------------------- | -------- | -------------- | ----------- | ------------------------------- |
| PREFIX-001 | <Concise imperative phrase> | High     | 🔴 Not Started | Developer 1 | <Implementation detail or ref>  |
| PREFIX-002 | <Concise imperative phrase> | Medium   | 🟡 In Progress | Developer 2 | <Blocker or dependency note>    |
| PREFIX-003 | <Concise imperative phrase> | High     | ✅ Completed   | Developer 3 | <What was done / files changed> |
```

**Rules:**

- **Task ID**: uppercase prefix reflecting the module/domain + zero-padded sequence (e.g. `AUTH-001`, `DB-012`, `INIT-005`)
- **Task**: imperative verb phrase — "Implement JWT refresh endpoint", not "JWT refresh"
- **Priority**: `High` / `Medium` / `Low`
- **Status**: emoji + label from the legend — never abbreviate or deviate
- **Assigned To**: `Developer N` or a name — always populated, never blank
- **Notes**: implementation detail, file reference, blocker, or dependency — never empty; use `—` if truly nothing to add

#### Progress Tracking

Append this section after all sprint tables and keep it updated.

```markdown
## Progress Tracking

### Overall Progress by Sprint

| Sprint    | Total Tasks | Not Started | In Progress | Completed | Progress % |
| --------- | ----------- | ----------- | ----------- | --------- | ---------- |
| Sprint 0  | 10          | 2           | 1           | 7         | 70%        |
| **TOTAL** | **10**      | **2**       | **1**       | **7**     | **70%**    |

### Progress by Developer

| Developer   | Assigned Tasks | Not Started | In Progress | Completed | Progress % |
| ----------- | -------------- | ----------- | ----------- | --------- | ---------- |
| Developer 1 | 5              | 1           | 0           | 4         | 80%        |
```

#### Recent Implementations

Log significant completions here. Agents append to this section — never overwrite previous entries.

```markdown
## Recent Implementations

### <Feature Name> (Completed <YYYY-MM-DD>)

**Overview**: <One sentence summary>

**Files Created/Modified**:

- `path/to/file.ts` — <what it does>

**Key Changes**:

- <Bullet per significant change>

**Test Coverage**: <N> tests passing
```

#### Status Transition Rules

- 🔴 → 🟡 when work begins
- 🟡 → ✅ only after tests are written and passing
- Never mark ✅ without updating the Notes column to reflect what was done
- Update progress tables every time any status changes
