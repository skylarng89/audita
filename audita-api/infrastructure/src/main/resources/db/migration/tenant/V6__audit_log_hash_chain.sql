-- Add cryptographic hash chain columns for tamper-evident immutability.
-- record_hash is SHA-256 of the canonical serialisation of all record
-- fields concatenated with the previous record's hash. This forms a
-- linked chain: altering any record invalidates all subsequent hashes.
--
-- chain_index is a monotonically increasing sequence used to order the
-- chain. It is NOT the primary key — the UUID id remains the PK.

ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS chain_index   BIGINT,
    ADD COLUMN IF NOT EXISTS record_hash   BYTEA,
    ADD COLUMN IF NOT EXISTS previous_hash BYTEA;

-- Backfill chain_index for existing rows (ordered by creation time).
-- Existing rows will have NULL record_hash and previous_hash — the
-- verification logic treats NULL hashes as "pre-chain, unverifiable"
-- rather than "tampered".
WITH ordered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at ASC) AS rn
    FROM audit_log
    WHERE chain_index IS NULL
)
UPDATE audit_log al
SET chain_index = ordered.rn
FROM ordered
WHERE al.id = ordered.id;

-- Create a sequence for new rows so chain_index is atomic and gapless
-- within a transaction boundary. We start beyond the max existing index.
DO $$
DECLARE
    max_idx BIGINT;
BEGIN
    SELECT COALESCE(MAX(chain_index), 0) INTO max_idx FROM audit_log;
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS audit_log_chain_seq START WITH %s', max_idx + 1);
END
$$;

-- Index for ordering the chain during verification
CREATE INDEX IF NOT EXISTS idx_audit_log_chain_index ON audit_log(chain_index ASC);
