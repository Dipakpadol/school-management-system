ALTER TABLE notification_logs
    ADD COLUMN IF NOT EXISTS provider VARCHAR(80),
    ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(180),
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS reference_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS failed_at TIMESTAMP WITH TIME ZONE;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notification_logs_status') THEN
        ALTER TABLE notification_logs DROP CONSTRAINT chk_notification_logs_status;
    END IF;
    ALTER TABLE notification_logs
        ADD CONSTRAINT chk_notification_logs_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING', 'SKIPPED'));

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notifications_status') THEN
        ALTER TABLE notifications DROP CONSTRAINT chk_notifications_status;
    END IF;
    ALTER TABLE notifications
        ADD CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING', 'SKIPPED'));

    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_notification_recipients_status') THEN
        ALTER TABLE notification_recipients DROP CONSTRAINT chk_notification_recipients_status;
    END IF;
    ALTER TABLE notification_recipients
        ADD CONSTRAINT chk_notification_recipients_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING', 'SKIPPED'));
END $$;

CREATE INDEX IF NOT EXISTS idx_notification_logs_reference
    ON notification_logs (reference_type, reference_id, channel, status, created_at)
    WHERE deleted = FALSE;

INSERT INTO notification_channel_configs (channel, provider_name, enabled, metadata_json, created_by, updated_by)
VALUES
    ('EMAIL', 'smtp', TRUE, '{"mode":"configured-by-environment"}', 'flyway', 'flyway'),
    ('SMS', 'none', FALSE, '{"mode":"not-configured"}', 'flyway', 'flyway'),
    ('WHATSAPP', 'none', FALSE, '{"mode":"not-configured"}', 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

UPDATE notification_channel_configs
SET provider_name = CASE channel
        WHEN 'EMAIL' THEN 'smtp'
        ELSE 'none'
    END,
    enabled = CASE channel
        WHEN 'EMAIL' THEN TRUE
        ELSE FALSE
    END,
    metadata_json = CASE channel
        WHEN 'EMAIL' THEN '{"mode":"configured-by-environment"}'
        ELSE '{"mode":"not-configured"}'
    END,
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE channel IN ('EMAIL', 'SMS', 'WHATSAPP')
  AND deleted = FALSE;

ALTER TABLE audit_log
    ALTER COLUMN entity_id TYPE VARCHAR(120) USING entity_id::text,
    ALTER COLUMN resource_id TYPE VARCHAR(120) USING resource_id::text,
    ALTER COLUMN metadata TYPE TEXT USING metadata::text,
    ALTER COLUMN old_value TYPE TEXT USING old_value::text,
    ALTER COLUMN new_value TYPE TEXT USING new_value::text;
