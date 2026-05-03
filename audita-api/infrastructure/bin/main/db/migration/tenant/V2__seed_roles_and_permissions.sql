-- ============================================================
-- Seed built-in roles and permissions
-- These are immutable (is_system = TRUE). Custom roles can be
-- created by Admins and reference the same permission set.
-- ============================================================

-- Permissions
INSERT INTO permissions (code, label) VALUES
    ('cr.create',           'Create Change Requests'),
    ('cr.view',             'View Change Requests'),
    ('cr.edit',             'Edit Change Requests'),
    ('cr.cancel',           'Cancel Change Requests'),
    ('cr.submit',           'Submit Change Requests for Approval'),
    ('cr.approve',          'Approve / Reject Change Requests'),
    ('users.view',          'View Users'),
    ('users.manage',        'Invite, Edit and Deactivate Users'),
    ('roles.view',          'View Roles'),
    ('roles.manage',        'Create and Manage Custom Roles'),
    ('groups.view',         'View Groups'),
    ('groups.manage',       'Create and Manage Groups'),
    ('settings.view',       'View Organisation Settings'),
    ('settings.manage',     'Manage Organisation Settings'),
    ('sla.view',            'View SLA Policies'),
    ('sla.manage',          'Create and Manage SLA Policies'),
    ('audit.view',          'View Audit Trail'),
    ('audit.export',        'Export Audit Trail to CSV')
ON CONFLICT (code) DO NOTHING;

-- Built-in roles
INSERT INTO roles (id, name, description, is_system) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Admin',     'Full organisation management',                             TRUE),
    ('00000000-0000-0000-0000-000000000002', 'Requester', 'Can create and manage change requests',                   TRUE),
    ('00000000-0000-0000-0000-000000000003', 'Approver',  'Can review and action change requests',                   TRUE),
    ('00000000-0000-0000-0000-000000000004', 'Auditor',   'Read-only access across the organisation',                TRUE)
ON CONFLICT (id) DO NOTHING;

-- Admin gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000001', id FROM permissions
ON CONFLICT DO NOTHING;

-- Requester permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000002', id FROM permissions
WHERE code IN ('cr.create', 'cr.view', 'cr.edit', 'cr.cancel', 'cr.submit',
               'users.view', 'groups.view', 'settings.view', 'sla.view')
ON CONFLICT DO NOTHING;

-- Approver permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000003', id FROM permissions
WHERE code IN ('cr.view', 'cr.approve', 'users.view', 'groups.view',
               'settings.view', 'sla.view')
ON CONFLICT DO NOTHING;

-- Auditor permissions (read-only)
INSERT INTO role_permissions (role_id, permission_id)
SELECT '00000000-0000-0000-0000-000000000004', id FROM permissions
WHERE code IN ('cr.view', 'users.view', 'groups.view', 'roles.view',
               'settings.view', 'sla.view', 'audit.view', 'audit.export')
ON CONFLICT DO NOTHING;
