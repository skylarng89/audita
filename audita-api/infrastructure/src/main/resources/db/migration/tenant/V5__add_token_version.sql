-- Add token_version column for JWT token revocability (SA14-005)
-- Existing users default to 0 (first version)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;
