-- Drop duplicate index on audit_log(created_at DESC).
-- idx_audit_created (V1 line 358) is identical to idx_audit_log_created (V1 line 364).
-- Both covered the same column in the same order; every INSERT/UPDATE paid
-- the cost of maintaining both indexes with zero query benefit.
DROP INDEX IF EXISTS idx_audit_created;

-- Add IF NOT EXISTS guard to the FK constraint originally created in V1.
-- ALTER TABLE ... ADD CONSTRAINT does not support IF NOT EXISTS in PostgreSQL,
-- so we wrap it in a DO block with a pg_constraint check.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE c.conname = 'fk_attachment_comment'
          AND t.relname = 'attachments'
          AND n.nspname = current_schema()
    ) THEN
        ALTER TABLE attachments
            ADD CONSTRAINT fk_attachment_comment
            FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE SET NULL;
    END IF;
END
$$;
