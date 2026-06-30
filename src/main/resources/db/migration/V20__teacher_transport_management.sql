ALTER TABLE teachers
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(80),
    ADD COLUMN IF NOT EXISTS gender VARCHAR(20),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS qualification VARCHAR(160),
    ADD COLUMN IF NOT EXISTS experience_years INTEGER,
    ADD COLUMN IF NOT EXISTS joining_date DATE,
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

UPDATE teachers
SET status = CASE WHEN active = TRUE THEN 'ACTIVE' ELSE 'INACTIVE' END
WHERE status IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_teachers_status') THEN
        ALTER TABLE teachers
            ADD CONSTRAINT chk_teachers_status CHECK (status IN ('ACTIVE', 'INACTIVE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_teachers_experience') THEN
        ALTER TABLE teachers
            ADD CONSTRAINT chk_teachers_experience CHECK (experience_years IS NULL OR experience_years >= 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS teacher_academic_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES teachers (id),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    assignment_type VARCHAR(40) NOT NULL,
    class_id UUID REFERENCES classes (id),
    section_id UUID REFERENCES sections (id),
    subject_id UUID REFERENCES subjects (id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_teacher_assignment_type CHECK (assignment_type IN ('CLASS_TEACHER', 'SUBJECT_TEACHER', 'COORDINATOR')),
    CONSTRAINT chk_teacher_assignment_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_teacher_assignments_teacher_year
    ON teacher_academic_assignments (teacher_id, academic_year_id, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_teacher_assignments_year
    ON teacher_academic_assignments (academic_year_id, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS teacher_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    teacher_id UUID NOT NULL REFERENCES teachers (id),
    document_type VARCHAR(80) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_path VARCHAR(500),
    file_url VARCHAR(500),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_teacher_documents_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX IF NOT EXISTS idx_teacher_documents_teacher
    ON teacher_documents (teacher_id, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS transport_drivers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80),
    mobile_number VARCHAR(30) NOT NULL,
    license_number VARCHAR(80) NOT NULL,
    license_expiry_date DATE NOT NULL,
    address VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transport_drivers_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transport_drivers_license_active
    ON transport_drivers (LOWER(license_number))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS transport_vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    vehicle_number VARCHAR(60) NOT NULL,
    vehicle_name VARCHAR(160) NOT NULL,
    vehicle_type VARCHAR(80) NOT NULL,
    capacity INTEGER NOT NULL,
    driver_id UUID REFERENCES transport_drivers (id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transport_vehicle_capacity CHECK (capacity > 0),
    CONSTRAINT chk_transport_vehicle_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transport_vehicles_year_number_active
    ON transport_vehicles (academic_year_id, LOWER(vehicle_number))
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transport_vehicles_year_status
    ON transport_vehicles (academic_year_id, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS transport_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    route_name VARCHAR(160) NOT NULL,
    route_code VARCHAR(60) NOT NULL,
    start_location VARCHAR(160) NOT NULL,
    end_location VARCHAR(160) NOT NULL,
    vehicle_id UUID REFERENCES transport_vehicles (id),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transport_route_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transport_routes_year_code_active
    ON transport_routes (academic_year_id, LOWER(route_code))
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transport_routes_year_status
    ON transport_routes (academic_year_id, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transport_routes_vehicle
    ON transport_routes (vehicle_id)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS transport_pickup_points (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES transport_routes (id),
    point_name VARCHAR(160) NOT NULL,
    pickup_time TIME,
    drop_time TIME,
    monthly_fee NUMERIC(12, 2),
    sequence_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transport_pickup_fee CHECK (monthly_fee IS NULL OR monthly_fee >= 0),
    CONSTRAINT chk_transport_pickup_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transport_pickup_route_name_active
    ON transport_pickup_points (route_id, LOWER(point_name))
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_transport_pickup_route_order
    ON transport_pickup_points (route_id, sequence_order)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS student_transport_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    vehicle_id UUID NOT NULL REFERENCES transport_vehicles (id),
    route_id UUID NOT NULL REFERENCES transport_routes (id),
    pickup_point_id UUID NOT NULL REFERENCES transport_pickup_points (id),
    assignment_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
    fee_assigned_status VARCHAR(30) NOT NULL DEFAULT 'NOT_ASSIGNED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_transport_status CHECK (status IN ('ASSIGNED', 'REMOVED', 'TRANSFERRED')),
    CONSTRAINT chk_student_transport_fee_status CHECK (fee_assigned_status IN ('ASSIGNED', 'NOT_ASSIGNED')),
    CONSTRAINT chk_student_transport_dates CHECK (end_date IS NULL OR end_date >= assignment_date)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_student_transport_student_year_active
    ON student_transport_assignments (student_id, academic_year_id)
    WHERE deleted = FALSE AND status = 'ASSIGNED';
CREATE INDEX IF NOT EXISTS idx_student_transport_vehicle_year_status
    ON student_transport_assignments (vehicle_id, academic_year_id, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_student_transport_route_year_status
    ON student_transport_assignments (route_id, academic_year_id, status)
    WHERE deleted = FALSE;

ALTER TABLE fee_structures
    ADD COLUMN IF NOT EXISTS transport_route_id UUID REFERENCES transport_routes (id),
    ADD COLUMN IF NOT EXISTS transport_pickup_point_id UUID REFERENCES transport_pickup_points (id);

ALTER TABLE student_fee_assignments
    ADD COLUMN IF NOT EXISTS transport_route_id UUID REFERENCES transport_routes (id),
    ADD COLUMN IF NOT EXISTS transport_pickup_point_id UUID REFERENCES transport_pickup_points (id);

ALTER TABLE fee_structures DROP CONSTRAINT IF EXISTS chk_fee_structures_scope;
ALTER TABLE fee_structures
    ADD CONSTRAINT chk_fee_structures_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL', 'TRANSPORT'));

ALTER TABLE student_fee_assignments DROP CONSTRAINT IF EXISTS chk_student_fee_assignments_scope;
ALTER TABLE student_fee_assignments
    ADD CONSTRAINT chk_student_fee_assignments_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL', 'TRANSPORT'));

CREATE INDEX IF NOT EXISTS idx_fee_structures_transport_scope_active
    ON fee_structures (fee_scope, academic_year_id, transport_route_id, transport_pickup_point_id, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_student_fee_assignments_transport_scope_active
    ON student_fee_assignments (fee_scope, academic_year_id, transport_route_id, transport_pickup_point_id, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS transport_fee_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    route_id UUID NOT NULL REFERENCES transport_routes (id),
    pickup_point_id UUID REFERENCES transport_pickup_points (id),
    fee_category_id UUID NOT NULL REFERENCES fee_categories (id),
    backing_fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    installment_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    number_of_installments INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_transport_fee_structures_amount CHECK (amount >= 0),
    CONSTRAINT chk_transport_fee_structures_installments CHECK (number_of_installments > 0),
    CONSTRAINT chk_transport_fee_structures_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_transport_fee_structures_scope_active
    ON transport_fee_structures (
        academic_year_id,
        route_id,
        fee_category_id,
        COALESCE(pickup_point_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE deleted = FALSE;

WITH permission_seed (code, name, description) AS (
    VALUES
        ('TEACHERS_READ', 'Read teachers', 'Allows reading teacher profiles, assignments, and documents.'),
        ('TEACHERS_MANAGE', 'Manage teachers', 'Allows managing teacher profiles, assignments, and documents.'),
        ('TRANSPORT_READ', 'Read transport', 'Allows reading vehicles, routes, pickup points, and transport assignments.'),
        ('TRANSPORT_MANAGE', 'Manage transport', 'Allows managing vehicles, routes, pickup points, and transport assignments.')
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
JOIN permissions ON permissions.code IN ('TEACHERS_READ', 'TEACHERS_MANAGE', 'TRANSPORT_READ', 'TRANSPORT_MANAGE')
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
JOIN permissions ON permissions.code IN ('TEACHERS_READ', 'TEACHERS_MANAGE', 'TRANSPORT_READ', 'TRANSPORT_MANAGE')
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
JOIN permissions ON permissions.code IN ('TRANSPORT_READ', 'TRANSPORT_MANAGE')
WHERE roles.name = 'RECEPTIONIST'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN ('TEACHERS_READ')
WHERE roles.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('teachers', 'Teacher Management', '/teachers', 'TEACHERS_READ', 'teachers', 55, 'flyway', 'flyway'),
    ('transport', 'Transport Management', '/transport', 'TRANSPORT_READ', 'transport', 65, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('teachers', 'transport')
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id = 'transport'
WHERE roles.name = 'RECEPTIONIST'
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
