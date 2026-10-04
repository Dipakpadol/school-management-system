WITH permission_seed (code, name, description, module_name) AS (
    VALUES
        ('TRANSPORT_READ', 'Read transport', 'Allows reading vehicles, routes, pickup points, and transport assignments.', 'TRANSPORT'),
        ('TRANSPORT_MANAGE', 'Manage transport', 'Allows managing vehicles, routes, pickup points, and transport assignments.', 'TRANSPORT'),
        ('LIBRARY_READ', 'Read library', 'Allows reading library catalog, copies, memberships, loans, fines, and library reports.', 'LIBRARY'),
        ('LIBRARY_CREATE', 'Create library records', 'Allows creating library masters, books, copies, and memberships.', 'LIBRARY'),
        ('LIBRARY_UPDATE', 'Update library records', 'Allows updating library masters, books, copies, memberships, and copy statuses.', 'LIBRARY'),
        ('LIBRARY_DELETE', 'Delete library records', 'Allows deactivating library books and memberships.', 'LIBRARY'),
        ('LIBRARY_ISSUE', 'Issue library books', 'Allows issuing library book copies to members.', 'LIBRARY'),
        ('LIBRARY_RETURN', 'Return library books', 'Allows returning or marking issued library books lost.', 'LIBRARY'),
        ('LIBRARY_FINE', 'Manage library fines', 'Allows paying and waiving library fines.', 'LIBRARY')
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
        WHEN 'TRANSPORT_READ' THEN 'Read transport'
        WHEN 'TRANSPORT_MANAGE' THEN 'Manage transport'
        WHEN 'LIBRARY_READ' THEN 'Read library'
        WHEN 'LIBRARY_CREATE' THEN 'Create library records'
        WHEN 'LIBRARY_UPDATE' THEN 'Update library records'
        WHEN 'LIBRARY_DELETE' THEN 'Delete library records'
        WHEN 'LIBRARY_ISSUE' THEN 'Issue library books'
        WHEN 'LIBRARY_RETURN' THEN 'Return library books'
        WHEN 'LIBRARY_FINE' THEN 'Manage library fines'
        ELSE name
    END,
    description = CASE code
        WHEN 'TRANSPORT_READ' THEN 'Allows reading vehicles, routes, pickup points, and transport assignments.'
        WHEN 'TRANSPORT_MANAGE' THEN 'Allows managing vehicles, routes, pickup points, and transport assignments.'
        WHEN 'LIBRARY_READ' THEN 'Allows reading library catalog, copies, memberships, loans, fines, and library reports.'
        WHEN 'LIBRARY_CREATE' THEN 'Allows creating library masters, books, copies, and memberships.'
        WHEN 'LIBRARY_UPDATE' THEN 'Allows updating library masters, books, copies, memberships, and copy statuses.'
        WHEN 'LIBRARY_DELETE' THEN 'Allows deactivating library books and memberships.'
        WHEN 'LIBRARY_ISSUE' THEN 'Allows issuing library book copies to members.'
        WHEN 'LIBRARY_RETURN' THEN 'Allows returning or marking issued library books lost.'
        WHEN 'LIBRARY_FINE' THEN 'Allows paying and waiving library fines.'
        ELSE description
    END,
    module_name = CASE
        WHEN code IN ('TRANSPORT_READ', 'TRANSPORT_MANAGE') THEN 'TRANSPORT'
        WHEN code IN (
            'LIBRARY_READ', 'LIBRARY_CREATE', 'LIBRARY_UPDATE', 'LIBRARY_DELETE',
            'LIBRARY_ISSUE', 'LIBRARY_RETURN', 'LIBRARY_FINE'
        ) THEN 'LIBRARY'
        ELSE module_name
    END,
    status = 'ACTIVE',
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE code IN (
    'TRANSPORT_READ', 'TRANSPORT_MANAGE',
    'LIBRARY_READ', 'LIBRARY_CREATE', 'LIBRARY_UPDATE', 'LIBRARY_DELETE',
    'LIBRARY_ISSUE', 'LIBRARY_RETURN', 'LIBRARY_FINE'
)
  AND deleted = FALSE;

WITH role_permission_seed (role_name, permission_code) AS (
    VALUES
        ('SUPER_ADMIN', 'TRANSPORT_READ'),
        ('SUPER_ADMIN', 'TRANSPORT_MANAGE'),
        ('SUPER_ADMIN', 'LIBRARY_READ'),
        ('SUPER_ADMIN', 'LIBRARY_CREATE'),
        ('SUPER_ADMIN', 'LIBRARY_UPDATE'),
        ('SUPER_ADMIN', 'LIBRARY_DELETE'),
        ('SUPER_ADMIN', 'LIBRARY_ISSUE'),
        ('SUPER_ADMIN', 'LIBRARY_RETURN'),
        ('SUPER_ADMIN', 'LIBRARY_FINE'),
        ('ADMIN', 'TRANSPORT_READ'),
        ('ADMIN', 'TRANSPORT_MANAGE'),
        ('ADMIN', 'LIBRARY_READ'),
        ('ADMIN', 'LIBRARY_CREATE'),
        ('ADMIN', 'LIBRARY_UPDATE'),
        ('ADMIN', 'LIBRARY_DELETE'),
        ('ADMIN', 'LIBRARY_ISSUE'),
        ('ADMIN', 'LIBRARY_RETURN'),
        ('ADMIN', 'LIBRARY_FINE'),
        ('PRINCIPAL', 'TRANSPORT_READ'),
        ('PRINCIPAL', 'TRANSPORT_MANAGE'),
        ('PRINCIPAL', 'LIBRARY_READ'),
        ('PRINCIPAL', 'LIBRARY_CREATE'),
        ('PRINCIPAL', 'LIBRARY_UPDATE'),
        ('PRINCIPAL', 'LIBRARY_DELETE'),
        ('PRINCIPAL', 'LIBRARY_ISSUE'),
        ('PRINCIPAL', 'LIBRARY_RETURN'),
        ('PRINCIPAL', 'LIBRARY_FINE'),
        ('RECEPTIONIST', 'TRANSPORT_READ'),
        ('RECEPTIONIST', 'TRANSPORT_MANAGE'),
        ('RECEPTIONIST', 'LIBRARY_READ'),
        ('RECEPTIONIST', 'LIBRARY_CREATE'),
        ('RECEPTIONIST', 'LIBRARY_UPDATE'),
        ('RECEPTIONIST', 'LIBRARY_ISSUE'),
        ('RECEPTIONIST', 'LIBRARY_RETURN'),
        ('RECEPTIONIST', 'LIBRARY_FINE'),
        ('TEACHER', 'LIBRARY_READ'),
        ('STUDENT', 'LIBRARY_READ'),
        ('PARENT', 'LIBRARY_READ'),
        ('WARDEN', 'LIBRARY_READ')
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

UPDATE menu_items
SET title = CASE module_id
        WHEN 'transport' THEN 'Transport Management'
        WHEN 'library' THEN 'Library Management'
        ELSE title
    END,
    route_path = CASE module_id
        WHEN 'transport' THEN '/transport'
        WHEN 'library' THEN '/modules/library'
        ELSE route_path
    END,
    required_permission = CASE module_id
        WHEN 'transport' THEN 'TRANSPORT_READ'
        WHEN 'library' THEN 'LIBRARY_READ'
        ELSE required_permission
    END,
    icon_key = CASE module_id
        WHEN 'transport' THEN 'transport'
        WHEN 'library' THEN 'local_library'
        ELSE icon_key
    END,
    display_order = CASE module_id
        WHEN 'transport' THEN 65
        WHEN 'library' THEN 118
        ELSE display_order
    END,
    active = TRUE,
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE module_id IN ('transport', 'library')
  AND deleted = FALSE;

WITH menu_seed (module_id, title, route_path, required_permission, icon_key, display_order) AS (
    VALUES
        ('transport', 'Transport Management', '/transport', 'TRANSPORT_READ', 'transport', 65),
        ('library', 'Library Management', '/modules/library', 'LIBRARY_READ', 'local_library', 118)
)
INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, active, created_by, updated_by)
SELECT seed.module_id, seed.title, seed.route_path, seed.required_permission, seed.icon_key, seed.display_order, TRUE, 'flyway', 'flyway'
FROM menu_seed seed
WHERE NOT EXISTS (
    SELECT 1
    FROM menu_items existing
    WHERE existing.module_id = seed.module_id
      AND existing.deleted = FALSE
);

WITH role_menu_seed (role_name, module_id) AS (
    VALUES
        ('SUPER_ADMIN', 'transport'),
        ('SUPER_ADMIN', 'library'),
        ('ADMIN', 'transport'),
        ('ADMIN', 'library'),
        ('PRINCIPAL', 'transport'),
        ('PRINCIPAL', 'library'),
        ('RECEPTIONIST', 'transport'),
        ('RECEPTIONIST', 'library'),
        ('TEACHER', 'library'),
        ('STUDENT', 'library'),
        ('PARENT', 'library'),
        ('WARDEN', 'library')
)
INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM role_menu_seed seed
JOIN roles ON roles.name = seed.role_name
JOIN menu_items ON menu_items.module_id = seed.module_id
WHERE roles.deleted = FALSE
  AND menu_items.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
