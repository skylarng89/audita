# Audita — User Flow Document

**Version:** 1.0.0
**Status:** Draft
**Last Updated:** 2026-04-19

---

## 1. Flow Conventions

- **[Page]** — A distinct screen or view.
- **[Modal]** — An overlay dialog.
- **[Action]** — A user-triggered event.
- **[System]** — An automated system response.
- **→** — Navigates to or results in.

---

## 2. Platform Bootstrap (First Run)

```
Application First Launch
        │
        ▼
[Setup Wizard — Step 1: Super Admin Registration]
  - Full name, email, password
        │
        [Action: Complete Setup]
        │
        ▼
[System: Super Admin account created; setup wizard marked complete]
        │
        ▼
[Super Admin Dashboard]
```

> Subsequent visits skip the wizard and go directly to [Login Page].

---

## 3. Authentication Flows

### 3.1 Login
```
[Login Page]
  - Email + Password input
  - "Sign in with Google" button
  - "Sign in with Microsoft" button
  - "Forgot Password?" link
        │
        ├── [Action: Sign In (local)]
        │         │
        │         ├── Invalid credentials → [Error inline]
        │         ├── Domain not whitelisted → [Error: "Your email domain is not permitted for this organization"]
        │         └── Valid → [Session created] → [Role-based redirect]
        │
        └── [Action: SSO (Google / Microsoft)]
                  │
                  ▼
        [Redirect to Google/Microsoft OIDC]
                  │
                  [OAuth consent + login at provider]
                  │
                  ▼
        [Callback to /api/v1/auth/oauth/{provider}/callback]
                  │
                  ├── Domain not whitelisted → redirect to [Login Page] with error
                  ├── Account exists → [Session created] → [Role-based redirect]
                  └── Account does not exist (JIT) → account auto-provisioned with default role
                            → [Role-based redirect]
```

Role-based redirect targets:
- Super Admin → [Super Admin Dashboard]
- Admin → [Admin Dashboard]
- Requester / Approver / Auditor → [Change Requests List]

### 3.2 Forgot Password
```
[Login Page]
        │
        [Action: Forgot Password]
        │
        ▼
[Forgot Password Page]
  - Email input
        │
        [Action: Send Reset Link]
        │
        ▼
[System: Password reset email sent if email exists]
        │
        ▼
[Success message shown]
        │
        [User clicks link in email]
        │
        ▼
[Reset Password Page]
  - New password + confirm
        │
        [Action: Reset Password]
        │
        ├── Validation error → inline error
        └── Success → [Login Page] with success toast
```

---

## 4. Super Admin Flows

### 4.1 Create Organization
```
[Super Admin Dashboard]
        │
        [Action: Create Organization]
        │
        ▼
[Modal: New Organization]
  - Organization name
  - Subdomain / slug
  - Initial Admin: name, email
        │
        [Action: Create]
        │
        ▼
[System: Org created; Admin user created; invite email sent to Admin]
        │
        ▼
[Organizations List] — org appears with status "Active"
```

### 4.3 Configure Tenant Domain Whitelist
```
[Super Admin Dashboard → Organizations List]
        │
        [Action: Manage Org → Domain Whitelist tab]
        │
        ▼
[Domain Whitelist Page]
  - Current allowed domains list (e.g., "acme.com", "acme.co.uk")
  - Whitelisting status: Active (≥1 domain) | Inactive (no domains)
        │
        ├── [Action: Add Domain]
        │     → [Modal: Add Allowed Domain]
        │         - Domain input (e.g., "acme.com")
        │         - Validation: must be valid domain format
        │         [Action: Add]
        │         → Domain added; whitelisting auto-activates if first entry
        │
        └── [Action: Remove Domain]
              → Confirmation modal
              → Domain removed
              → If last domain removed → whitelisting deactivates
              → [System: Log change to audit trail]
```

### 4.4 Configure SSO per Tenant
```
[Super Admin Dashboard → Organizations → SSO Settings tab]
        │
        ▼
[SSO Configuration Page]
  - Google SSO toggle (enable/disable)
      - Client ID, Client Secret fields (if enabled)
  - Microsoft SSO toggle (enable/disable)
      - Tenant ID, Client ID, Client Secret fields (if enabled)
        │
        [Action: Save]
        │
        ▼
[System: SSO config stored; Login page for that tenant shows SSO buttons if enabled]
```

---

## 5. Admin Flows

### 5.1 Organization Setup (First-time Admin)
```
[Admin receives invite email]
        │
        [Action: Accept Invite]
        │
        ▼
[Set Password Page]
        │
        ▼
[Admin Dashboard — Setup Checklist visible]
  Recommended steps:
  1. Configure Organization Profile
  2. Set Global Approval Type
  3. Create Groups
  4. Invite Users
  5. Configure Default Approvers
  6. Configure Custom Fields
  7. Configure SLA Policies
  8. Configure Email (SMTP)
```

### 5.2 Invite Users
```
[Admin → Users → Invite User]
        │
        ▼
[Modal: Invite User]
  - Email address
  - Full name
  - Role (dropdown: built-in + custom roles)
  - Group(s) (multi-select, optional)
        │
        [Action: Send Invite]
        │
        ├── Domain whitelisting active + email domain not allowed
        │     → [Error: "Email domain not permitted for this organization"]
        │
        └── Domain allowed (or whitelisting inactive)
              → [System: Invite email sent; user record created with "Pending" status]
              → [User receives email → Accept Invite → Set Password Page]
              → [User status → "Active"]
```

### 5.3 Manage Roles & Permissions
```
[Admin → Settings → Roles]
        │
        ├── View built-in roles (read-only display)
        │
        └── [Action: Create Custom Role]
                │
                ▼
        [Page: New Role]
          - Role name, description
          - Permission toggles (grouped by module):
              Users, Groups, Change Requests, Approvals,
              Settings, Audit Trail, Reports
                │
                [Action: Save Role]
                │
                ▼
        [Roles List] — new role visible, assignable to users
```

### 5.4 Configure Default Approvers
```
[Admin → Settings → Default Approvers]
        │
        ▼
[Default Approvers Page]
  - Existing approver list (ordered for Linear)
        │
        ├── [Action: Add Approver]
        │     → [Modal: Add Default Approver]
        │         - Search & select user
        │         - Set Required / Optional
        │         [Action: Add]
        │
        ├── [Action: Drag to Reorder] (for Linear workflows)
        │
        └── [Action: Remove Approver] → confirmation → removed
```

### 5.5 Configure Custom Fields
```
[Admin → Settings → Custom Fields]
        │
        ▼
[Custom Fields Page]
  - Existing fields list
        │
        [Action: Add Field]
        │
        ▼
[Modal: New Custom Field]
  - Label
  - Field type: Text / Number / Date / Dropdown / Checkbox
  - Options (if Dropdown)
  - Required / Optional
  - Display order
        │
        [Action: Save]
        │
        ▼
[Field appears on all new change request forms]
```

### 5.6 Configure SLA Policies
```
[Admin → Settings → SLA Policies]
        │
        ├── [Action: Create Policy]
        │     → [Modal: SLA Policy]
        │         - Name
        │         - Priority trigger (Low / Medium / High / Critical / All)
        │         - Deadline duration (hours/days)
        │         - Escalation contacts (user select, multi)
        │         - Escalation notification timing (e.g., 2h before breach)
        │
        └── [Action: Edit / Delete existing policy]
```

### 5.7 Configure Global Approval Type
```
[Admin → Settings → Workflow]
        │
        [Toggle: Approval Type = Linear | Non-linear]
        │
        [Action: Save]
        │
        ▼
[System: All new change requests default to this type]
```

---

## 6. Requester Flows

### 6.1 Create Change Request
```
[Change Requests List]
        │
        [Action: New Change Request]
        │
        ▼
[Change Request Form — Step 1: Details]
  - Title *
  - Description (rich text) *
  - Priority (Low/Medium/High/Critical) *
  - Risk Level *
  - Category *
  - Scheduled Start Date
  - Scheduled End Date
  - Affected Systems
  - Custom Fields (as configured)
  - File attachments (drag & drop)
        │
        [Action: Next → Step 2: Approvers]
        │
        ▼
[Change Request Form — Step 2: Approvers]
  - Default approvers pre-populated (Required/Optional labeled)
  - [Action: Add Ad-hoc Approver] → search users → select → set Required/Optional
  - [Action: Reorder approvers] (drag, if Linear mode)
  - [Action: Toggle approval type override] (Linear / Non-linear)
        │
        [Action: Save as Draft] → [Draft Change Request Detail Page]
        [Action: Submit] → confirmation modal
        │
        ▼
[System: CR created in "Pending Approval" state]
[System: Notification sent to all approvers (or first approver if Linear)]
        │
        ▼
[Change Request Detail Page]
```

### 6.2 Manage an Existing Change Request
```
[Change Requests List]
        │
        [Action: Click CR row]
        │
        ▼
[Change Request Detail Page]
  Sections:
  ├── Header: Title, status badge, priority, risk, SLA indicator
  ├── Details tab: All fields (editable if Draft or Pending, not closed)
  ├── Approvers tab: Approver list with statuses
  ├── Comments tab: Rich text comment thread
  └── Activity Stream tab: Immutable audit log
        │
        Actions available (while not closed):
        ├── [Edit Details] → inline edit → [Action: Save] → activity logged
        ├── [Add/Remove Approver] → activity logged
        ├── [Change Required/Optional] on approver
        ├── [Cancel Change Request] → confirmation → status = Cancelled
        └── [Post Comment] (see Section 8)
```

---

## 7. Approver Flows

### 7.1 Review & Approve (Non-linear or Linear — own turn)
```
[Approver receives email / in-app notification]
        │
        ▼
[Change Request Detail Page]
  - Review all details, attachments, existing comments
        │
        ├── [Action: Approve]
        │     → Confirmation toast
        │     → [System: Decision logged; activity stream updated]
        │     → [System: Notify requester + other approvers]
        │     → If all required approvers approved → CR status = "Approved"
        │
        └── [Action: Reject]
              → [Modal: Rejection Reason] (required text input)
              → [Action: Confirm Rejection]
              → [System: Decision logged; activity stream updated]
              → [System: Notify requester + other approvers]
              → Closure check:
                  ├── Single required approver → CR status = "Rejected"
                  └── Multiple approvers:
                        └── All required approvers rejected → CR = "Rejected"
                        └── Mix of decisions → CR stays "Pending Approval"
```

### 7.2 Change an Approval Decision
```
[Change Request Detail Page — Approvers tab]
  (CR must NOT be in closed state)
        │
        ├── [Action: Change to Approved] (if currently rejected)
        │     → Confirmation modal → decision updated → activity logged
        │
        └── [Action: Change to Rejected] (if currently approved)
              → [Modal: Rejection Reason] → updated → activity logged
              → Re-evaluate closure state
```

### 7.3 Linear Workflow — Approver Turn Sequence
```
CR submitted in Linear mode
        │
        ▼
[System: Notify Approver #1 only]
        │
        Approver #1 Approves
        │
        ▼
[System: Notify Approver #2]
        │
        Approver #2 Approves
        │
        ▼
        ... (continues down the list)
        │
        Final required approver approves
        │
        ▼
[System: CR status = "Approved"; notify requester]
```

---

## 8. Comment Flow (All Roles except Auditor)

```
[Change Request Detail Page → Comments Tab]
        │
        [Rich Text Editor]
          - Format text (bold, italic, lists, code)
          - Type @username → [Dropdown: matching users] → select → @mention inserted
          - Attach files (upload or drag-drop)
          - Insert hyperlinks
        │
        [Action: Post Comment]
        │
        ▼
[System: Comment saved; appended to activity stream]
[System: In-app + email notification sent to:]
  - Requester
  - All approvers on the CR
  - All @mentioned users
```

---

## 9. Auditor Flow

```
[Login → Change Requests List]
  - Read-only: view all CRs, filter, search
        │
        [Action: Click CR]
        │
        ▼
[Change Request Detail Page — read-only]
  - Can view: Details, Approvers, Comments, Activity Stream
  - Cannot: comment, approve, edit, cancel
        │
[Audit Trail section]
  - Global audit trail: all orgs actions
  - Filter by: user, action, date range, CR ID
```

---

## 10. Notification Flow

```
Triggering Event occurs (approve, reject, comment, mention, SLA breach, etc.)
        │
        ▼
[System: Determine recipients based on event type]
        │
        ├── In-App Notification
        │     → Bell icon badge incremented
        │     → Entry added to Notification Feed (with link to CR)
        │
        └── Email Notification
              → Email composed from template
              → Sent via configured SMTP
              → Includes: summary, CR link, action buttons (View CR)
```

### Notification Event → Recipient Matrix

| Event | Requester | Active Approvers | @Mentioned | Admin |
|---|---|---|---|---|
| CR Submitted | ✓ | ✓ (or next in Linear) | — | — |
| Approval Decision | ✓ | ✓ | — | — |
| Rejection Decision | ✓ | ✓ | — | — |
| Comment Posted | ✓ | ✓ | ✓ | — |
| @Mention | — | — | ✓ | — |
| Approver Added/Removed | ✓ | ✓ | — | — |
| SLA Approaching | ✓ | ✓ | — | Escalation contacts |
| SLA Breached | ✓ | ✓ | — | Escalation contacts |
| CR Approved | ✓ | ✓ | — | — |
| CR Rejected | ✓ | ✓ | — | — |
| CR Cancelled | ✓ | ✓ | — | — |

---

## 11. Global Audit Trail Flow (Admin / Auditor)

```
[Admin / Auditor → Audit Trail]
        │
        ▼
[Audit Trail Page]
  - Paginated, reverse-chronological list of all events
  - Each entry shows: timestamp, actor, action, target entity, before/after values
        │
        Filters:
        ├── Date range picker
        ├── Actor (user search)
        ├── Action type (dropdown)
        └── Change Request ID (text input)
        │
        [Action: Export to CSV]
        │
        ▼
[System: CSV download with filtered results]
```
