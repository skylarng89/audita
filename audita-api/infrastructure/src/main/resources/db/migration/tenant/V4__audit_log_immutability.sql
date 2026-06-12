-- Revoke UPDATE and DELETE on audit_log from the application role
REVOKE UPDATE, DELETE ON audit_log FROM PUBLIC;

-- Create a trigger function that prevents UPDATE and DELETE on audit_log
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'audit_log is append-only: % operation not permitted', TG_OP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger for UPDATE. The existence check is scoped to the current schema
-- (current_schema()) because pg_trigger is shared across all schemas and tests
-- create multiple tenant schemas in the same database.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger t
        JOIN pg_class c ON t.tgrelid = c.oid
        JOIN pg_namespace n ON c.relnamespace = n.oid
        WHERE t.tgname = 'audit_log_no_update'
          AND c.relname = 'audit_log'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TRIGGER audit_log_no_update
            BEFORE UPDATE ON audit_log
            FOR EACH ROW
            EXECUTE FUNCTION prevent_audit_log_mutation();
    END IF;
END
$$;

-- Attach trigger for DELETE. Same schema-scoped existence check.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger t
        JOIN pg_class c ON t.tgrelid = c.oid
        JOIN pg_namespace n ON c.relnamespace = n.oid
        WHERE t.tgname = 'audit_log_no_delete'
          AND c.relname = 'audit_log'
          AND n.nspname = current_schema()
    ) THEN
        CREATE TRIGGER audit_log_no_delete
            BEFORE DELETE ON audit_log
            FOR EACH ROW
            EXECUTE FUNCTION prevent_audit_log_mutation();
    END IF;
END
$$;
