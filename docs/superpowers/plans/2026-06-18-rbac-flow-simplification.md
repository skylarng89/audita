# RBAC & Flow Simplification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace role-based authorization with permission-based RBAC, simplify to 3 system roles (Admin, Requester, Auditor), introduce watchers, replace deployment approvers with a single assignee, make all approvers required, enforce read-only on completion, add shareable links, file logging, and update sample data.

**Architecture:** The existing permission infrastructure (permissions table, role_permissions join, JWT claims, UserPrincipal authorities) is activated by switching all 88 `@PreAuthorize` annotations from `hasRole()` to `hasAuthority()` via a custom `@authz.hasPermission()` SpEL bean that handles the SUPER_ADMIN wildcard. New watcher entities and a deployment assignee model replace the old approver-only model. Schema migrations are modified in-place since the DB is dropped.

**Tech Stack:** Java 25, Spring Boot 4.1.0, Hibernate 7.4.1, PostgreSQL 18.3, Flyway, Nuxt 4.4.8, Vue 3.5, Pinia 3.0.4, Tailwind CSS 4.3.0

**Spec:** `docs/superpowers/specs/2026-06-18-rbac-flow-simplification-design.md`

---

## File Structure

### Backend — Files Created

| File | Responsibility |
|---|---|
| `api/.../security/AuthorizationLogic.java` | SpEL bean (`@authz`) for permission checks with SUPER_ADMIN wildcard |
| `infrastructure/.../persistence/entity/CrWatcherEntity.java` | CR watcher entity |
| `infrastructure/.../persistence/entity/RequestUatWatcherEntity.java` | UAT watcher entity |
| `infrastructure/.../persistence/repository/CrWatcherRepository.java` | CR watcher repository |
| `infrastructure/.../persistence/repository/RequestUatWatcherRepository.java` | UAT watcher repository |
| `api/.../dto/request/AddWatchersRequest.java` | DTO for adding watchers |
| `api/.../dto/request/AssignDeployerRequest.java` | DTO for deployment assignee |
| `api/.../dto/response/CrWatcherResponse.java` | Watcher response DTO |
| `api/.../dto/response/RequestUatWatcherResponse.java` | UAT watcher response DTO |
| `api/.../dto/response/PermissionCatalogueResponse.java` | Permission list for admin UI |

### Backend — Files Deleted

| File | Reason |
|---|---|
| `infrastructure/.../persistence/entity/RequestDeploymentApproverEntity.java` | Deployment approvers replaced by single assignee |
| `infrastructure/.../persistence/repository/RequestDeploymentApproverRepository.java` | Same |

### Backend — Files Modified

| File | Changes |
|---|---|
| `V1__create_tenant_schema.sql` | Permissions seed, roles seed, cr_approvers columns, cr_watchers table |
| `V3__requests_workflow_core.sql` | UAT approvers columns, deployment table, deployment approvers table removed, uat_watchers table |
| `infrastructure/.../security/RoleHierarchy.java` | Remove APPROVER case |
| `api/.../security/UserPrincipal.java` | Add `*` wildcard for SUPER_ADMIN |
| `api/.../config/SecurityConfig.java` | Register `AuthorizationLogic` bean |
| `infrastructure/.../persistence/entity/CrApproverEntity.java` | Remove `isRequired`, `isAdHoc` |
| `infrastructure/.../persistence/entity/RequestUatApproverEntity.java` | Remove `isRequired` |
| `infrastructure/.../persistence/entity/RequestDeploymentEntity.java` | Remove approvers, add assignee |
| `infrastructure/.../persistence/entity/ChangeRequestEntity.java` | Simplify `evaluateApprovalClosure` |
| All 16 controllers | Replace `hasRole`/`hasAnyRole` with `hasAuthority`/`@authz.hasPermission` |
| `infrastructure/.../service/ChangeRequestService.java` | Watcher methods, move operations, `assertNotCompleted`, visibility, permission checks, audit logging |
| `infrastructure/.../service/RequestUatService.java` | UAT watcher methods, move operations, `assertNotCompleted` |
| `infrastructure/.../service/RequestDeploymentService.java` | Remove approver methods, add assignee + completion methods |
| `infrastructure/.../service/RoleService.java` | Overlap validation, delete role, list permissions |
| `infrastructure/.../service/SampleDataService.java` | Update roles, approvers, add watchers + deployments |
| `api/.../dto/request/AddApproverRequest.java` | Remove `isRequired` |
| `api/.../dto/response/RequestDeploymentResponse.java` | Replace approvers with assignee |
| `api/.../dto/response/ChangeRequestResponse.java` | Add watchers field |
| `api/.../dto/response/RequestUatResponse.java` | Add watchers field |
| `api/.../controller/RoleController.java` | Add permissions + delete endpoints |
| `api/.../controller/RequestDeploymentController.java` | Replace approve/reject with assign/complete |
| `api/src/main/resources/logback-spring.xml` | Add FILE appender with rolling policy |
| `api/src/main/resources/application.yml` | Env-configurable log levels |
| `.env.example` | Add logging section |

### Frontend — Files Modified

| File | Changes |
|---|---|
| `types/index.ts` | Remove `Approver`, add watcher types, update deployment status/type |
| `stores/auth.ts` | Remove Approver, add permission-based getter |
| `composables/useChangeRequests.ts` | Watcher methods, deployment assignee/complete, remove isRequired methods |
| `pages/change-requests/[id].vue` | Remove toggle, add watcher UI, move buttons, copy URL, read-only banner |
| `components/cr/CrUatPanel.vue` | Remove toggle, add UAT watcher UI, move buttons |
| `components/cr/CrDeploymentPanel.vue` | Remove approvers, add assignee UI, complete button |
| `components/cr/CrCompletionStatusControl.vue` | Gate by completionStatus + canMarkComplete |
| `pages/admin/roles/index.vue` | Full CRUD with permission picker |
| `composables/useRoleGuard.ts` | Permission-based checks |
| `middleware/can-create-cr.ts` | Permission check instead of role |
| `middleware/auth.global.ts` | Preserve redirect query param |
| `pages/audit-trail/index.vue` | New action types |

---

## Task Dependency Order

```
Task 1 (schema) → Task 2 (entities) → Task 3 (repositories) →
Task 4 (security) → Task 5 (role service) → Task 6 (CR service) →
Task 7 (UAT service) → Task 8 (deployment service) →
Task 9 (CR controller) → Task 10 (UAT controller) →
Task 11 (deployment controller) → Task 12 (role controller) →
Task 13 (other controllers) → Task 14 (sample data) →
Task 15 (logging) → Task 16 (frontend types/store) →
Task 17 (frontend composables) → Task 18 (frontend CR detail) →
Task 19 (frontend UAT panel) → Task 20 (frontend deployment panel) →
Task 21 (frontend roles admin) → Task 22 (frontend misc) →
Task 23 (integration verification)
```

---

## Task 1: Schema Migrations

**Files:**
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V1__create_tenant_schema.sql`
- Modify: `audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql`

- [ ] **Step 1: Update V1 — permissions seed**

In `V1__create_tenant_schema.sql`, replace the permissions INSERT block (lines 297-316) with:

```sql
INSERT INTO permissions (code, label) VALUES
    ('cr.create',              'Create Change Requests'),
    ('cr.view',                'View Change Requests'),
    ('cr.view.all',            'View All Change Requests (global)'),
    ('cr.edit',                'Edit Change Requests'),
    ('cr.cancel',              'Cancel Change Requests'),
    ('cr.submit',              'Submit Change Requests for Approval'),
    ('cr.approve',             'Approve / Reject Change Requests'),
    ('cr.manage_participants', 'Add/Remove Approvers, Watchers, Assignees'),
    ('uat.signoff',            'UAT Sign-Off (requester + approver)'),
    ('deployment.execute',     'Mark Deployment Completed'),
    ('users.view',             'View Users'),
    ('users.manage',           'Invite, Edit and Deactivate Users'),
    ('roles.view',             'View Roles'),
    ('roles.manage',           'Create and Manage Custom Roles'),
    ('groups.view',            'View Groups'),
    ('groups.manage',          'Create and Manage Groups'),
    ('settings.view',          'View Organisation Settings'),
    ('settings.manage',        'Manage Organisation Settings'),
    ('sla.view',               'View SLA Policies'),
    ('sla.manage',             'Create and Manage SLA Policies'),
    ('audit.view',             'View Audit Trail'),
    ('audit.export',           'Export Audit Trail to CSV')
ON CONFLICT (code) DO NOTHING;
```

- [ ] **Step 2: Update V1 — roles seed (remove Approver, update permissions)**

Replace the roles INSERT block (lines 318-345) with:

```sql
INSERT INTO roles (id, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Admin',     'Full organisation management',              TRUE),
    ('00000000-0000-0000-0000-000000000002', 'Requester', 'Can create and manage change requests',     TRUE),
    ('00000000-0000-0000-0000-000000000004', 'Auditor',   'Read-only access across the organisation',  TRUE)
ON CONFLICT (id) DO NOTHING;

-- Admin: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Requester: create, view, edit, cancel, submit, approve, manage_participants, uat.signoff, deployment.execute, users.view, groups.view, settings.view, sla.view
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions
WHERE code IN ('cr.create', 'cr.view', 'cr.edit', 'cr.cancel', 'cr.submit', 'cr.approve',
               'cr.manage_participants', 'uat.signoff', 'deployment.execute',
               'users.view', 'groups.view', 'settings.view', 'sla.view')
ON CONFLICT DO NOTHING;

-- Auditor: view all, users.view, groups.view, roles.view, settings.view, sla.view, audit.view, audit.export
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permissions
WHERE code IN ('cr.view', 'cr.view.all', 'users.view', 'groups.view', 'roles.view',
               'settings.view', 'sla.view', 'audit.view', 'audit.export')
ON CONFLICT DO NOTHING;
```

- [ ] **Step 3: Update V1 — cr_approvers table (remove is_required, is_ad_hoc)**

Replace the `cr_approvers` CREATE TABLE block (lines 179-191) with:

```sql
CREATE TABLE IF NOT EXISTS cr_approvers (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID        NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID        NOT NULL REFERENCES users(id),
    position          INT         NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    rejection_reason  TEXT,
    decided_at        TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cr_approver UNIQUE (change_request_id, user_id),
    CONSTRAINT chk_approver_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);
```

- [ ] **Step 4: Add V1 — cr_watchers table**

Add after the `cr_approvers` table definition:

```sql
CREATE TABLE IF NOT EXISTS cr_watchers (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    change_request_id UUID         NOT NULL REFERENCES change_requests(id) ON DELETE CASCADE,
    user_id           UUID         NOT NULL REFERENCES users(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cr_watcher UNIQUE (change_request_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_cr_watchers_cr ON cr_watchers(change_request_id);
CREATE INDEX IF NOT EXISTS idx_cr_watchers_user ON cr_watchers(user_id);
```

- [ ] **Step 5: Update V3 — request_uat_approvers (remove is_required)**

In `V3__requests_workflow_core.sql`, replace the `request_uat_approvers` CREATE TABLE block (lines 132-143) with:

```sql
CREATE TABLE IF NOT EXISTS request_uat_approvers (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uat_id           UUID        NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
    user_id          UUID        NOT NULL REFERENCES users(id),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    position         INT         NOT NULL,
    decided_at       TIMESTAMPTZ,
    rejection_reason TEXT,
    CONSTRAINT uq_uat_approver        UNIQUE (uat_id, user_id),
    CONSTRAINT chk_uat_approver_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);
```

- [ ] **Step 6: Add V3 — request_uat_watchers table**

Add after the `request_uat_approvers` table:

```sql
CREATE TABLE IF NOT EXISTS request_uat_watchers (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    uat_id     UUID        NOT NULL REFERENCES request_uat(id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_uat_watcher UNIQUE (uat_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_uat_watchers_uat ON request_uat_watchers(uat_id);
CREATE INDEX IF NOT EXISTS idx_uat_watchers_user ON request_uat_watchers(user_id);
```

- [ ] **Step 7: Update V3 — request_deployments table (replace approvers with assignee)**

Replace the `request_deployments` CREATE TABLE block (lines 156-165) with:

```sql
CREATE TABLE IF NOT EXISTS request_deployments (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id   UUID        NOT NULL UNIQUE REFERENCES change_requests(id) ON DELETE CASCADE,
    uat_id       UUID        NOT NULL UNIQUE REFERENCES request_uat(id),
    assignee_id  UUID        REFERENCES users(id),
    status       VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_by   UUID        REFERENCES users(id),
    promoted_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_deployment_status CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_deployment_assignee ON request_deployments(assignee_id);
```

- [ ] **Step 8: Remove V3 — request_deployment_approvers table**

Delete the entire `request_deployment_approvers` CREATE TABLE block (lines 167-178). This table no longer exists.

- [ ] **Step 9: Commit**

```bash
git add audita-api/infrastructure/src/main/resources/db/migration/tenant/V1__create_tenant_schema.sql audita-api/infrastructure/src/main/resources/db/migration/tenant/V3__requests_workflow_core.sql
git commit -m "refactor: update schema migrations for RBAC permissions, watchers, deployment assignee"
```

---

## Task 2: Entity Changes

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/CrApproverEntity.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestUatApproverEntity.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestDeploymentEntity.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/ChangeRequestEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/CrWatcherEntity.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestUatWatcherEntity.java`
- Delete: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/RequestDeploymentApproverEntity.java`

- [ ] **Step 1: Modify CrApproverEntity — remove isRequired and isAdHoc**

Read the current `CrApproverEntity.java`. Remove the `isRequired` field and its getter/setter. Remove the `isAdHoc` field and its getter/setter. Update the constructor to remove these parameters. The entity now has: `id`, `changeRequest`, `user`, `position`, `status`, `rejectionReason`, `decidedAt`, `createdAt`.

- [ ] **Step 2: Modify RequestUatApproverEntity — remove isRequired**

Read the current `RequestUatApproverEntity.java`. Remove the `isRequired` field and its getter/setter. Update the constructor. The entity now has: `id`, `uat`, `user`, `status`, `position`, `decidedAt`, `rejectionReason`.

- [ ] **Step 3: Modify RequestDeploymentEntity — remove approvers, add assignee**

Read the current `RequestDeploymentEntity.java`. Remove the `@OneToMany` approvers collection and its getter/setter. Add:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")
private UserEntity assignee;

public UserEntity getAssignee() { return assignee; }
public void setAssignee(UserEntity assignee) { this.assignee = assignee; }
```

Ensure `completedAt` field exists (it should already). The status field changes from String values `PENDING_APPROVAL`/`APPROVED`/`REJECTED` to `PENDING`/`COMPLETED`/`CANCELLED` — but since it's a raw String column, no enum change is needed in the entity itself.

- [ ] **Step 4: Modify ChangeRequestEntity — simplify evaluateApprovalClosure**

Read `ChangeRequestEntity.java` and find the `evaluateApprovalClosure` method. Replace the logic that checks required vs optional with simplified logic where ALL approvers are required:

```java
public void evaluateApprovalClosure() {
    if (approvers == null || approvers.isEmpty()) {
        return;
    }
    boolean allApproved = true;
    for (CrApproverEntity approver : approvers) {
        if (approver.getStatus() == ApproverStatus.REJECTED) {
            this.approvalStatus = ChangeRequestStatus.REJECTED;
            return;
        }
        if (approver.getStatus() != ApproverStatus.APPROVED) {
            allApproved = false;
        }
    }
    if (allApproved) {
        this.approvalStatus = ChangeRequestStatus.APPROVED;
    }
}
```

- [ ] **Step 5: Create CrWatcherEntity**

```java
package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cr_watchers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"change_request_id", "user_id"}))
public class CrWatcherEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", nullable = false)
    private ChangeRequestEntity changeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public CrWatcherEntity() {}

    public CrWatcherEntity(ChangeRequestEntity changeRequest, UserEntity user) {
        this.changeRequest = changeRequest;
        this.user = user;
    }

    public UUID getId() { return id; }
    public ChangeRequestEntity getChangeRequest() { return changeRequest; }
    public UserEntity getUser() { return user; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 6: Create RequestUatWatcherEntity**

```java
package io.audita.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "request_uat_watchers",
    uniqueConstraints = @UniqueConstraint(columnNames = {"uat_id", "user_id"}))
public class RequestUatWatcherEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uat_id", nullable = false)
    private RequestUatEntity uat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public RequestUatWatcherEntity() {}

    public RequestUatWatcherEntity(RequestUatEntity uat, UserEntity user) {
        this.uat = uat;
        this.user = user;
    }

    public UUID getId() { return id; }
    public RequestUatEntity getUat() { return uat; }
    public UserEntity getUser() { return user; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
```

- [ ] **Step 7: Delete RequestDeploymentApproverEntity**

Delete the file `RequestDeploymentApproverEntity.java` entirely.

- [ ] **Step 8: Verify build compiles**

Run: `cd audita-api && ./gradlew compileJava -x test --console=plain 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL (may have errors in services/controllers that reference removed fields — those will be fixed in later tasks. If build fails due to references to `isRequired` or `isAdHoc` or `RequestDeploymentApproverEntity`, proceed to the next tasks which fix those references.)

- [ ] **Step 9: Commit**

```bash
git add -A audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/entity/
git commit -m "refactor: entity changes — remove isRequired/isAdHoc, add watcher entities, deployment assignee"
```

---

## Task 3: Repository Changes

**Files:**
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/CrWatcherRepository.java`
- Create: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/RequestUatWatcherRepository.java`
- Delete: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/RequestDeploymentApproverRepository.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/CrApproverRepository.java` (if it has `deleteByIsSampleTrue` — no change needed since column removal is schema-level)

- [ ] **Step 1: Create CrWatcherRepository**

```java
package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.CrWatcherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CrWatcherRepository extends JpaRepository<CrWatcherEntity, UUID> {

    List<CrWatcherEntity> findByChangeRequestId(UUID changeRequestId);

    @Query("SELECT w FROM CrWatcherEntity w WHERE w.changeRequest.id = :crId AND w.user.id = :userId")
    Optional<CrWatcherEntity> findByCrIdAndUserId(UUID crId, UUID userId);

    void deleteByChangeRequestId(UUID changeRequestId);

    boolean existsByChangeRequestIdAndUserId(UUID changeRequestId, UUID userId);

    void deleteByIsSampleTrue();

    long countByIsSampleTrue();
}
```

Note: `isSample` field needs to be added to `CrWatcherEntity` if sample data uses it. Add `@Column(name = "is_sample") private boolean sample;` with getter/setter to `CrWatcherEntity`, and add `is_sample BOOLEAN NOT NULL DEFAULT FALSE` to the `cr_watchers` table in V1 migration. Do the same for `RequestUatWatcherEntity` and `request_uat_watchers` table.

- [ ] **Step 2: Create RequestUatWatcherRepository**

```java
package io.audita.infrastructure.persistence.repository;

import io.audita.infrastructure.persistence.entity.RequestUatWatcherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestUatWatcherRepository extends JpaRepository<RequestUatWatcherEntity, UUID> {

    List<RequestUatWatcherEntity> findByUatId(UUID uatId);

    @Query("SELECT w FROM RequestUatWatcherEntity w WHERE w.uat.id = :uatId AND w.user.id = :userId")
    Optional<RequestUatWatcherEntity> findByUatIdAndUserId(UUID uatId, UUID userId);

    void deleteByUatId(UUID uatId);

    boolean existsByUatIdAndUserId(UUID uatId, UUID userId);

    void deleteByIsSampleTrue();

    long countByIsSampleTrue();
}
```

- [ ] **Step 3: Delete RequestDeploymentApproverRepository**

Delete the file `RequestDeploymentApproverRepository.java` entirely.

- [ ] **Step 4: Update V1/V3 migrations — add is_sample to watcher tables**

In V1, update the `cr_watchers` table to add `is_sample BOOLEAN NOT NULL DEFAULT FALSE` column. In V3, update `request_uat_watchers` to add the same column.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: add watcher repositories, delete deployment approver repository"
```

---

## Task 4: Security — AuthorizationLogic and UserPrincipal

**Files:**
- Create: `audita-api/api/src/main/java/io/audita/api/security/AuthorizationLogic.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/security/UserPrincipal.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/config/SecurityConfig.java`
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/security/RoleHierarchy.java`

- [ ] **Step 1: Create AuthorizationLogic bean**

```java
package io.audita.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component("authz")
public class AuthorizationLogic {

    private static final String WILDCARD = "*";

    public boolean hasPermission(Authentication auth, String permission) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (WILDCARD.equals(name) || name.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPermission(Authentication auth, String... permissions) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (WILDCARD.equals(name)) {
                return true;
            }
            for (String perm : permissions) {
                if (name.equals(perm)) {
                    return true;
                }
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Modify UserPrincipal — add wildcard for SUPER_ADMIN**

Read `UserPrincipal.java`. In the `ofSuperAdmin()` method, add `SimpleGrantedAuthority("*")` to the authorities list. This grants SUPER_ADMIN all permissions via the wildcard.

- [ ] **Step 3: Modify RoleHierarchy — remove APPROVER**

Read `RoleHierarchy.java`. In the `precedence()` method switch statement, remove the `case "APPROVER" -> 500;` line. Custom roles remain at `default -> 100`.

- [ ] **Step 4: Commit**

```bash
git add -A audita-api/api/src/main/java/io/audita/api/security/ audita-api/infrastructure/src/main/java/io/audita/infrastructure/security/
git commit -m "feat: add AuthorizationLogic bean for permission-based RBAC, SUPER_ADMIN wildcard"
```

---

## Task 5: RoleService — Custom Role CRUD and Validation

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RoleService.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/PermissionCatalogueResponse.java` (create)
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/persistence/repository/PermissionRepository.java`

- [ ] **Step 1: Add listAll to PermissionRepository**

Read `PermissionRepository.java`. Add:

```java
List<PermissionEntity> findAllByOrderByCodeAsc();
```

- [ ] **Step 2: Create PermissionCatalogueResponse DTO**

```java
package io.audita.api.dto.response;

import java.util.List;

public record PermissionCatalogueResponse(
    List<PermissionEntry> permissions
) {
    public record PermissionEntry(String code, String label) {}
}
```

- [ ] **Step 3: Update RoleService — denyPermissionOverlap**

Read `RoleService.java`. Update `denyPermissionOverlap()` to also reject when a custom role's permission set equals ALL available permissions:

```java
private void denyPermissionOverlap(Set<UUID> permissionIds) {
    // Reject if permission set equals ALL permissions (can't create another Admin)
    long totalPermissionCount = permissionRepository.count();
    if (permissionIds.size() == totalPermissionCount) {
        throw new DomainNotPermittedException("OVERLAPPING_PERMISSIONS",
            "A custom role cannot have all permissions. Use the Admin role instead.");
    }

    // Reject if another role has the exact same permission set
    List<RoleEntity> allRoles = roleRepository.findAll();
    Set<String> newPermissionCodes = permissionRepository.findAllById(permissionIds).stream()
        .map(PermissionEntity::getCode)
        .collect(Collectors.toSet());

    for (RoleEntity existing : allRoles) {
        Set<String> existingCodes = existing.getPermissions().stream()
            .map(PermissionEntity::getCode)
            .collect(Collectors.toSet());
        if (existingCodes.equals(newPermissionCodes)) {
            throw new DomainNotPermittedException("OVERLAPPING_PERMISSIONS",
                "A role with this exact set of permissions already exists: " + existing.getName());
        }
    }
}
```

- [ ] **Step 4: Add deleteRole to RoleService**

```java
@Transactional
public void deleteRole(UUID roleId) {
    RoleEntity role = roleRepository.findById(roleId)
        .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));

    if (role.isSystem()) {
        throw new DomainNotPermittedException("SYSTEM_ROLE_IMMUTABLE",
            "System roles cannot be deleted");
    }

    long assignedUserCount = userRepository.countByRoleId(roleId);
    if (assignedUserCount > 0) {
        throw new DomainConflictException("ROLE_IN_USE",
            "Cannot delete role with " + assignedUserCount + " assigned users. Reassign users first.");
    }

    roleRepository.delete(role);
    log.info("Custom role deleted: {} ({})", role.getName(), role.getId());
}
```

Note: `countByRoleId` needs to be added to `UserRepository` — check if it exists, and if not, add `long countByRoleId(UUID roleId);` or use `user_roles` join table count.

- [ ] **Step 5: Add listPermissions to RoleService**

```java
@Transactional(readOnly = true)
public List<PermissionEntity> listAllPermissions() {
    return permissionRepository.findAllByOrderByCodeAsc();
}
```

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: RoleService — custom role CRUD, overlap validation, permission catalogue"
```

---

## Task 6: ChangeRequestService — Watchers, Participants, Read-Only, Visibility

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/ChangeRequestService.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/request/AddWatchersRequest.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/response/CrWatcherResponse.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/request/AddApproverRequest.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/ChangeRequestResponse.java`

This is the largest task. It should be broken into sub-steps.

- [ ] **Step 1: Create AddWatchersRequest DTO**

```java
package io.audita.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record AddWatchersRequest(
    @NotEmpty List<UUID> userIds
) {}
```

- [ ] **Step 2: Create CrWatcherResponse DTO**

```java
package io.audita.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CrWatcherResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String userFullName,
    OffsetDateTime createdAt
) {}
```

- [ ] **Step 3: Modify AddApproverRequest — remove isRequired**

Read `AddApproverRequest.java`. Remove the `isRequired` field. The record should only have `userId` (and `groupId` if it exists for group approvers).

- [ ] **Step 4: Modify ChangeRequestResponse — add watchers field**

Read `ChangeRequestResponse.java`. Add `List<CrWatcherResponse> watchers` to the response record.

- [ ] **Step 5: Add assertNotCompleted to ChangeRequestService**

Read `ChangeRequestService.java`. Add this guard method near `assertCanMutate`:

```java
private void assertNotCompleted(ChangeRequestEntity changeRequest) {
    if (changeRequest.getCompletionStatus() == CompletionStatus.COMPLETED) {
        throw new DomainConflictException("REQUEST_COMPLETED",
            "This request is completed and read-only.");
    }
}
```

Import `DomainConflictException` from the domain exception package (check if it exists; if not, use `InvalidStateTransitionException`).

- [ ] **Step 6: Update assertCanMutate — replace role checks with permission checks**

Replace `ELEVATED_ROLES` usage in `assertCanMutate` with a permission-based check. The caller passes the actor's permissions (from JWT) instead of role name. Add a new overload:

```java
private void assertCanMutate(ChangeRequestEntity changeRequest, UUID actorUserId, Set<String> actorPermissions) {
    if (actorPermissions != null && actorPermissions.contains("cr.view.all")) {
        return; // Admin-level access
    }
    UserEntity createdBy = changeRequest.getCreatedBy();
    if (createdBy == null || !createdBy.getId().equals(actorUserId)) {
        throw new DomainNotPermittedException("FORBIDDEN",
            "You are not allowed to modify this change request.");
    }
}
```

Update all call sites of `assertCanMutate` to pass the actor's permissions. The permissions can be obtained from `SecurityContextHolder.getContext().getAuthentication().getAuthorities()` — or better, pass them from the controller via the `CurrentUser` abstraction. Check how `actorRole` is currently obtained and replace with `actorPermissions`.

- [ ] **Step 7: Update hasGlobalViewAccess — replace role checks with permission checks**

```java
private boolean hasGlobalViewAccess(Set<String> viewerPermissions) {
    return viewerPermissions != null && viewerPermissions.contains("cr.view.all");
}
```

Update `getById()` and `list()` to accept permissions instead of role name.

- [ ] **Step 8: Update isViewerAllowed — add watchers and assignee**

```java
private boolean isViewerAllowed(ChangeRequestEntity cr, UUID viewerId) {
    if (cr.getApprovers().stream().anyMatch(a -> a.getUser().getId().equals(viewerId))) {
        return true;
    }
    if (crWatcherRepository.existsByChangeRequestIdAndUserId(cr.getId(), viewerId)) {
        return true;
    }
    if (cr.getDeployment() != null && cr.getDeployment().getAssignee() != null
        && cr.getDeployment().getAssignee().getId().equals(viewerId)) {
        return true;
    }
    return false;
}
```

Note: Check if `ChangeRequestEntity` has a `deployment` relationship. If not, query `RequestDeploymentRepository.findByRequestId()`.

- [ ] **Step 9: Add watcher methods to ChangeRequestService**

Inject `CrWatcherRepository` into `ChangeRequestService`. Add:

```java
public List<CrWatcherEntity> listWatchers(UUID crId) { ... }
public List<CrWatcherEntity> addWatchers(UUID crId, List<UUID> userIds, UUID actorUserId, Set<String> actorPermissions) { ... }
public void removeWatcher(UUID crId, UUID userId, UUID actorUserId, Set<String> actorPermissions) { ... }
public void moveWatcherToApprover(UUID crId, UUID userId, UUID actorUserId, Set<String> actorPermissions) { ... }
public void moveApproverToWatcher(UUID crId, UUID approverId, UUID actorUserId, Set<String> actorPermissions) { ... }
```

Each method must:
1. Load the CR
2. `assertCanMutate(cr, actorUserId, actorPermissions)`
3. `assertNotCompleted(cr)`
4. Perform the operation
5. Write audit log entry
6. Send notifications if applicable

For `addWatchers`: check mutual exclusivity with `cr_approvers`, check user is not Auditor, create watcher entities.

For `moveWatcherToApprover`: remove from `cr_watchers`, create `CrApproverEntity` with `position = last + 1`, `status = PENDING`.

For `moveApproverToWatcher`: only allow if approver `status == PENDING` (can't convert someone who already voted).

- [ ] **Step 10: Update addApprover — remove isRequired, add watcher exclusivity check**

Update `addApprover()` to:
1. Remove the `isRequired` parameter (always required now)
2. Check mutual exclusivity: if user is already a watcher on this CR, throw `CONFLICT`
3. Keep the existing Auditor check

- [ ] **Step 11: Tighten core-field edit to DRAFT only**

In `update()`, change the status check from `status.isEditable()` (DRAFT or PENDING_APPROVAL) to `status == ChangeRequestStatus.DRAFT`. Add `assertNotCompleted(cr)` at the top.

- [ ] **Step 12: Add assertNotCompleted to all mutation methods**

Add `assertNotCompleted(cr)` at the top of: `submit`, `cancel`, `completeRequest`, `setWorkflowMode`, `addApprover`, `addApproverGroup`, `removeApprover`, `reorderApprovers`, `uploadAttachment`, `upsertCustomFields`, and all new watcher/participant methods.

- [ ] **Step 13: Add audit logging for all new actions**

Each new method (addWatcher, removeWatcher, moveWatcherToApprover, moveApproverToWatcher) must call `auditLogService.log()` with the appropriate action type, entity type, entity ID, actor ID, actor email, payload, and IP address per `patterns.md` section 5.1.

- [ ] **Step 14: Add debug logging**

Add `log.debug(...)` / `log.info(...)` calls at service entry points, permission denials (WARN), and state transitions (INFO) per the spec section 13.6.

- [ ] **Step 15: Commit**

```bash
git add -A
git commit -m "feat: ChangeRequestService — watchers, participant management, read-only, permission-based visibility"
```

---

## Task 7: RequestUatService — UAT Watchers and Move Operations

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestUatService.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestUatResponse.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/request/CreateRequestUatRequest.java` (if it has isRequired)
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestUatApproverResponse.java` (remove isRequired)

- [ ] **Step 1: Inject RequestUatWatcherRepository**

Add `RequestUatWatcherRepository` and `CrWatcherRepository` to `RequestUatService` constructor.

- [ ] **Step 2: Add assertNotCompleted**

Add the same `assertNotCompleted(ChangeRequestEntity)` guard, loading the parent CR from the UAT entity.

- [ ] **Step 3: Add UAT watcher methods**

```java
public List<RequestUatWatcherEntity> listUatWatchers(UUID requestId) { ... }
public List<RequestUatWatcherEntity> addUatWatchers(UUID requestId, List<UUID> userIds, UUID actorUserId, Set<String> actorPermissions) { ... }
public void removeUatWatcher(UUID requestId, UUID userId, UUID actorUserId, Set<String> actorPermissions) { ... }
public void moveUatWatcherToApprover(UUID requestId, UUID userId, UUID actorUserId, Set<String> actorPermissions) { ... }
public void moveUatApproverToWatcher(UUID requestId, UUID approverId, UUID actorUserId, Set<String> actorPermissions) { ... }
```

Same pattern as CR watchers: mutual exclusivity with UAT approvers, Auditor check, audit logging, notifications.

- [ ] **Step 4: Update addApprover — remove isRequired**

Remove the `isRequired` parameter. Add watcher exclusivity check.

- [ ] **Step 5: Update RequestUatResponse — add watchers field**

Add `List<RequestUatWatcherResponse> watchers` to `RequestUatResponse`.

- [ ] **Step 6: Create RequestUatWatcherResponse DTO**

```java
package io.audita.api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RequestUatWatcherResponse(
    UUID id,
    UUID userId,
    String userEmail,
    String userFullName,
    OffsetDateTime createdAt
) {}
```

- [ ] **Step 7: Remove isRequired from RequestUatApproverResponse**

Read `RequestUatApproverResponse.java` and remove the `isRequired` field.

- [ ] **Step 8: Add audit logging and debug logging**

Same pattern as Task 6 — audit log each new action, add debug/info logging.

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "feat: RequestUatService — UAT watchers, move operations, remove isRequired"
```

---

## Task 8: RequestDeploymentService — Single Assignee Model

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/RequestDeploymentService.java`
- Modify: `audita-api/api/src/main/java/io/audita/api/dto/response/RequestDeploymentResponse.java`
- Create: `audita-api/api/src/main/java/io/audita/api/dto/request/AssignDeployerRequest.java`

- [ ] **Step 1: Create AssignDeployerRequest DTO**

```java
package io.audita.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignDeployerRequest(
    @NotNull UUID userId
) {}
```

- [ ] **Step 2: Update RequestDeploymentResponse — replace approvers with assignee**

```java
public record RequestDeploymentResponse(
    UUID id,
    UUID requestId,
    UUID uatId,
    String status,
    UserSummary assignee,
    String createdByFullName,
    OffsetDateTime promotedAt,
    OffsetDateTime completedAt
) {}
```

Where `UserSummary` is a small record: `record UserSummary(UUID id, String email, String fullName) {}`. Check if one already exists in the codebase and reuse it.

- [ ] **Step 3: Update RequestDeploymentService — remove approver methods**

Delete: `addApprover`, `approveDeployment`, `rejectDeployment`, `loadApprover`, and all methods referencing `RequestDeploymentApproverEntity` or `RequestDeploymentApproverRepository`. Remove the repository injection.

- [ ] **Step 4: Update createFromPromotion**

Modify `createFromPromotion()` to:
1. Create deployment with `assignee_id = NULL`, `status = "PENDING"`
2. No longer merge CR/UAT approvers into deployment approvers
3. Audit log the promotion

- [ ] **Step 5: Add assignDeployer method**

```java
@Transactional
public RequestDeploymentEntity assignDeployer(UUID requestId, UUID deployerUserId,
        UUID actorUserId, Set<String> actorPermissions) {
    RequestDeploymentEntity deployment = loadDeploymentByRequestId(requestId);
    assertNotCompleted(deployment);
    assertCanManageParticipants(deployment, actorUserId, actorPermissions);

    UserEntity deployer = userRepository.findById(deployerUserId)
        .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));

    if (isAuditor(deployer)) {
        throw new DomainNotPermittedException("FORBIDDEN",
            "Auditors cannot be assigned as deployers.");
    }

    UUID previousAssigneeId = deployment.getAssignee() != null
        ? deployment.getAssignee().getId() : null;
    deployment.setAssignee(deployer);

    auditLogService.log(
        previousAssigneeId == null ? "DEPLOYMENT_ASSIGNEE_SET" : "DEPLOYMENT_ASSIGNEE_CHANGED",
        "request_deployment", deployment.getId(), actorUserId,
        resolveActorEmail(actorUserId),
        Map.of("assigneeId", deployerUserId, "assigneeEmail", deployer.getEmail(),
               "previousAssigneeId", previousAssigneeId == null ? "null" : previousAssigneeId),
        RequestContext.getCurrentIp());

    notificationService.createAndPush(deployerUserId, "DEPLOYMENT_ASSIGNED",
        "Deployment Assigned", "You have been assigned to handle a deployment.",
        "/change-requests/" + requestId);

    log.info("Deployer assigned to deployment {} for request {}: {}",
        deployment.getId(), requestId, deployer.getEmail());
    return deployment;
}
```

- [ ] **Step 6: Add completeDeployment method**

```java
@Transactional
public RequestDeploymentEntity completeDeployment(UUID requestId, UUID actorUserId,
        Set<String> actorPermissions) {
    RequestDeploymentEntity deployment = loadDeploymentByRequestId(requestId);

    if (!"PENDING".equals(deployment.getStatus())) {
        throw new DomainConflictException("DEPLOYMENT_NOT_PENDING",
            "Deployment is not in PENDING status.");
    }
    if (deployment.getAssignee() == null) {
        throw new DomainConflictException("NO_ASSIGNEE",
            "Deployment has no assignee.");
    }

    boolean isAssignee = deployment.getAssignee().getId().equals(actorUserId);
    boolean isAdmin = actorPermissions != null && actorPermissions.contains("cr.view.all");
    if (!isAssignee && !isAdmin) {
        throw new DomainNotPermittedException("FORBIDDEN",
            "Only the assigned deployer can mark the deployment as completed.");
    }

    deployment.setStatus("COMPLETED");
    deployment.setCompletedAt(OffsetDateTime.now());

    auditLogService.log("DEPLOYMENT_COMPLETED", "request_deployment",
        deployment.getId(), actorUserId, resolveActorEmail(actorUserId),
        Map.of("assigneeId", deployment.getAssignee().getId(),
               "completedAt", deployment.getCompletedAt().toString()),
        RequestContext.getCurrentIp());

    // Notify requester
    ChangeRequestEntity cr = changeRequestRepository.findById(requestId)
        .orElseThrow(() -> new NotFoundException("CR_NOT_FOUND", "Change request not found"));
    if (cr.getCreatedBy() != null) {
        notificationService.createAndPush(cr.getCreatedBy().getId(),
            "DEPLOYMENT_COMPLETED", "Deployment Completed",
            "Deployment for '" + cr.getTitle() + "' is complete. Ready to close the request.",
            "/change-requests/" + requestId);
    }

    log.info("Deployment {} completed for request {} by {}",
        deployment.getId(), requestId, resolveActorEmail(actorUserId));
    return deployment;
}
```

- [ ] **Step 7: Update isDeploymentDone**

```java
@Transactional(readOnly = true)
public boolean isDeploymentDone(UUID requestId) {
    return requestDeploymentRepository.findByRequestId(requestId)
        .map(d -> "COMPLETED".equals(d.getStatus()))
        .orElse(false);
}
```

- [ ] **Step 8: Add assertNotCompleted and assertCanManageParticipants**

Add `assertNotCompleted` that checks the parent CR's `completionStatus`. Add `assertCanManageParticipants` that checks if actor is creator or admin.

- [ ] **Step 9: Remove deployment approver response mapping**

Remove any code that maps `RequestDeploymentApproverEntity` to response DTOs. The `getDeployment()` method should return the new `RequestDeploymentResponse` with `assignee` instead of `approvers`.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: RequestDeploymentService — single assignee model, complete deployment, remove approvers"
```

---

## Task 9: ChangeRequestController — Permission-Based Annotations and New Endpoints

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/ChangeRequestController.java`

- [ ] **Step 1: Replace all @PreAuthorize annotations**

Go through every `@PreAuthorize` in `ChangeRequestController.java` and replace:

| Current | New |
|---|---|
| `hasAnyRole('REQUESTER','ADMIN','SUPER_ADMIN')` | `@authz.hasPermission(authentication, 'cr.create')` (on create) |
| Same on submit | `@authz.hasPermission(authentication, 'cr.submit')` |
| Same on update/edit | `@authz.hasPermission(authentication, 'cr.edit')` |
| Same on cancel | `@authz.hasPermission(authentication, 'cr.cancel')` |
| Same on approver management | `@authz.hasPermission(authentication, 'cr.manage_participants')` |
| `isAuthenticated() and !hasRole('AUDITOR')` (approve) | `@authz.hasPermission(authentication, 'cr.approve')` |
| Same on reject | `@authz.hasPermission(authentication, 'cr.approve')` |
| `isAuthenticated()` (read endpoints) | `@authz.hasPermission(authentication, 'cr.view')` |
| `hasAnyRole('REQUESTER','APPROVER','AUDITOR','ADMIN','SUPER_ADMIN')` (search candidates) | `@authz.hasPermission(authentication, 'cr.view')` |

- [ ] **Step 2: Remove updateApproverRequirement endpoint**

Delete the `PATCH /{id}/approvers/{approverId}/requirement` endpoint method entirely.

- [ ] **Step 3: Add watcher endpoints**

```java
@GetMapping("/{id}/watchers")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
public ResponseEntity<List<CrWatcherResponse>> listWatchers(@PathVariable UUID id) { ... }

@PostMapping("/{id}/watchers")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<List<CrWatcherResponse>> addWatchers(@PathVariable UUID id,
        @Valid @RequestBody AddWatchersRequest request) { ... }

@DeleteMapping("/{id}/watchers/{userId}")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> removeWatcher(@PathVariable UUID id, @PathVariable UUID userId) { ... }

@PostMapping("/{id}/watchers/{userId}/promote")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> promoteWatcher(@PathVariable UUID id, @PathVariable UUID userId) { ... }

@PostMapping("/{id}/approvers/{approverId}/demote")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> demoteApprover(@PathVariable UUID id, @PathVariable UUID approverId) { ... }
```

Each endpoint extracts actor info from `CurrentUser` (or `SecurityContextHolder`) and delegates to the service.

- [ ] **Step 4: Update addApprover endpoint — remove isRequired from request**

The `addApprover` endpoint no longer accepts `isRequired` in the request body. Update the method signature.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: ChangeRequestController — permission-based auth, watcher endpoints, remove requirement toggle"
```

---

## Task 10: RequestUatController — Permission-Based Annotations and UAT Watcher Endpoints

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/RequestUatController.java`

- [ ] **Step 1: Replace all @PreAuthorize annotations**

Same pattern as Task 9. Replace `hasAnyRole('REQUESTER','ADMIN','SUPER_ADMIN')` with `@authz.hasPermission(authentication, ...)` based on the operation:
- Create/update UAT: `cr.edit`
- Add UAT approver: `cr.manage_participants`
- UAT sign-off: `uat.signoff`
- Promote to deployment: `cr.edit`
- Read endpoints: `cr.view`
- Approve/reject UAT: `uat.signoff`

- [ ] **Step 2: Remove updateApproverRequirement endpoint**

Delete the `PATCH /{id}/uat/approvers/{approverId}/requirement` endpoint.

- [ ] **Step 3: Add UAT watcher endpoints**

```java
@GetMapping("/{id}/uat/watchers")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.view')")
public ResponseEntity<List<RequestUatWatcherResponse>> listUatWatchers(@PathVariable UUID id) { ... }

@PostMapping("/{id}/uat/watchers")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<List<RequestUatWatcherResponse>> addUatWatchers(@PathVariable UUID id,
        @Valid @RequestBody AddWatchersRequest request) { ... }

@DeleteMapping("/{id}/uat/watchers/{userId}")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> removeUatWatcher(@PathVariable UUID id, @PathVariable UUID userId) { ... }

@PostMapping("/{id}/uat/watchers/{userId}/promote")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> promoteUatWatcher(@PathVariable UUID id, @PathVariable UUID userId) { ... }

@PostMapping("/{id}/uat/approvers/{approverId}/demote")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<Void> demoteUatApprover(@PathVariable UUID id, @PathVariable UUID approverId) { ... }
```

- [ ] **Step 4: Update addUatApprover — remove isRequired**

Update the endpoint to not accept `isRequired` in the request body.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: RequestUatController — permission-based auth, UAT watcher endpoints"
```

---

## Task 11: RequestDeploymentController — Assignee and Complete Endpoints

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/RequestDeploymentController.java`

- [ ] **Step 1: Replace @PreAuthorize annotations**

| Current | New |
|---|---|
| `isAuthenticated()` (read) | `@authz.hasPermission(authentication, 'cr.view')` |
| `isAuthenticated() and !hasRole('AUDITOR')` (approve/reject) | Remove these endpoints entirely |
| `isAuthenticated()` (comments) | `@authz.hasPermission(authentication, 'cr.view')` |

- [ ] **Step 2: Remove approve and reject endpoints**

Delete the `approveDeployment` and `rejectDeployment` endpoint methods entirely.

- [ ] **Step 3: Add assignee endpoint**

```java
@PatchMapping("/{id}/deployment/assignee")
@PreAuthorize("@authz.hasPermission(authentication, 'cr.manage_participants')")
public ResponseEntity<RequestDeploymentResponse> assignDeployer(
        @PathVariable UUID id,
        @Valid @RequestBody AssignDeployerRequest request) {
    // extract actorUserId and actorPermissions from SecurityContext
    return ResponseEntity.ok(deploymentService.assignDeployer(id, request.userId(), actorUserId, actorPermissions));
}
```

- [ ] **Step 4: Add complete endpoint**

```java
@PostMapping("/{id}/deployment/complete")
@PreAuthorize("@authz.hasPermission(authentication, 'deployment.execute')")
public ResponseEntity<RequestDeploymentResponse> completeDeployment(@PathVariable UUID id) {
    // extract actorUserId and actorPermissions from SecurityContext
    return ResponseEntity.ok(deploymentService.completeDeployment(id, actorUserId, actorPermissions));
}
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: RequestDeploymentController — assignee and complete endpoints, remove approve/reject"
```

---

## Task 12: RoleController — Permissions Catalogue and Delete Endpoint

**Files:**
- Modify: `audita-api/api/src/main/java/io/audita/api/controller/RoleController.java`

- [ ] **Step 1: Add permissions catalogue endpoint**

```java
@GetMapping("/permissions")
@PreAuthorize("@authz.hasPermission(authentication, 'roles.view')")
public ResponseEntity<PermissionCatalogueResponse> listPermissions() {
    List<PermissionEntity> permissions = roleService.listAllPermissions();
    List<PermissionCatalogueResponse.PermissionEntry> entries = permissions.stream()
        .map(p -> new PermissionCatalogueResponse.PermissionEntry(p.getCode(), p.getLabel()))
        .toList();
    return ResponseEntity.ok(new PermissionCatalogueResponse(entries));
}
```

- [ ] **Step 2: Add delete role endpoint**

```java
@DeleteMapping("/{id}")
@PreAuthorize("@authz.hasPermission(authentication, 'roles.manage')")
public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
    roleService.deleteRole(id);
    return ResponseEntity.noContent().build();
}
```

- [ ] **Step 3: Update existing @PreAuthorize annotations**

Replace `hasAnyRole('ADMIN','SUPER_ADMIN')` with `@authz.hasPermission(authentication, 'roles.view')` (on GET) and `@authz.hasPermission(authentication, 'roles.manage')` (on POST/PATCH).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: RoleController — permissions catalogue endpoint, delete role endpoint"
```

---

## Task 13: Remaining Controllers — Permission-Based Annotations

**Files:**
- Modify: All remaining controllers that have `@PreAuthorize` annotations

The remaining controllers and their annotation changes:

| Controller | Current | New |
|---|---|---|
| `AuthController` | (public endpoints, no change) | No change |
| `CommentController` | `isAuthenticated()` | `@authz.hasPermission(authentication, 'cr.view')` (but also check `assertNotCompleted` in service) |
| `GroupController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` (manage) | `@authz.hasPermission(authentication, 'groups.manage')` |
| `GroupController` | `isAuthenticated()` (read) | `@authz.hasPermission(authentication, 'groups.view')` |
| `UserController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` (manage) | `@authz.hasPermission(authentication, 'users.manage')` |
| `UserController` | `hasAnyRole('AUDITOR','ADMIN','SUPER_ADMIN')` (list) | `@authz.hasPermission(authentication, 'users.view')` |
| `UserController` | `isAuthenticated()` (search) | `@authz.hasPermission(authentication, 'users.view')` |
| `NotificationController` | `isAuthenticated()` | Keep as `isAuthenticated()` (all logged-in users need notifications) |
| `DashboardController` | `isAuthenticated()` | Keep as `isAuthenticated()` |
| `DepartmentAdminController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` | `@authz.hasPermission(authentication, 'groups.manage')` (departments are now groups) |
| `CustomFieldAdminController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` | `@authz.hasPermission(authentication, 'settings.manage')` |
| `TenantSettingsController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` | `@authz.hasPermission(authentication, 'settings.manage')` (manage), `@authz.hasPermission(authentication, 'settings.view')` (read) |
| `AuditTrailController` | `hasAnyRole('ADMIN','AUDITOR')` | `@authz.hasPermission(authentication, 'audit.view')` |
| `AuditTrailController` | (export) | `@authz.hasPermission(authentication, 'audit.export')` |
| `SampleDataController` | `hasAnyRole('ADMIN','SUPER_ADMIN')` | `@authz.hasPermission(authentication, 'settings.manage')` |
| `TenantController` | `hasRole('SUPER_ADMIN')` | Keep as `hasRole('SUPER_ADMIN')` (platform-level, not tenant) |
| `PlatformHealthController` | `hasRole('SUPER_ADMIN')` | Keep as `hasRole('SUPER_ADMIN')` |
| `SsoController` | (check current annotations) | `@authz.hasPermission(authentication, 'settings.manage')` for config, `isAuthenticated()` for callback |

- [ ] **Step 1: Update each controller**

Go through each controller listed above and replace the `@PreAuthorize` annotations per the table.

- [ ] **Step 2: Verify build compiles**

Run: `cd audita-api && ./gradlew compileJava -x test --console=plain 2>&1 | tail -30`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "refactor: replace all hasRole/hasAnyRole with permission-based @authz.hasPermission"
```

---

## Task 14: SampleDataService Updates

**Files:**
- Modify: `audita-api/infrastructure/src/main/java/io/audita/infrastructure/service/SampleDataService.java`

- [ ] **Step 1: Update createSampleUsers — remove Approver role**

Replace the role lookups and user assignments:
- Remove `roleRepository.findByName("Approver")` 
- Users previously assigned Approver (`david_kim`, `robert_johnson`, `priya_sharma`) get `Requester` instead
- 8 users: 1 Admin, 6 Requester, 1 Auditor

- [ ] **Step 2: Update addApprover method — remove isRequired parameter**

Change the `addApprover` helper method signature from:
`addApprover(ChangeRequestEntity cr, UserEntity user, boolean isRequired, int position, ApproverStatus status, OffsetDateTime decidedAt)`
to:
`addApprover(ChangeRequestEntity cr, UserEntity user, int position, ApproverStatus status, OffsetDateTime decidedAt)`

Update all call sites to remove the `boolean isRequired` argument. Update the `CrApproverEntity` constructor call to remove `isRequired` and `isAdHoc`.

- [ ] **Step 3: Add createSampleWatchers method**

Add watchers to several sample CRs — users who are not already approvers on those CRs. Example:

```java
private void createSampleWatchers(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
    addWatcher(crs.get("pg_upgrade"), users.get("lisa_patel"));
    addWatcher(crs.get("payment_v2"), users.get("alex_thompson"));
    addWatcher(crs.get("k8s_migration"), users.get("maria_garcia"));
    addWatcher(crs.get("spring_cve"), users.get("james_wilson"));
    addWatcher(crs.get("monitoring"), users.get("priya_sharma"));
}
```

Call it from `importSampleData()` after `createSampleApprovers()`.

- [ ] **Step 4: Add createSampleDeployments method**

For DELIVERY_PIPELINE CRs that are APPROVED, create deployment rows with assignees:

```java
private void createSampleDeployments(Map<String, ChangeRequestEntity> crs, Map<String, UserEntity> users, OffsetDateTime now) {
    // pg_upgrade: completed deployment
    createDeployment(crs.get("pg_upgrade"), users.get("david_kim"), "COMPLETED", now.minusDays(2), now.minusDays(1));
    // k8s_migration: pending deployment
    createDeployment(crs.get("k8s_migration"), users.get("david_kim"), "PENDING", now.minusDays(1), null);
}
```

Note: These CRs need `workflowMode = DELIVERY_PIPELINE` set. Update `createCR()` to accept workflow mode, or set it after creation.

- [ ] **Step 5: Update removeSampleData — clean up new tables**

Add cleanup for `crWatcherRepository.deleteByIsSampleTrue()` and `requestUatWatcherRepository.deleteByIsSampleTrue()` before deleting CRs. Remove any cleanup for deployment approvers (the repository is deleted).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: SampleDataService — updated roles, watchers, deployments for new schema"
```

---

## Task 15: Logging — File Appender and Env Configuration

**Files:**
- Modify: `audita-api/api/src/main/resources/logback-spring.xml`
- Modify: `audita-api/api/src/main/resources/application.yml`
- Modify: `.env.example`

- [ ] **Step 1: Update logback-spring.xml — add FILE appender**

Add a `FILE` appender inside the `<configuration>` root (active in all profiles):

```xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_FILE:-logs/audita-api.log}</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <fileNamePattern>logs/audita-api-%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>${LOG_FILE_MAX_SIZE:-50MB}</maxFileSize>
        <maxHistory>${LOG_FILE_MAX_HISTORY:-30}</maxHistory>
        <totalSizeCap>${LOG_FILE_TOTAL_SIZE_CAP:-1GB}</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{ISO8601} %-5level [%thread] [%X{tenant_id}|%X{user_id}|%X{request_id}] %logger{36} - %msg%n%rEx</pattern>
    </encoder>
</appender>
```

Add `<appender-ref ref="FILE" />` to both the root logger in the non-dev profile and the dev profile root logger, and to the `io.audita` logger in both profiles.

- [ ] **Step 2: Update application.yml — env-configurable log levels**

Replace the logging section:

```yaml
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    "[io.audita]": ${LOG_LEVEL_APP:DEBUG}
    "[org.flywaydb]": ${LOG_LEVEL_FLYWAY:INFO}
    "[org.hibernate.orm.deprecation]": ERROR
    "[org.hibernate.orm.connections.pooling]": WARN
    "[org.hibernate.orm.query]": ERROR
    "[io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics]": ERROR
  pattern:
    console: "%d{ISO8601} %-5level [%thread] %logger{36} - %msg%n"
  file:
    name: ${LOG_FILE:logs/audita-api.log}
  logback:
    rollingpolicy:
      max-file-size: ${LOG_FILE_MAX_SIZE:50MB}
      max-history: ${LOG_FILE_MAX_HISTORY:30}
      total-size-cap: ${LOG_FILE_TOTAL_SIZE_CAP:1GB}
```

- [ ] **Step 3: Update .env.example — add logging section**

Add after the application settings section:

```env
# Logging configuration
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_LEVEL_FLYWAY=INFO
LOG_FILE=logs/audita-api.log
LOG_FILE_MAX_SIZE=50MB
LOG_FILE_MAX_HISTORY=30
LOG_FILE_TOTAL_SIZE_CAP=1GB
```

- [ ] **Step 4: Create logs directory**

Create the `audita-api/logs/` directory and add a `.gitkeep` file so the directory exists in the repo. Add `logs/` to `.gitignore` (but keep `.gitkeep`).

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: file logging with daily rotation and env-configurable log levels"
```

---

## Task 16: Frontend — Types and Auth Store

**Files:**
- Modify: `audita-web/types/index.ts`
- Modify: `audita-web/stores/auth.ts`
- Modify: `audita-web/composables/useRoleGuard.ts`
- Modify: `audita-web/middleware/can-create-cr.ts`

- [ ] **Step 1: Update types/index.ts**

Remove `"Approver"` from `UserRole`:
```typescript
export type UserRole = "Admin" | "Requester" | "Auditor" | "SUPER_ADMIN";
```

Add watcher types:
```typescript
export interface CrWatcher {
  id: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  createdAt: string;
}

export interface UatWatcher {
  id: string;
  userId: string;
  userEmail: string;
  userFullName: string;
  createdAt: string;
}
```

Update `DeploymentStatus`:
```typescript
export type DeploymentStatus = "PENDING" | "COMPLETED" | "CANCELLED";
```

Add `assignee` to `Deployment` interface:
```typescript
export interface Deployment {
  id: string;
  requestId: string;
  uatId: string;
  status: DeploymentStatus;
  assignee: { id: string; email: string; fullName: string } | null;
  createdByFullName: string;
  promotedAt: string;
  completedAt: string | null;
}
```

Remove `isRequired` from `CrApprover` and `UatApprover` interfaces. Add `watchers` to `ChangeRequest` and `UatRequest` interfaces.

- [ ] **Step 2: Update stores/auth.ts**

Remove `Approver` references. Add permission-based checking:

```typescript
permissions: string[] | null;

getters: {
  // ... existing getters ...
  can: (s) => (permission: string) => s.permissions?.includes(permission) ?? false,
  hasPermission: (s) => (permission: string) => s.permissions?.includes(permission) ?? false,
}
```

Update `setAuth` to extract permissions from the auth response (JWT claims or response body). Check what `AuthResponse` contains and add `permissions: string[]` if not present.

- [ ] **Step 3: Update useRoleGuard.ts**

Add a permission-based guard:

```typescript
export function usePermissionGuard(requiredPermission: string) {
  const auth = useAuthStore();
  const isAllowed = computed(() => auth.hasPermission(requiredPermission));
  const redirect = "/dashboard";
  return { isAllowed, redirect };
}
```

- [ ] **Step 4: Update can-create-cr.ts middleware**

```typescript
export default defineNuxtRouteMiddleware(() => {
  const auth = useAuthStore();
  if (!auth.hasPermission("cr.create")) {
    return navigateTo("/change-requests");
  }
});
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: frontend types and auth store — permission-based, remove Approver, add watchers"
```

---

## Task 17: Frontend — Composables

**Files:**
- Modify: `audita-web/composables/useChangeRequests.ts`

- [ ] **Step 1: Add watcher methods**

```typescript
function listWatchers(id: string) {
  return api<CrWatcher[]>(`/api/v1/change-requests/${id}/watchers`);
}

function addWatchers(id: string, userIds: string[]) {
  return api<CrWatcher[]>(`/api/v1/change-requests/${id}/watchers`, {
    method: "POST",
    body: { userIds },
  });
}

function removeWatcher(id: string, userId: string) {
  return api<void>(`/api/v1/change-requests/${id}/watchers/${userId}`, {
    method: "DELETE",
  });
}

function promoteWatcher(id: string, userId: string) {
  return api<void>(`/api/v1/change-requests/${id}/watchers/${userId}/promote`, {
    method: "POST",
  });
}

function demoteApprover(id: string, approverId: string) {
  return api<void>(`/api/v1/change-requests/${id}/approvers/${approverId}/demote`, {
    method: "POST",
  });
}
```

- [ ] **Step 2: Add UAT watcher methods**

```typescript
function listUatWatchers(id: string) {
  return api<UatWatcher[]>(`/api/v1/change-requests/${id}/uat/watchers`);
}

function addUatWatchers(id: string, userIds: string[]) {
  return api<UatWatcher[]>(`/api/v1/change-requests/${id}/uat/watchers`, {
    method: "POST",
    body: { userIds },
  });
}

function removeUatWatcher(id: string, userId: string) {
  return api<void>(`/api/v1/change-requests/${id}/uat/watchers/${userId}`, {
    method: "DELETE",
  });
}

function promoteUatWatcher(id: string, userId: string) {
  return api<void>(`/api/v1/change-requests/${id}/uat/watchers/${userId}/promote`, {
    method: "POST",
  });
}

function demoteUatApprover(id: string, approverId: string) {
  return api<void>(`/api/v1/change-requests/${id}/uat/approvers/${approverId}/demote`, {
    method: "POST",
  });
}
```

- [ ] **Step 3: Add deployment assignee and complete methods**

```typescript
function assignDeployer(id: string, userId: string) {
  return api<Deployment>(`/api/v1/change-requests/${id}/deployment/assignee`, {
    method: "PATCH",
    body: { userId },
  });
}

function completeDeployment(id: string) {
  return api<Deployment>(`/api/v1/change-requests/${id}/deployment/complete`, {
    method: "POST",
  });
}
```

- [ ] **Step 4: Remove isRequired from addApprover and addUatApprover**

Update `addApprover` to not send `isRequired`. Update `addUatApprover` to not send `isRequired`.

- [ ] **Step 5: Remove updateApproverRequirement methods**

Delete `updateApproverRequirement` and `updateUatApproverRequirement` functions.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: useChangeRequests composable — watcher methods, deployment assignee/complete"
```

---

## Task 18: Frontend — Change Request Detail Page

**Files:**
- Modify: `audita-web/pages/change-requests/[id].vue`

This is the largest frontend task.

- [ ] **Step 1: Remove Required/Optional toggle**

Remove the toggle UI from the approvers list (around lines 663-681). Remove the "Always Optional" badge for Auditors. All approvers are required — no toggle needed.

- [ ] **Step 2: Add watcher management UI**

Add a new section below the approvers list for watchers. Include:
- "Add Watcher" button (gated by `canManageApprovers`)
- Search input + user selection (same pattern as approver add)
- Watcher list with name, email, "Remove" button, "Promote to Approver" button

- [ ] **Step 3: Add "Demote to Watcher" button on approver rows**

Add a button on each approver row (only for PENDING approvers) that calls `demoteApprover()`. Gated by `canManageApprovers`.

- [ ] **Step 4: Update canManageApprovers**

Change to check `completionStatus !== "COMPLETED"` instead of just status:
```typescript
const canManageApprovers = computed(() => {
  if (cr.value?.completionStatus === "COMPLETED") return false;
  if (auth.hasPermission("cr.manage_participants") && (isCreator.value || auth.isAdmin || auth.isSuperAdmin)) return true;
  return false;
});
```

- [ ] **Step 5: Add Copy URL button**

Add a "Copy URL" button near the request title/header area:

```vue
<button @click="copyUrl" class="btn-icon" title="Copy link to share">
  <svg><!-- copy icon --></svg>
</button>
```

```typescript
async function copyUrl() {
  const url = window.location.href;
  await navigator.clipboard.writeText(url);
  // Show "Copied!" tooltip for 2 seconds
  copied.value = true;
  setTimeout(() => { copied.value = false; }, 2000);
}
```

- [ ] **Step 6: Add read-only banner**

When `completionStatus === "COMPLETED"`, show a banner at the top of the detail page:

```vue
<div v-if="cr.completionStatus === 'COMPLETED'" class="rounded-lg bg-info-light border border-info/30 px-4 py-3 text-sm text-info mb-4">
  This request is completed and read-only.
</div>
```

- [ ] **Step 7: Lock core fields after submit**

Update `canEditCR` to only allow editing in DRAFT:
```typescript
const canEditCR = computed(() => {
  if (cr.value?.completionStatus === "COMPLETED") return false;
  if (cr.value?.status !== "DRAFT") return false;
  return isCreator.value || auth.isAdmin || auth.isSuperAdmin;
});
```

- [ ] **Step 8: Update canSeeApprovalActions and canCastVote**

Replace `auth.role === "Auditor"` checks with `!auth.hasPermission("cr.approve")`:
```typescript
const canSeeApprovalActions = computed(() => {
  if (cr.value?.status !== "PENDING_APPROVAL") return false;
  if (!auth.hasPermission("cr.approve")) return false;
  return auth.isAdmin || auth.isSuperAdmin || isApprover.value;
});
```

- [ ] **Step 9: Update candidate filtering**

Remove Auditor filtering from candidate search (the backend now handles it). Or keep the frontend filter as defense-in-depth — check `!auth.hasPermission("cr.approve")` for the current user instead of role check.

- [ ] **Step 10: Commit**

```bash
git add -A
git commit -m "feat: CR detail page — watchers, copy URL, read-only banner, permission-based gating"
```

---

## Task 19: Frontend — UAT Panel

**Files:**
- Modify: `audita-web/components/cr/CrUatPanel.vue`

- [ ] **Step 1: Remove Required/Optional toggle**

Remove the toggle UI from UAT approver rows (around lines 158-168).

- [ ] **Step 2: Add UAT watcher management UI**

Add a watcher section below UAT approvers with:
- "Add UAT Watcher" button (gated by `canManage`)
- Search + user selection
- Watcher list with Remove and "Promote to Approver" buttons

- [ ] **Step 3: Add "Demote to Watcher" on UAT approver rows**

Add button on each PENDING UAT approver row.

- [ ] **Step 4: Update canManage**

Replace `auth.role !== "Auditor"` with `auth.hasPermission("cr.manage_participants")`:
```typescript
const canManage = computed(() => {
  if (!uat.value || uat.value.readOnly) return false;
  if (!auth.hasPermission("cr.manage_participants")) return false;
  return auth.isSuperAdmin || auth.isAdmin || uat.value.createdBy === auth.userId;
});
```

- [ ] **Step 5: Update addUatApprover call**

Remove `isRequired: false` from the `addUatApprover` call.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat: CrUatPanel — UAT watchers, move operations, remove toggle"
```

---

## Task 20: Frontend — Deployment Panel

**Files:**
- Modify: `audita-web/components/cr/CrDeploymentPanel.vue`

- [ ] **Step 1: Remove approvers list and approve/reject UI**

Delete the read-only approvers list section and the approve/reject buttons + reject modal.

- [ ] **Step 2: Add assignee selection UI**

```vue
<div v-if="deployment.status === 'PENDING'" class="assignee-section">
  <div v-if="!deployment.assignee">
    <button @click="showAssigneeSearch = true" v-if="canManage">
      Assign Deployer
    </button>
  </div>
  <div v-else>
    <p>Deployer: {{ deployment.assignee.fullName }} ({{ deployment.assignee.email }})</p>
    <button @click="showAssigneeSearch = true" v-if="canManage">
      Change Assignee
    </button>
  </div>
</div>
```

Add user search component (reuse the same search pattern as approver search).

- [ ] **Step 3: Add "Mark Deployment Completed" button**

```vue
<button
  v-if="canCompleteDeployment"
  @click="handleComplete"
  class="btn-primary"
>
  Mark Deployment Completed
</button>
```

```typescript
const canCompleteDeployment = computed(() => {
  if (!deployment.value || deployment.value.status !== "PENDING") return false;
  if (!deployment.value.assignee) return false;
  return auth.userId === deployment.value.assignee.id || auth.isAdmin || auth.isSuperAdmin;
});
```

- [ ] **Step 4: Update canManage**

```typescript
const canManage = computed(() => {
  if (!deployment.value || deployment.value.status !== "PENDING") return false;
  if (!auth.hasPermission("cr.manage_participants")) return false;
  return auth.isSuperAdmin || auth.isAdmin || isCreator.value;
});
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: CrDeploymentPanel — single assignee model, complete button, remove approvers"
```

---

## Task 21: Frontend — Roles Admin Page

**Files:**
- Modify: `audita-web/pages/admin/roles/index.vue`

- [ ] **Step 1: Add Create Role button and modal**

Add a "Create Role" button (gated by `auth.hasPermission("roles.manage")`). Modal with:
- Name input
- Description input
- Permission picker: checkbox grid grouped by category (use the grouping from spec section 8.4)
- Fetch permissions from `GET /api/v1/roles/permissions`
- Live validation for full-permission and duplicate-set

- [ ] **Step 2: Add Edit Role modal**

Same as create but pre-populated. System roles don't show edit button.

- [ ] **Step 3: Add Delete Role button and confirmation**

Shows assigned user count (from role response or a separate API call). Blocks if > 0. Calls `DELETE /api/v1/roles/{id}`.

- [ ] **Step 4: Add roles composable or API calls**

Add functions to fetch permissions catalogue, create role, update role, delete role. Add to `useChangeRequests.ts` or create a new `useRoles.ts` composable.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: admin roles page — full CRUD with permission picker"
```

---

## Task 22: Frontend — Misc Updates

**Files:**
- Modify: `audita-web/middleware/auth.global.ts`
- Modify: `audita-web/pages/audit-trail/index.vue`
- Modify: `audita-web/components/cr/CrCompletionStatusControl.vue`
- Modify: `audita-web/components/shared/AppSidebar.vue`
- Modify: `audita-web/layouts/default.vue`

- [ ] **Step 1: Update auth.global.ts — preserve redirect query param**

```typescript
if (!auth.isAuthenticated) {
    const redirect = encodeURIComponent(to.fullPath);
    return navigateTo(`/auth/sign-in?redirect=${redirect}`);
}
```

- [ ] **Step 2: Update audit-trail/index.vue — add new action types**

Add to the `ACTION_TYPES` array:
```typescript
"CR_WATCHER_ADDED", "CR_WATCHER_REMOVED", "CR_WATCHER_PROMOTED", "CR_APPROVER_DEMOTED",
"UAT_WATCHER_ADDED", "UAT_WATCHER_REMOVED", "UAT_WATCHER_PROMOTED", "UAT_APPROVER_DEMOTED",
"DEPLOYMENT_ASSIGNEE_SET", "DEPLOYMENT_ASSIGNEE_CHANGED", "DEPLOYMENT_COMPLETED",
"ROLE_CREATED", "ROLE_UPDATED", "ROLE_DELETED",
```

Add labels for each in the label mapping.

- [ ] **Step 3: Update CrCompletionStatusControl.vue**

Gate "Mark Complete" by `canMarkComplete` that checks:
- `completionStatus === "IN_PROGRESS"`
- `approvalStatus === "APPROVED"`
- For DELIVERY_PIPELINE: `deploymentDone === true`
- User has `cr.manage_participants` permission (requester or admin)

- [ ] **Step 4: Update AppSidebar.vue and default.vue**

Replace `auth.isAdmin` with `auth.hasPermission("users.manage")` for Users link.
Replace `auth.isAdmin || auth.isAuditor` with `auth.hasPermission("audit.view")` for Audit Trail link.
Replace `auth.isAdmin` with `auth.hasPermission("settings.manage")` for Settings link.
Replace `auth.canCreateCR` with `auth.hasPermission("cr.create")` for New Request button.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat: frontend misc — redirect after login, audit action types, permission-based sidebar"
```

---

## Task 23: Integration Verification

- [ ] **Step 1: Drop the database**

```bash
# Connect to PostgreSQL and drop the audita database
# The database will be recreated by Flyway on next startup
```

- [ ] **Step 2: Build and start the backend**

Run: `cd audita-api && ./gradlew bootRun --console=plain 2>&1 | tail -30`
Expected: Application starts, Flyway runs migrations, no errors.

- [ ] **Step 3: Verify log file creation**

Check that `audita-api/logs/audita-api.log` exists and contains startup logs.

- [ ] **Step 4: Build and start the frontend**

Run: `cd audita-web && pnpm dev`
Expected: Frontend starts without errors.

- [ ] **Step 5: Manual smoke test**

1. Log in as Admin (from sample data: `sarah.chen@acme-demo.io` / `Password@2026`)
2. Verify sidebar shows correct links based on permissions
3. Create a change request (APPROVAL_ONLY)
4. Add approvers — verify no Required/Optional toggle
5. Add watchers — verify mutual exclusivity with approvers
6. Submit the request
7. Log in as a different user (Requester) who is an approver
8. Approve the request
9. Verify "Mark Complete" button appears after all approvals
10. Mark complete — verify read-only banner appears
11. Create a DELIVERY_PIPELINE request
12. Go through approval -> UAT -> promote to deployment
13. Assign a deployer
14. Log in as deployer, mark deployment completed
15. Log in as requester, mark CR complete
16. Verify Copy URL button works
17. Log out, paste the copied URL, verify redirect to sign-in, log in, verify redirect to the request
18. Log in as Auditor — verify read-only access to all requests, audit trail access, no action buttons

- [ ] **Step 6: Run backend tests**

Run: `cd audita-api && ./gradlew test --console=plain 2>&1 | tail -30`
Fix any test failures caused by the refactoring (tests that reference `isRequired`, `Approver` role, deployment approvers, etc.).

- [ ] **Step 7: Run frontend typecheck**

Run: `cd audita-web && pnpm typecheck 2>&1 | tail -30`
Fix any type errors.

- [ ] **Step 8: Run frontend lint**

Run: `cd audita-web && pnpm lint 2>&1 | tail -30`
Fix any lint errors.

- [ ] **Step 9: Commit final**

```bash
git add -A
git commit -m "test: fix test failures from RBAC refactor, verify integration"
```

---

## Self-Review Notes

### Spec Coverage Check

| Spec Section | Task(s) |
|---|---|
| 2. RBAC Permission Enforcement | Tasks 1, 4, 5, 9-13 |
| 3. Watchers | Tasks 1, 2, 3, 6, 7, 9, 10, 14, 17, 18, 19 |
| 4. Deployment Single Assignee | Tasks 1, 2, 3, 8, 11, 17, 20 |
| 5. All Approvers Required | Tasks 1, 2, 6, 7, 9, 10, 14 |
| 6. Read-Only Boundary | Tasks 2, 6, 7, 8, 18, 20, 22 |
| 7. Visibility Rules | Task 6 |
| 8. Custom Role Management | Tasks 5, 12, 21 |
| 9. Self-Approval Rule | Task 6 (existing behavior preserved) |
| 10. Shareable Request Links | Tasks 18, 22 |
| 11. Sample Data Updates | Task 14 |
| 12. Audit Trail Coverage | Tasks 6, 7, 8, 22 |
| 13. Logging | Task 15 |
| 14. Schema & Migration | Task 1 |

All spec sections are covered by at least one task.
