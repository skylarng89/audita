# Audita — Product Requirements Document (PRD)

**Version:** 1.0.0
**Status:** Draft
**Last Updated:** 2026-04-19

---

## 1. Product Overview

**Audita** is a self-hosted, multi-tenant ITIL/ITSM Change Management solution designed to streamline the lifecycle of IT change requests across organizations. It provides structured approval workflows, real-time collaboration, full audit trails, and role-based access control — all within a secure, extensible platform.

### 1.1 Mission Statement

> Make change management simple, transparent, and auditable for every team — from SMEs to enterprises.

### 1.2 Key Principles

- **Simple by default** — intuitive UI, sensible defaults, minimal friction to raise a change request.
- **Flexible by design** — configurable workflows, custom fields, and roles to fit any organization.
- **Secure & auditable** — every action is logged, every decision is traceable.
- **Extensible** — built to support future integrations (Slack, webhooks, SAML, etc.).

---

## 2. Target Users & Personas

### 2.1 Super Admin
The platform-level administrator who manages the Audita instance. Operates across all tenants (organizations). Typically the IT/DevOps team running the self-hosted instance.

**Goals:** Create/manage organizations; manage global platform settings.

---

### 2.2 Organization Admin
An administrator scoped to a single organization. Responsible for configuring the organization's Audita environment.

**Goals:** Configure workflows, manage users/groups/roles, define custom fields, set global approval policies and SLAs.

---

### 2.3 Requester
A user who raises change requests for review and approval.

**Goals:** Quickly raise a well-documented change request, track its progress, and receive timely notifications on decisions.

---

### 2.4 Approver
A user responsible for reviewing and approving or rejecting change requests.

**Goals:** Review change details clearly, collaborate via comments, and make informed approval decisions.

---

### 2.5 Auditor
A read-only stakeholder who monitors change activity for compliance and reporting purposes.

**Goals:** View all change requests, activity streams, and audit trails without modifying anything.

---

## 3. Feature Summary

### 3.1 Multi-Tenancy & Organization Management

- Super Admin can create and manage multiple Organizations (tenants).
- Each Organization is isolated — users, groups, settings, and change requests are scoped to their organization.
- Super Admin can assign an initial Admin to each Organization at creation time.
- Super Admin has a global view across all organizations for platform management.
- Super Admin can configure **allowed email domains** per tenant to restrict access to specific email domains only.
- Super Admin can enable/disable SSO providers (Google, Microsoft) per tenant.

---

### 3.2 Authentication & Authorization

- **Authentication methods:**
  - Local email/password
  - SSO via **Google** (OIDC)
  - SSO via **Microsoft** / Azure AD (OIDC)
- Users may link their account to an SSO provider in addition to (or instead of) a local password.
- On first SSO login, a user account is auto-provisioned (JIT provisioning) if the email domain is permitted and no matching account exists.
- **Roles:** Each user holds exactly one role. Roles control access within an organization.
- **Built-in Roles:**
  - `Super Admin` — Platform-wide management
  - `Admin` — Organization-wide configuration and management
  - `Requester` — Can create and manage change requests
  - `Approver` — Can review and act on change requests
  - `Auditor` — View-only access across the organization
- **Custom Roles:** Admins can create custom roles with granular permission assignments from a defined permission set.

---

### 3.3 Domain Whitelisting (per Tenant)

- Super Admins can configure one or more **allowed email domains** per tenant (e.g., `acme.com`, `acme.co.uk`).
- When at least one domain is configured, whitelisting is considered **active** for that tenant.
- While whitelisting is active:
  - Only users whose email domain matches an allowed domain may log in (local or SSO).
  - Admins cannot invite users with non-matching email domains.
  - JIT-provisioned SSO users are blocked if their email domain is not on the list.
- Super Admins can add, remove, or deactivate the domain list at any time.
- Removing all domains disables whitelisting (open access restored).
- Domain entries are stored case-insensitively.

---

### 3.3 User & Group Management

- Admins can invite users via email and assign a role.
- Users can belong to multiple groups.
- Groups are freeform — created by Admins and used to organize users.
- Admins can configure default approvers per group, specifying each as **Required** or **Optional**.

---

### 3.4 Change Request Management

#### Core Fields
| Field | Description |
|---|---|
| Title | Short description of the change |
| Description | Detailed explanation (rich text) |
| Priority | Low / Medium / High / Critical |
| Risk Level | Low / Medium / High / Critical |
| Category | Admin-configured categories |
| Scheduled Start Date | Planned start of the change |
| Scheduled End Date | Planned end of the change |
| Affected Systems | List of systems impacted |
| Custom Fields | Admin-defined fields (text, number, date, dropdown, checkbox) |

#### Change Request States
| State | Description |
|---|---|
| `Draft` | Created but not yet submitted |
| `Pending Approval` | Submitted and awaiting approver action |
| `Approved` | All required approvers have approved |
| `Rejected` | Closed due to rejection criteria being met |
| `Cancelled` | Withdrawn by Requester or Admin before closure |

#### SLA & Deadlines
- Admins can configure SLA deadlines globally or per-priority.
- When a deadline is breached, escalation notifications are sent to designated escalation contacts.
- SLA breach status is visible on the change request.

---

### 3.5 Approval Workflows

#### Approval Types
- **Linear** — Approvers must act in sequence. Each approver is notified only after the preceding approver has acted. Requesters/Admins can reorder approvers in a change request to define the sequence.
- **Non-linear** — All approvers are notified simultaneously and can act in any order.

#### Global vs. Per-Request
- Admins configure the default approval type globally.
- Requesters may override the approval type per change request **before any approver has acted**.
- Once any approver records a decision, the approval type is locked for that request.

#### Default Approvers
- Admins predefine a list of default approvers, each marked as **Required** or **Optional**.
- These are automatically added to every new change request.
- Requesters can add ad-hoc approvers beyond the defaults.

#### Rejection Rules
- When an approver rejects, they must provide a mandatory written reason.
- **Single approver scenario:** If the only approver rejects → request is immediately `Rejected` (closed).
- **Multiple approver scenario:** If some reject and some approve → request stays `Pending Approval`. The request closes as `Rejected` only when all required approvers have rejected.
- Approvers may change their decision (approve ↔ reject) at any time, **unless** the request is in a closed state (`Approved`, `Rejected`, or `Cancelled`).
- Admins and Requesters can still change the Required/Optional status of approvers even after approval has started (e.g., when an approver becomes unavailable).

---

### 3.6 Comments & Collaboration

- Rich text comment editor supporting:
  - Formatted text (bold, italic, lists, code blocks)
  - User tagging (`@mention`)
  - File attachments (images, documents)
  - Hyperlink embedding
- Tagged users receive in-app and email notifications.
- Comments are appended to the change request's activity stream.

---

### 3.7 File Uploads

- Supported in change request descriptions and comments.
- Storage: configurable to **local disk** or **S3-compatible object storage** (AWS S3, MinIO, etc.).
- File type and size limits configurable by Admin.

---

### 3.8 Activity Stream & Audit Trail

#### Per-Request Activity Stream
Every change request has an immutable, time-ordered activity stream that records:
- Change request field edits
- Approver additions/removals/reordering
- Approval decisions (approve/reject/change decision) with reasons
- Comments and file uploads
- Status transitions
- Settings changes (approval type, Required/Optional changes)
- SLA breach events

#### Global Audit Trail
- Admins and Auditors can access an organization-wide audit log.
- Searchable and filterable by: user, action type, date range, change request ID.
- Entries are immutable and cannot be deleted.

---

### 3.9 Notifications

#### In-App Notifications
- Real-time bell notifications visible in the application header.
- Notification feed with read/unread states.

#### Email Notifications
Triggered on:
- New change request submitted (notify approvers)
- Approval decision made (notify requester and other approvers in linear flows)
- Comment posted / user mentioned
- Approver added/removed from a request
- SLA deadline approaching / breached
- Status change (Approved / Rejected / Cancelled)
- Linear workflow: next approver in sequence is notified when their turn begins

---

### 3.10 Admin Configuration Panel

Admins can configure:
- Organization profile (name, logo, timezone)
- Global approval type (Linear / Non-linear)
- Default approvers and their Required/Optional status
- Custom roles and permissions
- Custom change request fields (type, label, required/optional, display order)
- Change request categories
- SLA policies (global and per-priority)
- File upload settings (max size, allowed types, storage backend)
- Email settings (SMTP configuration)
- Escalation contacts per SLA policy

---

## 4. Non-Functional Requirements

| Category | Requirement |
|---|---|
| **Performance** | Change request list loads in < 2s for up to 10,000 records per org |
| **Security** | Passwords hashed (bcrypt), HTTPS enforced, JWT + refresh token auth |
| **Scalability** | Horizontal scaling supported; stateless backend |
| **Availability** | Designed for 99.9% uptime in self-hosted environments |
| **Auditability** | All audit trail entries are immutable |
| **Accessibility** | WCAG 2.1 AA compliance target |
| **Internationalisation** | UI strings externalised for future i18n support |

---

## 5. Out of Scope (v1.0)

- SAML / LDAP authentication
- Slack / MS Teams / webhook integrations
- Mobile native apps (responsive web only)
- Change request templates
- Reporting dashboards / analytics
- API access tokens for external integrations
- Multi-language UI
- Automated approval rules (e.g., auto-approve after N days)

---

## 6. Success Metrics

| Metric | Target |
|---|---|
| Time to raise a change request | < 3 minutes |
| Time to first approver notification | < 30 seconds after submission |
| Audit trail completeness | 100% of state-changing actions logged |
| User onboarding (Admin setup to first CR) | < 15 minutes |
