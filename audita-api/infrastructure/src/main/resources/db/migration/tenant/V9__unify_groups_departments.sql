ALTER TABLE groups
    ADD COLUMN IF NOT EXISTS is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS display_order INT     NOT NULL DEFAULT 0;

ALTER TABLE change_requests
    ADD COLUMN IF NOT EXISTS request_group_id       UUID REFERENCES groups(id),
    ADD COLUMN IF NOT EXISTS destination_group_id   UUID REFERENCES groups(id);
