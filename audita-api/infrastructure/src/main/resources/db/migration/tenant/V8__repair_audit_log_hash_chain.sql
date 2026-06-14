-- Idempotent repair: V6 was recorded in flyway_schema_history during a
-- checksum-mismatch repair cycle but the SQL was never applied. This
-- migration re-applies the audit log hash chain columns safely.
--
-- chain_index    - monotonically increasing sequence for chain ordering
-- record_hash    - SHA-256 of canonical serialisation + previous hash
-- previous_hash  - SHA-256 of the previous record in the chain

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS chain_index   BIGINT,
    ADD COLUMN IF NOT EXISTS record_hash   BYTEA,
    ADD COLUMN IF NOT EXISTS previous_hash BYTEA;

-- Backfill chain_index for existing rows ordered by creation time.
-- The V4 UPDATE trigger (audit_log_no_update) may or may not exist
-- depending on whether V4 was properly applied. Disable only if present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'audit_log_no_update'
          AND tgrelid = 'audit_log'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE audit_log DISABLE TRIGGER audit_log_no_update';
    END IF;
END
$$;

WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) AS rn
    FROM audit_log
    WHERE chain_index IS NULL
)
UPDATE audit_log al
SET chain_index = ordered.rn
FROM ordered
WHERE al.id = ordered.id;

-- Re-enable trigger if it was disabled
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'audit_log_no_update'
          AND tgrelid = 'audit_log'::regclass
    ) THEN
        EXECUTE 'ALTER TABLE audit_log ENABLE TRIGGER audit_log_no_update';
    END IF;
END
$$;

-- Sequence for atomic gapless chain_index on new rows
DO $$
DECLARE
    max_idx BIGINT;
BEGIN
    SELECT COALESCE(MAX(chain_index), 0) INTO max_idx FROM audit_log;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS audit_log_chain_seq START WITH %s', max_idx + 1);
END
$$;

-- Index for chain verification queries
CREATE INDEX IF NOT EXISTS idx_audit_log_chain_index ON audit_log(chain_index ASC);
