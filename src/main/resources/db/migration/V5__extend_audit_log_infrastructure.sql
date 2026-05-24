ALTER TABLE audit_log
    ADD COLUMN IF NOT EXISTS module_name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS entity_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS entity_id VARCHAR(120),
    ADD COLUMN IF NOT EXISTS old_value TEXT,
    ADD COLUMN IF NOT EXISTS new_value TEXT,
    ADD COLUMN IF NOT EXISTS performed_by VARCHAR(120),
    ADD COLUMN IF NOT EXISTS performed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(80);

UPDATE audit_log
SET module_name = COALESCE(module_name, resource_type, 'SYSTEM'),
    entity_name = COALESCE(entity_name, resource_type, 'UNKNOWN'),
    entity_id = COALESCE(entity_id, resource_id),
    performed_by = COALESCE(performed_by, actor, 'system'),
    performed_at = COALESCE(performed_at, created_at, CURRENT_TIMESTAMP)
WHERE module_name IS NULL
   OR entity_name IS NULL
   OR performed_by IS NULL
   OR performed_at IS NULL;

ALTER TABLE audit_log
    ALTER COLUMN module_name SET NOT NULL,
    ALTER COLUMN entity_name SET NOT NULL,
    ALTER COLUMN performed_by SET NOT NULL,
    ALTER COLUMN performed_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_log_module_action ON audit_log (module_name, action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log (entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_performed_by ON audit_log (performed_by);
CREATE INDEX IF NOT EXISTS idx_audit_log_performed_at ON audit_log (performed_at);

INSERT INTO permissions (code, name, description, created_by, updated_by)
SELECT 'AUDIT_LOGS_READ',
       'Read audit logs',
       'Allows reading system audit logs and activity history.',
       'flyway',
       'flyway'
WHERE NOT EXISTS (
    SELECT 1
    FROM permissions
    WHERE code = 'AUDIT_LOGS_READ'
      AND deleted = FALSE
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code = 'AUDIT_LOGS_READ'
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );
