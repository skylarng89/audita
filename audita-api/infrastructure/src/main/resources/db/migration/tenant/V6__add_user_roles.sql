-- Support multiple roles per user while keeping users.role_id as the primary/effective role.
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);

-- Backfill existing single-role users into the new join table.
INSERT INTO user_roles (user_id, role_id)
SELECT id, role_id
FROM users
WHERE role_id IS NOT NULL
ON CONFLICT DO NOTHING;