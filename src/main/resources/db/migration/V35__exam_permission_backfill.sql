WITH permission_seed (code, name, description, module_name) AS (
    VALUES
        ('EXAMS_READ', 'Read exams', 'Allows reading exam schedules, marks, results, and exam reports.', 'EXAMS'),
        ('EXAMS_MANAGE', 'Manage exams', 'Allows managing exam schedules, marks, and result publication.', 'EXAMS')
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

UPDATE permissions
SET name = CASE code
        WHEN 'EXAMS_READ' THEN 'Read exams'
        WHEN 'EXAMS_MANAGE' THEN 'Manage exams'
        ELSE name
    END,
    description = CASE code
        WHEN 'EXAMS_READ' THEN 'Allows reading exam schedules, marks, results, and exam reports.'
        WHEN 'EXAMS_MANAGE' THEN 'Allows managing exam schedules, marks, and result publication.'
        ELSE description
    END,
    module_name = 'EXAMS',
    status = 'ACTIVE',
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE code IN ('EXAMS_READ', 'EXAMS_MANAGE')
  AND deleted = FALSE;

WITH role_permission_seed (role_name, permission_code) AS (
    VALUES
        ('SUPER_ADMIN', 'EXAMS_READ'),
        ('SUPER_ADMIN', 'EXAMS_MANAGE'),
        ('ADMIN', 'EXAMS_READ'),
        ('ADMIN', 'EXAMS_MANAGE'),
        ('PRINCIPAL', 'EXAMS_READ'),
        ('PRINCIPAL', 'EXAMS_MANAGE')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM role_permission_seed seed
JOIN roles ON roles.name = seed.role_name
JOIN permissions ON permissions.code = seed.permission_code
WHERE roles.deleted = FALSE
  AND permissions.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );
