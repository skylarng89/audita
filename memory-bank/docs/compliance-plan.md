# Audita - Compliance Readiness Plan (Non-Attestation)

## Scope
- This repo can be used to build a service that may be assessed against common security/compliance frameworks.
- This file is a **readiness and evidence plan**, not an SOC 2/ISO attestation.

## Deployment model assumption (important)
- **Self-hosted only**: customers run Audita on their own infrastructure.
- You do not provide managed hosting, uptime guarantees, incident response operations, or employee access for customer environments.
- Compliance evidence is therefore shared:
  - **In-repo/app responsibilities (you control)**: security design, code behavior, shipped configuration defaults/tooling.
  - **Customer environment responsibilities (they control)**: IAM/MFA for their operators, network perimeter hardening, backup/DR execution and restore testing, vulnerability patch cadence, and their incident response process.

## Baseline frameworks commonly requested in 2026
- SOC 2 (AICPA Trust Services Criteria)
- ISO/IEC 27001 (security management system controls)
- GDPR / UK GDPR (if processing EU/UK personal data)
- CCPA/CPRA (if selling/sharing Californian personal data)

## Evidence you control (in code / repo)
### Security program artifacts
- Documented threat modeling process (architecture + per-feature)
- Security policies: password/auth policy, access control policy, change control policy
- Secure SDLC: code review expectations, dependency scanning, SAST/DAST pipeline evidence

### Operational evidence
- Incident response playbook + roles
- Logging/monitoring policy (what is logged, retention, access to logs)
- Backup/restore strategy and tests
- Vulnerability management workflow (triage, patch SLAs)

### Access control evidence
- RBAC/authorization tests (controller-level and service-level)
- Periodic access review process (procedural evidence)
- Tenant isolation tests (BOLA / IDOR regression coverage)

### Data protection evidence
- Encryption at rest for secrets and sensitive fields
- Encryption in transit (TLS) and secure cookie settings
- Data retention + deletion semantics (product behavior)

## Evidence customers control (shared responsibility)
- IAM/MFA + least-privilege for operators and business users
- Access review cadence and documentation
- Host/network hardening (patching, endpoint controls)
- Backup/restore execution, restore testing, and retention schedules
- Incident response operations and breach notification workflows
- Third-party/vendor risk management in their environment

## Repo findings (current)
- Security engineering work present: JWT + HttpOnly refresh cookies, structured logging guidance, tenant isolation intent, immutable audit log approach.
- SSO is currently removed from the app surfaces.
- No SOC 2 report / ISO certificate / privacy notice artifacts found in-repo.

## Next actions (repo + operational)
1. Add documented incident response and breach notification procedures (template + ownership).
2. Add log retention and access policy docs (who can access logs; for how long).
3. Add data retention/deletion semantics docs for: audit logs, user data, tokens, attachments.
4. Add a risk register mapping to SOC 2 TSC categories (Security + optional criteria).
5. Add an “evidence checklist” folder layout for auditors (screenshots, CI logs, scan outputs, runbooks).

## HIPAA / sector-specific posture
- Not automatically achievable from code alone.
- Provide a feature flag + documented configuration path only after a concrete HIPAA scoping decision (BAA, PHI handling, audit controls, breach workflow, etc.).
