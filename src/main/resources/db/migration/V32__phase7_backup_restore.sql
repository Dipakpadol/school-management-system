CREATE TABLE IF NOT EXISTS system_backups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    file_name VARCHAR(240) NOT NULL,
    storage_path VARCHAR(700) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    size_bytes BIGINT,
    created_by_user VARCHAR(120),
    database_version VARCHAR(80),
    application_version VARCHAR(120),
    checksum_sha256 VARCHAR(64),
    error_message VARCHAR(1000),
    notes VARCHAR(1000),
    postgres_tool_version VARCHAR(160),
    format VARCHAR(40),
    pre_restore_safety BOOLEAN NOT NULL DEFAULT FALSE,
    source_backup_id UUID REFERENCES system_backups (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_system_backups_type CHECK (backup_type IN ('DATABASE')),
    CONSTRAINT chk_system_backups_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED', 'RESTORED', 'DELETED')),
    CONSTRAINT chk_system_backups_size CHECK (size_bytes IS NULL OR size_bytes >= 0),
    CONSTRAINT chk_system_backups_checksum CHECK (checksum_sha256 IS NULL OR checksum_sha256 ~ '^[a-fA-F0-9]{64}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_system_backups_storage_path_active
    ON system_backups (storage_path)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_system_backups_status_started
    ON system_backups (status, started_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_system_backups_completed_at
    ON system_backups (completed_at DESC)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS system_restore_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    backup_id UUID NOT NULL REFERENCES system_backups (id),
    safety_backup_id UUID REFERENCES system_backups (id),
    status VARCHAR(30) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE,
    initiated_by VARCHAR(120),
    confirmation_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    error_message VARCHAR(1000),
    notes VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_system_restore_history_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_system_restore_history_started
    ON system_restore_history (started_at DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_system_restore_history_backup
    ON system_restore_history (backup_id)
    WHERE deleted = FALSE;

WITH setting_seed (group_name, setting_key, setting_value) AS (
    VALUES
        ('backup', 'backupEnabled', 'false'),
        ('backup', 'backupFrequency', 'DAILY'),
        ('backup', 'backupTime', '02:00'),
        ('backup', 'backupRetentionDays', '30'),
        ('backup', 'backupRetentionCount', '10'),
        ('backup', 'backupLocation', '')
)
INSERT INTO application_settings (group_name, setting_key, setting_value, created_by, updated_by)
SELECT seed.group_name, seed.setting_key, seed.setting_value, 'flyway', 'flyway'
FROM setting_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM application_settings existing
    WHERE LOWER(existing.group_name) = LOWER(seed.group_name)
      AND LOWER(existing.setting_key) = LOWER(seed.setting_key)
      AND existing.deleted = FALSE
);

WITH permission_seed (code, name, description, module_name) AS (
    VALUES
        ('BACKUP_READ', 'Read backups', 'Allows reading backup metadata, restore history, and operational backup status.', 'BACKUP'),
        ('BACKUP_CREATE', 'Create backups', 'Allows creating manual database backups.', 'BACKUP'),
        ('BACKUP_DOWNLOAD', 'Download backups', 'Allows securely downloading completed backup archives.', 'BACKUP'),
        ('BACKUP_DELETE', 'Delete backups', 'Allows deleting backup archives according to retention policy.', 'BACKUP'),
        ('BACKUP_RESTORE', 'Restore backups', 'Allows restoring the PostgreSQL database from a completed backup.', 'BACKUP')
)
INSERT INTO permissions (code, name, description, module_name, status, created_by, updated_by)
SELECT seed.code, seed.name, seed.description, seed.module_name, 'ACTIVE', 'flyway', 'flyway'
FROM permission_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions existing
    WHERE existing.code = seed.code
      AND existing.deleted = FALSE
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'BACKUP_READ', 'BACKUP_CREATE', 'BACKUP_DOWNLOAD', 'BACKUP_DELETE', 'BACKUP_RESTORE'
)
WHERE roles.name = 'SUPER_ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'BACKUP_READ', 'BACKUP_CREATE', 'BACKUP_DOWNLOAD', 'BACKUP_DELETE'
)
WHERE roles.name = 'ADMIN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('backup', 'Backup & Restore', '/modules/backup', 'BACKUP_READ', 'backup', 130, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id = 'backup'
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
