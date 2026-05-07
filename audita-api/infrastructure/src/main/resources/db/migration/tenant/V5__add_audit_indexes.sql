-- ============================================================
-- Sprint 6: Performance indexes for global audit_log queries.
-- audit_log is append-only; indexes are safe to add at any time.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_audit_log_created     ON audit_log(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action_type ON audit_log(action_type);
CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id    ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity      ON audit_log(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_custom_field_defs_order ON custom_field_definitions(display_order);
