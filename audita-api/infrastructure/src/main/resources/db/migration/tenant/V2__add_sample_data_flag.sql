-- V2: Add is_sample flag to all tenant tables that can contain sample data.
-- Enables efficient identification and removal of demo/sample data without
-- affecting real user data.  Partial indexes keep the index tiny since sample
-- rows are a tiny fraction of production data.

ALTER TABLE users ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE groups ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE group_members ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE change_requests ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE cr_approvers ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE comment_mentions ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE custom_field_definitions ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE change_request_custom_fields ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE activity_stream ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE default_approvers ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE attachments ADD COLUMN IF NOT EXISTS is_sample BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_users_is_sample ON users (is_sample) WHERE is_sample = TRUE;
CREATE INDEX IF NOT EXISTS idx_groups_is_sample ON groups (is_sample) WHERE is_sample = TRUE;
CREATE INDEX IF NOT EXISTS idx_change_requests_is_sample ON change_requests (is_sample) WHERE is_sample = TRUE;
CREATE INDEX IF NOT EXISTS idx_custom_field_defs_is_sample ON custom_field_definitions (is_sample) WHERE is_sample = TRUE;