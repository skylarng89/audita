-- Repair migration for legacy tenant schemas where refresh_tokens is missing.
-- This runs after V9 and is idempotent.

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash      VARCHAR(255) NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent_hash VARCHAR(255),
    ip_hash         VARCHAR(255)
);

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS user_agent_hash VARCHAR(255);

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS ip_hash VARCHAR(255);

ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_hash
    ON refresh_tokens(token_hash);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
