# Audita — Design System & UI Reference

**Last Updated:** 2026-04-23
**Source:** `ui-designs/` folder (40 screens, light + dark variants)

---

## 1. Design Language

**Theme:** "The Sovereign Architect of Infrastructure" — professional, authoritative, command-center feel.

**Tone:** Corporate, precise, trust-inspiring. Dark navy used for authority; white/light-grey for clarity. Red for risk/urgency, amber for warnings, green for success.

---

## 2. Colour Palette

| Token | Value | Usage |
|---|---|---|
| `primary` | `#1D3A8A` (Navy Blue) | Primary buttons, sidebar active state, hero panel, brand accents |
| `primary-dark` | `#152D6E` | Button hover states |
| `surface` | `#FFFFFF` | Card/panel backgrounds (light mode) |
| `surface-dark` | `#111827` | Card/panel backgrounds (dark mode) |
| `background` | `#F3F4F6` | Page background (light mode) |
| `background-dark` | `#0F172A` | Page background (dark mode) |
| `text-primary` | `#111827` | Body text (light) |
| `text-muted` | `#6B7280` | Labels, secondary text |
| `danger` | `#EF4444` | Critical risk, rejection, destructive actions |
| `warning` | `#F59E0B` | SLA warnings, medium risk |
| `success` | `#10B981` | Approved status, success toasts |
| `info` | `#3B82F6` | Informational badges |
| `border` | `#E5E7EB` | Card and input borders (light) |

---

## 3. Typography

| Role | Font | Size | Weight |
|---|---|---|---|
| App logo / Brand | Inter or similar sans-serif | — | Bold |
| Page heading (H1) | Inter | 2rem / 32px | Bold (700) |
| Section heading (H2) | Inter | 1.5rem / 24px | SemiBold (600) |
| Card title | Inter | 1rem / 16px | SemiBold (600) |
| Body / Label | Inter | 0.875rem / 14px | Regular (400) |
| Caption / Muted | Inter | 0.75rem / 12px | Regular (400) |
| Monospace (IDs, hashes) | JetBrains Mono or system mono | 0.8rem | Regular |

---

## 4. Layout & Navigation

### Authenticated Layout (Admin / Requester / Approver / Auditor)

```
┌──────────────────────────────────────────────────────────────────┐
│ HEADER: [Audita logo + role label] [Search] [Bell] [?] [Avatar]  │
├─────────────────┬────────────────────────────────────────────────┤
│ SIDEBAR (fixed) │ MAIN CONTENT AREA                              │
│  Dashboard      │  Breadcrumbs                                   │
│  Change Requests│  Page title + subtitle                         │
│  Users          │  Action buttons (top-right)                    │
│  Groups         │  Filters / Table / Cards                       │
│  Audit Trail    │                                                │
│  Settings       │                                                │
│                 │                                                │
│  [+ New Change] │                                                │
└─────────────────┴────────────────────────────────────────────────┘
```

- Sidebar is **fixed-width, left-aligned** (~220px).
- Active nav item: left-border accent + background highlight.
- **"+ New Change"** CTA button is pinned at the bottom of the sidebar.
- Header: search (centre), notifications bell (badge count), help icon, user avatar + name + role label (top-right).

### Super Admin Layout

Similar sidebar structure with items: Command Center, Tenant Management, Change Risk, Audit Logs, Security Policy, Infrastructure.

### Auth Layout

Split-panel: left = navy blue hero panel (logo, tagline, marketing copy), right = white form panel.

---

## 5. Screen Inventory

| Screen | File | Notes |
|---|---|---|
| Sign In | `audita_sign_in/` | Split panel: navy hero + form; Google/Microsoft SSO buttons |
| Sign In (dark) | `audita_sign_in_dark/` | Dark variant |
| Forgot Password | `audita_forgot_password/` | Email input, back to login link |
| Complete Setup | `audita_complete_your_setup/` | Invite acceptance: full name + password creation |
| Dashboard (Admin) | `audita_dashboard_1/`, `audita_dashboard_2/` | KPI cards, pending approvals list, activity feed, category donut chart |
| Dashboard (dark) | `audita_dashboard_dark/` | Dark variant |
| Platform Dashboard | `audita_platform_dashboard/` | Super Admin: tenant count, global users, system health, SSO status, domain controls |
| Platform Dashboard (dark) | `audita_platform_dashboard_dark/` | |
| Change Requests List | `audita_change_requests/` | Table: ID, Title, Category, Status badge, Priority badge, Risk bar, Scheduled date, Requester avatar |
| Change Requests (dark) | `audita_change_requests_dark/` | |
| CR Detail (tab: Details) | `audita_cr_detail_1/` | Header: status badge, title, description, Approve/Reject buttons; tabs: Details, Approvers, Comments, Activity Stream |
| CR Detail (tab: Approvers) | `audita_cr_detail_2/` | Approver list with status indicators |
| CR Detail (dark) | `audita_cr_detail_dark/` | |
| Create Change Request | `audita_create_change_request/` | Multi-section form: Foundational Details, Impact Analysis, Technical Documentation (file upload) |
| Create CR (dark) | `audita_create_change_request_dark/` | |
| Audit Trail | `audita_audit_trail/` | Filterable table: Timestamp, Actor, Action Type badge, Entity Type, Entity ID; Export CSV |
| Audit Trail (dark) | `audita_audit_trail_dark/` | |
| User Management | `audita_user_management/` | Stats cards (total, pending, security health), role/status filters, user table with actions |
| User Management (dark) | `audita_user_management_dark/` | |
| Group Management | `audita_group_management/` | Group list, member management |
| Group Management (dark) | `audita_group_management_dark/` | |
| Roles & Permissions | `audita_roles_permissions/` | Role list (left panel), permission toggles per module (right panel) |
| Roles & Permissions (dark) | `audita_roles_permissions_dark/` | |
| Organization Settings | `audita_organization_settings/` | Tabs: General, Workflow, Custom Fields, Branding; approval type toggle; custom field list |
| Organization Settings (dark) | `audita_organization_settings_dark/` | |
| SLA Policies | `audita_sla_policies/` | Policy table, escalation logic panel, policy execution timeline |
| SLA Policies (dark) | `audita_sla_policies_dark/` | |
| SLA Breach Detail | `audita_sla_breach_detail/` | Detail view for a breached SLA |
| SLA Breach Detail (dark) | `audita_sla_breach_detail_dark/` | |
| Tenant Management | `audita_tenant_management/` | Super Admin: org list with status, last audit |
| Tenant Management (dark) | `audita_tenant_management_dark/` | |
| Provision New Org | `audita_provision_new_org/` | Super Admin: form to create new org |
| Provision New Org (dark) | `audita_provision_new_org_dark/` | |
| Clarity / Onboarding | `audita_clarity_dark/`, `audita_clarity_system_light_mode/` | Setup checklist for first-time Admin |
| UI Component Library | `audita_ui_component_library/`, `audita_ui_component_library_dark/` | Reference component library |

---

## 6. Core Components

### Status Badges (Change Requests)

| Status | Color |
|---|---|
| Draft | Grey / neutral |
| Pending Approval | Amber / yellow |
| Scheduled / In Progress | Blue |
| Approved | Green |
| Rejected | Red |
| Cancelled | Grey (muted) |

### Priority Badges

| Priority | Color |
|---|---|
| Low | Grey |
| Medium / Moderate | Blue |
| High / Substantial | Orange |
| Critical / Extreme | Red |

### Risk Profile Bar

Visual progress bar from green (low) → red (critical/extreme).

### Common UI Elements

- **Cards:** White background, subtle shadow, rounded-lg corners, `border border-border`.
- **Buttons (primary):** Navy blue background, white text, rounded-md. Hover: darker navy.
- **Buttons (secondary/ghost):** White background, navy border, navy text.
- **Buttons (danger):** Red background or red border.
- **Inputs:** Light grey background (`#F9FAFB`), 1px border, rounded-md, focus ring navy.
- **Tables:** Clean, no heavy grid lines. Row hover: subtle grey bg. Avatar + initials for users.
- **Modals:** Centered, overlay backdrop, close icon top-right, action buttons bottom-right.
- **Toast notifications:** Bottom-right or top-right; success (green), error (red), info (blue).
- **Tabs:** Underline style, active tab has bottom border + bold text.
- **Pagination:** Numbered + prev/next arrows; active page is filled navy circle.

---

## 7. Dark Mode

All screens have dark variants. Dark mode toggled via Tailwind's `dark:` class strategy. Toggle should be accessible from the user menu/header.

| Light | Dark |
|---|---|
| Page bg: `#F3F4F6` | `#0F172A` |
| Card bg: `#FFFFFF` | `#1E293B` |
| Sidebar bg: `#FFFFFF` | `#0F172A` |
| Text: `#111827` | `#F9FAFB` |

---

## 8. Responsive Strategy

- Primary target: **desktop web** (1280px+). Designs are optimised for this.
- Responsive web only (no native mobile apps in v1).
- Mobile-friendly at 768px+ via Tailwind responsive breakpoints.
- Sidebar collapses to icon-only or hamburger menu on smaller viewports.

---

## 9. Accessibility Target

WCAG 2.1 AA. Key requirements:
- Sufficient colour contrast ratios (4.5:1 for body text).
- Keyboard navigability for all interactive elements.
- ARIA labels on icon-only buttons.
- Focus indicators visible.
- Error messages linked to form fields via `aria-describedby`.
