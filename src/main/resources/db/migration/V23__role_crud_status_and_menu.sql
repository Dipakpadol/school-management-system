ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

UPDATE roles
SET status = 'ACTIVE'
WHERE status IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_roles_status') THEN
        ALTER TABLE roles
            ADD CONSTRAINT chk_roles_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
    END IF;
END $$;

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('roles', 'Role & Permission', '/roles', 'USERS_READ', 'roles', 35, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id = 'roles'
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
