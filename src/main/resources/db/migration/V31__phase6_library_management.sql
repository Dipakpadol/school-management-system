CREATE TABLE IF NOT EXISTS library_book_categories (
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_book_categories_name_active
    ON library_book_categories (LOWER(name))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_authors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    biography VARCHAR(1000),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_authors_name_active
    ON library_authors (LOWER(name))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_publishers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    contact_info VARCHAR(500),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_publishers_name_active
    ON library_publishers (LOWER(name))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_books (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(240) NOT NULL,
    isbn VARCHAR(40),
    category_id UUID REFERENCES library_book_categories (id),
    publisher_id UUID REFERENCES library_publishers (id),
    edition VARCHAR(80),
    publication_year INTEGER,
    language VARCHAR(80),
    description VARCHAR(1000),
    shelf_location VARCHAR(80),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_library_books_publication_year CHECK (publication_year IS NULL OR publication_year BETWEEN 1000 AND 2200)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_books_isbn_active
    ON library_books (LOWER(isbn))
    WHERE deleted = FALSE
      AND isbn IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_library_books_category
    ON library_books (category_id)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_book_authors (
    book_id UUID NOT NULL REFERENCES library_books (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES library_authors (id),
    PRIMARY KEY (book_id, author_id)
);

CREATE INDEX IF NOT EXISTS idx_library_book_authors_author
    ON library_book_authors (author_id);

CREATE TABLE IF NOT EXISTS library_book_copies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id UUID NOT NULL REFERENCES library_books (id),
    accession_number VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    shelf_location VARCHAR(80),
    acquired_on DATE,
    price NUMERIC(12, 2),
    condition_note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_library_copy_status CHECK (status IN ('AVAILABLE', 'ISSUED', 'LOST', 'DAMAGED', 'WITHDRAWN')),
    CONSTRAINT chk_library_copy_price CHECK (price IS NULL OR price >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_book_copies_accession_active
    ON library_book_copies (LOWER(accession_number))
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_library_book_copies_book_status
    ON library_book_copies (book_id, status)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_type VARCHAR(30) NOT NULL,
    student_id UUID REFERENCES students (id),
    teacher_id UUID REFERENCES teachers (id),
    staff_id UUID REFERENCES staff (id),
    membership_number VARCHAR(60) NOT NULL,
    start_date DATE NOT NULL,
    expiry_date DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_library_member_type CHECK (member_type IN ('STUDENT', 'TEACHER', 'STAFF')),
    CONSTRAINT chk_library_membership_identity CHECK (
        (member_type = 'STUDENT' AND student_id IS NOT NULL AND teacher_id IS NULL AND staff_id IS NULL)
        OR (member_type = 'TEACHER' AND teacher_id IS NOT NULL AND student_id IS NULL AND staff_id IS NULL)
        OR (member_type = 'STAFF' AND staff_id IS NOT NULL AND student_id IS NULL AND teacher_id IS NULL)
    ),
    CONSTRAINT chk_library_membership_dates CHECK (expiry_date IS NULL OR expiry_date >= start_date)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_memberships_number_active
    ON library_memberships (LOWER(membership_number))
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_memberships_student_active
    ON library_memberships (student_id)
    WHERE deleted = FALSE
      AND active = TRUE
      AND student_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_memberships_teacher_active
    ON library_memberships (teacher_id)
    WHERE deleted = FALSE
      AND active = TRUE
      AND teacher_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_memberships_staff_active
    ON library_memberships (staff_id)
    WHERE deleted = FALSE
      AND active = TRUE
      AND staff_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS library_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    copy_id UUID NOT NULL REFERENCES library_book_copies (id),
    membership_id UUID NOT NULL REFERENCES library_memberships (id),
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    returned_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    issued_by VARCHAR(100),
    returned_by VARCHAR(100),
    remarks VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_library_loan_status CHECK (status IN ('ACTIVE', 'RETURNED', 'LOST')),
    CONSTRAINT chk_library_loan_dates CHECK (due_date >= issue_date AND (return_date IS NULL OR return_date >= issue_date))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_loans_copy_active
    ON library_loans (copy_id)
    WHERE deleted = FALSE
      AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_library_loans_membership_status
    ON library_loans (membership_id, status, issue_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_library_loans_overdue
    ON library_loans (status, due_date)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS library_fines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES library_loans (id),
    amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    reason VARCHAR(500),
    fine_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_at TIMESTAMP WITH TIME ZONE,
    collected_by VARCHAR(100),
    waived_at TIMESTAMP WITH TIME ZONE,
    waived_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_library_fine_status CHECK (status IN ('PENDING', 'PAID', 'WAIVED')),
    CONSTRAINT chk_library_fine_amounts CHECK (amount >= 0 AND paid_amount >= 0 AND paid_amount <= amount)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_library_fines_loan_active
    ON library_fines (loan_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_library_fines_status_date
    ON library_fines (status, fine_date)
    WHERE deleted = FALSE;

WITH setting_seed (group_name, setting_key, setting_value) AS (
    VALUES
        ('library', 'defaultLoanDays', '14'),
        ('library', 'maxActiveLoans', '3'),
        ('library', 'finePerDay', '0.00')
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

INSERT INTO role_permissions (role_id, permission_id)
SELECT roles.id, permissions.id
FROM roles
JOIN permissions ON permissions.code IN (
    'LIBRARY_READ', 'LIBRARY_CREATE', 'LIBRARY_UPDATE', 'LIBRARY_DELETE',
    'LIBRARY_ISSUE', 'LIBRARY_RETURN', 'LIBRARY_FINE'
)
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL')
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
    'LIBRARY_READ', 'LIBRARY_CREATE', 'LIBRARY_UPDATE', 'LIBRARY_ISSUE', 'LIBRARY_RETURN', 'LIBRARY_FINE'
)
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
JOIN permissions ON permissions.code = 'LIBRARY_READ'
WHERE roles.name IN ('TEACHER', 'STUDENT', 'PARENT', 'WARDEN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_permissions existing
      WHERE existing.role_id = roles.id
        AND existing.permission_id = permissions.id
  );

INSERT INTO menu_items
    (module_id, title, route_path, required_permission, icon_key, display_order, created_by, updated_by)
VALUES
    ('library', 'Library Management', '/modules/library', 'LIBRARY_READ', 'local_library', 118, 'flyway', 'flyway')
ON CONFLICT DO NOTHING;

INSERT INTO role_menu_items (role_id, menu_item_id)
SELECT roles.id, menu_items.id
FROM roles
JOIN menu_items ON menu_items.module_id = 'library'
WHERE roles.name IN ('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'RECEPTIONIST', 'TEACHER', 'STUDENT', 'PARENT', 'WARDEN')
  AND NOT EXISTS (
      SELECT 1
      FROM role_menu_items existing
      WHERE existing.role_id = roles.id
        AND existing.menu_item_id = menu_items.id
  );
