ALTER TABLE teachers
    ADD COLUMN IF NOT EXISTS staff_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS ux_teachers_staff_active
    ON teachers (staff_id)
    WHERE deleted = FALSE
      AND staff_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS staff_departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_departments_name_active
    ON staff_departments (LOWER(name))
    WHERE deleted = FALSE
      AND active = TRUE;

CREATE TABLE IF NOT EXISTS staff_designations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    department_id UUID REFERENCES staff_departments (id),
    description VARCHAR(500),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_designations_name_active
    ON staff_designations (LOWER(name))
    WHERE deleted = FALSE
      AND active = TRUE;

CREATE INDEX IF NOT EXISTS idx_staff_designations_department
    ON staff_designations (department_id)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(40) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80),
    gender VARCHAR(20),
    date_of_birth DATE,
    email VARCHAR(160),
    phone_number VARCHAR(30),
    user_account_id UUID REFERENCES user_accounts (id),
    teacher_id UUID REFERENCES teachers (id),
    department_id UUID REFERENCES staff_departments (id),
    designation_id UUID REFERENCES staff_designations (id),
    joining_date DATE NOT NULL,
    staff_type VARCHAR(30) NOT NULL DEFAULT 'NON_TEACHING',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    relieving_date DATE,
    exit_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_staff_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_staff_type CHECK (staff_type IN ('TEACHING', 'NON_TEACHING')),
    CONSTRAINT chk_staff_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXITED')),
    CONSTRAINT chk_staff_exit_dates CHECK (relieving_date IS NULL OR relieving_date >= joining_date)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_employee_code_active
    ON staff (LOWER(employee_code))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_user_account_active
    ON staff (user_account_id)
    WHERE deleted = FALSE
      AND user_account_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_teacher_active
    ON staff (teacher_id)
    WHERE deleted = FALSE
      AND teacher_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_staff_roster
    ON staff (status, department_id, designation_id, joining_date, relieving_date)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS staff_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff (id),
    document_type VARCHAR(80) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_url VARCHAR(500),
    file_path VARCHAR(500),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by VARCHAR(120),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_staff_documents_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT chk_staff_documents_location CHECK (file_url IS NOT NULL OR file_path IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_staff_documents_staff
    ON staff_documents (staff_id, uploaded_at)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS staff_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff (id),
    attendance_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    remarks VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_staff_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'LEAVE'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_attendance_staff_date_active
    ON staff_attendance (staff_id, attendance_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_staff_attendance_date
    ON staff_attendance (attendance_date, staff_id)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS staff_leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    paid BOOLEAN NOT NULL DEFAULT FALSE,
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_staff_leave_types_name_active
    ON staff_leave_types (LOWER(name))
    WHERE deleted = FALSE
      AND active = TRUE;

CREATE TABLE IF NOT EXISTS staff_leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff (id),
    leave_type_id UUID NOT NULL REFERENCES staff_leave_types (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_by VARCHAR(120),
    reviewed_by VARCHAR(120),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    review_comment VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_staff_leave_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_staff_leave_date_range CHECK (end_date >= start_date)
);

CREATE INDEX IF NOT EXISTS idx_staff_leave_requests_staff_dates
    ON staff_leave_requests (staff_id, start_date, end_date, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS salary_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(140) NOT NULL,
    basic_salary NUMERIC(12, 2) NOT NULL DEFAULT 0,
    allowances NUMERIC(12, 2) NOT NULL DEFAULT 0,
    deductions NUMERIC(12, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_salary_structure_amounts CHECK (basic_salary >= 0 AND allowances >= 0 AND deductions >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_salary_structures_code_active
    ON salary_structures (LOWER(code))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS staff_salary_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff (id),
    salary_structure_id UUID NOT NULL REFERENCES salary_structures (id),
    effective_from DATE NOT NULL,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_staff_salary_assignment_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE INDEX IF NOT EXISTS idx_staff_salary_assignments_effective
    ON staff_salary_assignments (staff_id, effective_from, effective_to)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS payroll_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_id UUID NOT NULL REFERENCES staff (id),
    payroll_year INTEGER NOT NULL,
    payroll_month INTEGER NOT NULL,
    salary_structure_name VARCHAR(140) NOT NULL,
    basic_salary NUMERIC(12, 2) NOT NULL,
    allowances NUMERIC(12, 2) NOT NULL,
    deductions NUMERIC(12, 2) NOT NULL,
    gross_salary NUMERIC(12, 2) NOT NULL,
    net_salary NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_payroll_period CHECK (payroll_year BETWEEN 2000 AND 2100 AND payroll_month BETWEEN 1 AND 12),
    CONSTRAINT chk_payroll_amounts CHECK (
        basic_salary >= 0
        AND allowances >= 0
        AND deductions >= 0
        AND gross_salary >= 0
        AND net_salary >= 0
    ),
    CONSTRAINT chk_payroll_status CHECK (status IN ('PENDING', 'REVIEWED', 'PAID'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_payroll_records_staff_period_active
    ON payroll_records (staff_id, payroll_year, payroll_month)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_payroll_records_period_status
    ON payroll_records (payroll_year, payroll_month, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS communication_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message TEXT NOT NULL,
    audience_type VARCHAR(30) NOT NULL,
    academic_year_id UUID REFERENCES academic_years (id),
    class_id UUID REFERENCES classes (id),
    section_id UUID REFERENCES sections (id),
    publish_at TIMESTAMP WITH TIME ZONE,
    expiry_at TIMESTAMP WITH TIME ZONE,
    event_start_at TIMESTAMP WITH TIME ZONE,
    event_end_at TIMESTAMP WITH TIME ZONE,
    location VARCHAR(180),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    recipient_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP WITH TIME ZONE,
    published_by VARCHAR(120),
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_communication_type CHECK (type IN ('ANNOUNCEMENT', 'CIRCULAR', 'NOTICE', 'EVENT')),
    CONSTRAINT chk_communication_audience CHECK (audience_type IN ('ALL', 'STAFF', 'TEACHERS', 'PARENTS', 'STUDENTS', 'CLASS', 'DIVISION')),
    CONSTRAINT chk_communication_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_communication_publish_expiry CHECK (expiry_at IS NULL OR publish_at IS NULL OR expiry_at >= publish_at),
    CONSTRAINT chk_communication_event_dates CHECK (event_end_at IS NULL OR event_start_at IS NULL OR event_end_at >= event_start_at)
);

CREATE INDEX IF NOT EXISTS idx_communication_records_status_type
    ON communication_records (status, type, created_at)
    WHERE deleted = FALSE;

WITH permission_seed (code, name, description, module_name) AS (
    VALUES
        ('STAFF_READ', 'Read staff', 'Allows reading staff, departments, designations, documents, and staff reports.', 'STAFF'),
        ('STAFF_CREATE', 'Create staff', 'Allows creating staff, departments, designations, and staff documents.', 'STAFF'),
        ('STAFF_UPDATE', 'Update staff', 'Allows updating staff profiles, lifecycle, departments, designations, and documents.', 'STAFF'),
        ('STAFF_DELETE', 'Delete staff', 'Allows deactivating staff profiles and archiving staff documents.', 'STAFF'),
        ('LEAVE_READ', 'Read leave', 'Allows reading leave types and staff leave requests.', 'LEAVE'),
        ('LEAVE_CREATE', 'Create leave', 'Allows creating leave types and staff leave requests.', 'LEAVE'),
        ('LEAVE_APPROVE', 'Approve leave', 'Allows approving and rejecting staff leave requests.', 'LEAVE'),
        ('PAYROLL_READ', 'Read payroll', 'Allows reading salary structures, salary assignments, and payroll records.', 'PAYROLL'),
        ('PAYROLL_CREATE', 'Create payroll', 'Allows creating salary structures and salary assignments.', 'PAYROLL'),
        ('PAYROLL_UPDATE', 'Update payroll', 'Allows updating salary structures and payroll records.', 'PAYROLL'),
        ('PAYROLL_PROCESS', 'Process payroll', 'Allows generating and marking payroll records paid.', 'PAYROLL'),
        ('COMMUNICATION_READ', 'Read communications', 'Allows reading announcements, circulars, notices, and events.', 'COMMUNICATION'),
        ('COMMUNICATION_CREATE', 'Create communications', 'Allows creating announcements, circulars, notices, and events.', 'COMMUNICATION'),
        ('COMMUNICATION_UPDATE', 'Update communications', 'Allows updating and archiving communication records.', 'COMMUNICATION'),
        ('COMMUNICATION_PUBLISH', 'Publish communications', 'Allows publishing and unpublishing communication records.', 'COMMUNICATION')
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
    'STAFF_READ', 'STAFF_CREATE', 'STAFF_UPDATE', 'STAFF_DELETE',
    'LEAVE_READ', 'LEAVE_CREATE', 'LEAVE_APPROVE',
    'PAYROLL_READ', 'PAYROLL_CREATE', 'PAYROLL_UPDATE', 'PAYROLL_PROCESS',
    'COMMUNICATION_READ', 'COMMUNICATION_CREATE', 'COMMUNICATION_UPDATE', 'COMMUNICATION_PUBLISH'
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
    'STAFF_READ', 'STAFF_UPDATE',
    'LEAVE_READ', 'LEAVE_CREATE', 'LEAVE_APPROVE',
    'PAYROLL_READ',
    'COMMUNICATION_READ', 'COMMUNICATION_CREATE', 'COMMUNICATION_UPDATE', 'COMMUNICATION_PUBLISH'
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
    'LEAVE_READ', 'LEAVE_CREATE',
    'COMMUNICATION_READ'
)
WHERE roles.name = 'TEACHER'
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
    'STAFF_READ',
    'PAYROLL_READ', 'PAYROLL_CREATE', 'PAYROLL_UPDATE', 'PAYROLL_PROCESS',
    'COMMUNICATION_READ'
)
WHERE roles.name = 'ACCOUNTANT'
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
    'STAFF_READ',
    'COMMUNICATION_READ'
)
WHERE roles.name IN ('RECEPTIONIST', 'WARDEN', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('staff', 'Staff Management', '/modules/staff', 'STAFF_READ', 'badge', 115, 'flyway', 'flyway'),
    ('communications', 'Communications', '/modules/communications', 'COMMUNICATION_READ', 'campaign', 120, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id IN ('staff', 'communications')
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'ACCOUNTANT', 'RECEPTIONIST', 'WARDEN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id = 'communications'
WHERE roles.name IN ('TEACHER', 'STUDENT', 'PARENT')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
