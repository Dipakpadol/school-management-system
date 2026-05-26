CREATE TABLE menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_id VARCHAR(80) NOT NULL,
    title VARCHAR(160) NOT NULL,
    route_path VARCHAR(200) NOT NULL,
    required_permission VARCHAR(120),
    icon_key VARCHAR(80),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_menu_items_module_active
    ON menu_items (module_id)
    WHERE deleted = FALSE;

CREATE TABLE role_menu_items (
    role_id UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    menu_item_id UUID NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, menu_item_id)
);

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('students', 'Student Management', '/students', 'STUDENTS_READ', 'school', 10, 'flyway', 'flyway'),
    ('fees', 'Fees Management', '/fees', 'FEES_READ', 'payments', 20, 'flyway', 'flyway'),
    ('users', 'User Management', '/users', 'USERS_READ', 'users', 30, 'flyway', 'flyway'),
    ('audit-logs', 'Audit Logs', '/audit-logs', 'AUDIT_LOGS_READ', 'audit', 40, 'flyway', 'flyway'),
    ('academic', 'Academic Management', '/modules/academic', 'ACADEMIC_READ', 'academic', 50, 'flyway', 'flyway'),
    ('hostel', 'Hostel Management', '/modules/hostel', 'HOSTEL_READ', 'hostel', 60, 'flyway', 'flyway'),
    ('attendance', 'Attendance', '/modules/attendance', 'ATTENDANCE_READ', 'attendance', 70, 'flyway', 'flyway'),
    ('exams', 'Exams & Results', '/modules/exams', 'EXAMS_READ', 'exams', 80, 'flyway', 'flyway'),
    ('reports', 'Reports', '/modules/reports', 'REPORTS_READ', 'reports', 90, 'flyway', 'flyway'),
    ('notifications', 'Notifications', '/modules/notifications', 'NOTIFICATIONS_READ', 'notifications', 100, 'flyway', 'flyway'),
    ('settings', 'Settings', '/modules/settings', 'SETTINGS_READ', 'settings', 110, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
CROSS JOIN menu_items
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN (
    'students',
    'academic',
    'attendance',
    'exams',
    'reports',
    'notifications',
    'settings'
)
WHERE roles.name = 'PRINCIPAL'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN (
    'students',
    'academic',
    'attendance',
    'exams',
    'reports'
)
WHERE roles.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('students', 'fees', 'reports')
WHERE roles.name = 'ACCOUNTANT'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('students', 'fees')
WHERE roles.name = 'RECEPTIONIST'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('students', 'hostel', 'attendance', 'reports')
WHERE roles.name = 'WARDEN'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('students', 'academic', 'attendance', 'fees')
WHERE roles.name IN ('STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
