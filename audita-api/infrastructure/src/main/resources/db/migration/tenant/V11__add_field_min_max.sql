ALTER TABLE custom_field_definitions
  ADD COLUMN min_value NUMERIC,
  ADD COLUMN max_value NUMERIC;

ALTER TABLE custom_field_definitions
  ADD CONSTRAINT chk_cf_min_max CHECK (min_value IS NULL OR max_value IS NULL OR min_value <= max_value);
