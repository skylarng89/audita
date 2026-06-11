-- Revoke UPDATE and DELETE on platform_audit_log from the application role
REVOKE UPDATE, DELETE ON platform_audit_log FROM PUBLIC;

-- Create a trigger function that prevents UPDATE and DELETE on platform_audit_log
CREATE OR REPLACE FUNCTION prevent_platform_audit_log_mutation()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'platform_audit_log is append-only: % operation not permitted', TG_OP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Attach trigger for UPDATE
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'platform_audit_log_no_update'
    ) THEN
        CREATE TRIGGER platform_audit_log_no_update
            BEFORE UPDATE ON platform_audit_log
            FOR EACH ROW
            EXECUTE FUNCTION prevent_platform_audit_log_mutation();
    END IF;
END
$$;

-- Attach trigger for DELETE
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger
        WHERE tgname = 'platform_audit_log_no_delete'
    ) THEN
        CREATE TRIGGER platform_audit_log_no_delete
            BEFORE DELETE ON platform_audit_log
            FOR EACH ROW
            EXECUTE FUNCTION prevent_platform_audit_log_mutation();
    END IF;
END
$$;
