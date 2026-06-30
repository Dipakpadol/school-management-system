CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_code VARCHAR(100) NOT NULL,
    template_name VARCHAR(160) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(180),
    body TEXT NOT NULL,
    variables_json TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_templates_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH')),
    CONSTRAINT chk_notification_templates_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX ux_notification_templates_code_active
    ON notification_templates (LOWER(template_code))
    WHERE deleted = FALSE;

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID REFERENCES notification_templates (id),
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(180),
    body TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scheduled_at TIMESTAMP WITH TIME ZONE,
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notifications_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH')),
    CONSTRAINT chk_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING'))
);

CREATE TABLE notification_recipients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notifications (id),
    recipient VARCHAR(180) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_recipients_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING'))
);

CREATE TABLE notification_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(180) NOT NULL,
    subject VARCHAR(180),
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    provider_response TEXT,
    error_message TEXT,
    sent_at TIMESTAMP WITH TIME ZONE,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_logs_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH')),
    CONSTRAINT chk_notification_logs_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRY_PENDING'))
);

CREATE INDEX idx_notification_logs_created_at
    ON notification_logs (created_at DESC)
    WHERE deleted = FALSE;

CREATE TABLE notification_channel_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel VARCHAR(30) NOT NULL,
    provider_name VARCHAR(80) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_channel_configs_channel CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH'))
);

CREATE UNIQUE INDEX ux_notification_channel_configs_channel_active
    ON notification_channel_configs (channel)
    WHERE deleted = FALSE;

INSERT INTO notification_channel_configs (channel, provider_name, enabled, metadata_json, created_by, updated_by)
VALUES
    ('EMAIL', 'console', TRUE, '{"mode":"mock"}', 'flyway', 'flyway'),
    ('SMS', 'console', TRUE, '{"mode":"mock"}', 'flyway', 'flyway'),
    ('WHATSAPP', 'console', TRUE, '{"mode":"mock"}', 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO notification_templates
    (template_code, template_name, channel, subject, body, variables_json, status, created_by, updated_by)
VALUES
    ('FEE_DUE_EMAIL', 'Fee Due Email', 'EMAIL', 'Fee reminder', 'Dear {{parentName}}, fee is due for {{studentName}}.', '["parentName","studentName"]', 'ACTIVE', 'flyway', 'flyway'),
    ('ABSENCE_SMS', 'Absence SMS', 'SMS', NULL, 'Student {{studentName}} is marked absent today.', '["studentName"]', 'ACTIVE', 'flyway', 'flyway'),
    ('HOSTEL_FEE_WHATSAPP', 'Hostel Fee WhatsApp', 'WHATSAPP', NULL, 'Hostel fee reminder for {{studentName}}.', '["studentName"]', 'ACTIVE', 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

UPDATE menu_items
SET route_path = '/notifications',
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE module_id = 'notifications'
  AND deleted = FALSE;
