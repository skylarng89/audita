-- Add optional refresh token context binding fields for session hardening.
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS user_agent_hash VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ip_hash VARCHAR(255);
