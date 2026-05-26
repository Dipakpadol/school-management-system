WITH permission_seed (code, name, description) AS (
    VALUES
        ('EXAMS_READ', 'Read exams', 'Allows reading exam schedules, marks, grades, and result records.'),
        ('EXAMS_MANAGE', 'Manage exams', 'Allows managing exam schedules, marks, grades, and result records.'),
        ('NOTIFICATIONS_READ', 'Read notifications', 'Allows reading notification templates and history.'),
        ('NOTIFICATIONS_MANAGE', 'Manage notifications', 'Allows managing notification templates and history.'),
        ('REPORTS_MANAGE', 'Manage reports', 'Allows managing report definitions and saved report configurations.')
)
INSERT INTO permissions (code, name, description, created_by, updated_by)
SELECT seed.code, seed.name, seed.description, 'flyway', 'flyway'
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
    'EXAMS_READ',
    'EXAMS_MANAGE',
    'NOTIFICATIONS_READ',
    'NOTIFICATIONS_MANAGE',
    'REPORTS_MANAGE'
)
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN')
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
    'EXAMS_READ',
    'EXAMS_MANAGE',
    'NOTIFICATIONS_READ',
    'NOTIFICATIONS_MANAGE',
    'REPORTS_MANAGE'
)
WHERE roles.name = 'PRINCIPAL'
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
    'EXAMS_READ',
    'NOTIFICATIONS_READ'
)
WHERE roles.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );
