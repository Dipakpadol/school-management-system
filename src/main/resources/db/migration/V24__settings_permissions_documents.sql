ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS module_name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

UPDATE permissions
SET module_name = split_part(code, '_', 1)
WHERE module_name IS NULL OR module_name = '';

ALTER TABLE permissions
    ALTER COLUMN module_name SET NOT NULL;

CREATE TABLE IF NOT EXISTS application_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_name VARCHAR(80) NOT NULL,
    setting_key VARCHAR(120) NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_application_settings_group_key_active
    ON application_settings (LOWER(group_name), LOWER(setting_key))
    WHERE deleted = FALSE;
