ALTER TABLE students
    ADD COLUMN IF NOT EXISTS user_account_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_students_user_account'
    ) THEN
        ALTER TABLE students
            ADD CONSTRAINT fk_students_user_account
            FOREIGN KEY (user_account_id) REFERENCES user_accounts (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_students_user_account_active
    ON students (user_account_id)
    WHERE deleted = FALSE
      AND user_account_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_students_user_account_active
    ON students (user_account_id)
    WHERE deleted = FALSE
      AND user_account_id IS NOT NULL;

UPDATE students student
SET user_account_id = account.id
FROM user_accounts account
JOIN user_roles user_role ON user_role.user_id = account.id
JOIN roles role ON role.id = user_role.role_id
WHERE student.user_account_id IS NULL
  AND student.email IS NOT NULL
  AND LOWER(student.email) = LOWER(account.email)
  AND account.deleted = FALSE
  AND role.deleted = FALSE
  AND role.name = 'STUDENT'
  AND student.deleted = FALSE;

UPDATE students student
SET user_account_id = account.id
FROM user_accounts account
WHERE student.user_account_id IS NULL
  AND student.deleted = FALSE
  AND account.deleted = FALSE
  AND (
      (student.admission_number = 'ADM-DEMO-0001' AND account.email = 'student1@school.com')
      OR (student.admission_number = 'ADM-2026-0001' AND account.email = 'student@school.test')
  );

UPDATE parents parent
SET user_account_id = account.id
FROM user_accounts account
WHERE parent.user_account_id IS NULL
  AND parent.deleted = FALSE
  AND account.deleted = FALSE
  AND (
      LOWER(parent.email) = LOWER(account.email)
      OR (parent.email = 'rajesh.sharma@parent.school.test' AND account.email = 'parent1@school.com')
  );

WITH permission_seed (code, name, description, module_name) AS (
    VALUES
        ('PORTAL_STUDENT_READ', 'Read student portal', 'Allows a student to read only their own scoped portal data.', 'PORTAL'),
        ('PORTAL_PARENT_READ', 'Read parent portal', 'Allows a parent to read only linked children portal data.', 'PORTAL'),
        ('PORTAL_TEACHER_READ', 'Read teacher portal', 'Allows a teacher to read only assigned teacher portal data.', 'PORTAL'),
        ('PORTAL_TEACHER_ATTENDANCE', 'Manage teacher portal attendance', 'Allows a teacher to read and mark attendance only for assigned classes.', 'PORTAL'),
        ('PORTAL_TEACHER_MARKS', 'Manage teacher portal marks', 'Allows a teacher to read and enter marks only for assigned subjects.', 'PORTAL')
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

DELETE FROM role_permissions existing
USING roles role, permissions permission
WHERE existing.role_id = role.id
  AND existing.permission_id = permission.id
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND role.name IN ('STUDENT', 'PARENT')
  AND permission.code IN (
      'STUDENTS_READ',
      'ACADEMIC_READ',
      'ATTENDANCE_READ',
      'FEES_READ',
      'COMMUNICATION_READ',
      'LIBRARY_READ',
      'NOTIFICATIONS_SEND'
  );

DELETE FROM role_permissions existing
USING roles role, permissions permission
WHERE existing.role_id = role.id
  AND existing.permission_id = permission.id
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND role.name = 'TEACHER'
  AND permission.code IN (
      'STUDENTS_READ',
      'ACADEMIC_READ',
      'ATTENDANCE_READ',
      'ATTENDANCE_MARK',
      'REPORTS_READ',
      'COMMUNICATION_READ',
      'LIBRARY_READ'
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN ('PORTAL_STUDENT_READ')
WHERE role.name = 'STUDENT'
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN ('PORTAL_PARENT_READ')
WHERE role.name = 'PARENT'
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN (
    'PORTAL_TEACHER_READ',
    'PORTAL_TEACHER_ATTENDANCE',
    'PORTAL_TEACHER_MARKS'
)
WHERE role.name = 'TEACHER'
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT role.id, permission.id
FROM roles role
JOIN permissions permission ON permission.code IN (
    'PORTAL_STUDENT_READ',
    'PORTAL_PARENT_READ',
    'PORTAL_TEACHER_READ',
    'PORTAL_TEACHER_ATTENDANCE',
    'PORTAL_TEACHER_MARKS'
)
WHERE role.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL')
  AND role.deleted = FALSE
  AND permission.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('student-portal', 'Student Portal', '/portal/student', 'PORTAL_STUDENT_READ', 'school', 5, 'flyway', 'flyway'),
    ('parent-portal', 'Parent Portal', '/portal/parent', 'PORTAL_PARENT_READ', 'family_restroom', 6, 'flyway', 'flyway'),
    ('teacher-portal', 'Teacher Portal', '/portal/teacher', 'PORTAL_TEACHER_READ', 'co_present', 7, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT role.id, menu.id
FROM roles role
JOIN menu_items menu ON (
    (role.name = 'STUDENT' AND menu.module_id = 'student-portal')
    OR (role.name = 'PARENT' AND menu.module_id = 'parent-portal')
    OR (role.name = 'TEACHER' AND menu.module_id = 'teacher-portal')
    OR (role.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL')
        AND menu.module_id IN ('student-portal', 'parent-portal', 'teacher-portal'))
)
WHERE role.deleted = FALSE
  AND menu.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = role.id
        AND existing.menu_item_id = menu.id
  );
