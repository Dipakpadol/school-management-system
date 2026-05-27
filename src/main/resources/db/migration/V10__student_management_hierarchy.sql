DO $$
BEGIN
    IF to_regclass('public.parents') IS NULL AND to_regclass('public.parent_guardians') IS NOT NULL THEN
        ALTER TABLE parent_guardians RENAME TO parents;
    END IF;

    IF to_regclass('public.student_parent_mapping') IS NULL AND to_regclass('public.student_parents') IS NOT NULL THEN
        ALTER TABLE student_parents RENAME TO student_parent_mapping;
    END IF;
END $$;

CREATE TABLE academic_years (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_academic_year_dates CHECK (end_date >= start_date)
);

CREATE UNIQUE INDEX ux_academic_years_code_active
    ON academic_years (LOWER(code))
    WHERE deleted = FALSE;
CREATE INDEX idx_academic_years_active_dates
    ON academic_years (active, start_date DESC)
    WHERE deleted = FALSE;

CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
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

CREATE UNIQUE INDEX ux_classes_year_code_active
    ON classes (academic_year_id, LOWER(code))
    WHERE deleted = FALSE;
CREATE INDEX idx_classes_year_active
    ON classes (academic_year_id, display_order, name)
    WHERE deleted = FALSE;

CREATE TABLE sections (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes (id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    capacity INTEGER,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_sections_capacity CHECK (capacity IS NULL OR capacity > 0)
);

CREATE UNIQUE INDEX ux_sections_class_code_active
    ON sections (class_id, LOWER(code))
    WHERE deleted = FALSE;
CREATE INDEX idx_sections_class_active
    ON sections (class_id, display_order, name)
    WHERE deleted = FALSE;

CREATE TABLE teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number VARCHAR(40) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    email VARCHAR(160),
    phone_number VARCHAR(30),
    user_account_id UUID,
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

CREATE UNIQUE INDEX ux_teachers_employee_active
    ON teachers (LOWER(employee_number))
    WHERE deleted = FALSE;
CREATE UNIQUE INDEX ux_teachers_email_active
    ON teachers (LOWER(email))
    WHERE deleted = FALSE AND email IS NOT NULL;

CREATE TABLE subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL,
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

CREATE UNIQUE INDEX ux_subjects_code_active
    ON subjects (LOWER(code))
    WHERE deleted = FALSE;

CREATE TABLE class_teacher_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes (id),
    section_id UUID NOT NULL REFERENCES sections (id),
    teacher_id UUID NOT NULL REFERENCES teachers (id),
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
    CONSTRAINT chk_class_teacher_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX ux_class_teacher_section_active
    ON class_teacher_mapping (class_id, section_id)
    WHERE deleted = FALSE AND active = TRUE;

CREATE TABLE subject_teacher_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes (id),
    section_id UUID NOT NULL REFERENCES sections (id),
    subject_id UUID NOT NULL REFERENCES subjects (id),
    teacher_id UUID NOT NULL REFERENCES teachers (id),
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
    CONSTRAINT chk_subject_teacher_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX ux_subject_teacher_active
    ON subject_teacher_mapping (class_id, section_id, subject_id, teacher_id)
    WHERE deleted = FALSE AND active = TRUE;

ALTER TABLE student_class_assignments
    ADD COLUMN IF NOT EXISTS academic_year_id UUID REFERENCES academic_years (id),
    ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES classes (id),
    ADD COLUMN IF NOT EXISTS section_id UUID REFERENCES sections (id);

DROP INDEX IF EXISTS ux_student_current_assignment_active;

CREATE UNIQUE INDEX ux_student_assignment_year_hierarchy_active
    ON student_class_assignments (student_id, academic_year_id)
    WHERE deleted = FALSE AND active = TRUE AND academic_year_id IS NOT NULL;

CREATE UNIQUE INDEX ux_student_assignment_year_name_active
    ON student_class_assignments (student_id, LOWER(academic_year))
    WHERE deleted = FALSE AND active = TRUE AND academic_year_id IS NULL;

CREATE UNIQUE INDEX ux_student_class_roll_hierarchy_active
    ON student_class_assignments (academic_year_id, class_id, section_id, LOWER(roll_number))
    WHERE deleted = FALSE
      AND active = TRUE
      AND academic_year_id IS NOT NULL
      AND class_id IS NOT NULL
      AND section_id IS NOT NULL
      AND roll_number IS NOT NULL;

INSERT INTO academic_years (code, name, start_date, end_date, active, created_by, updated_by)
SELECT 'AY-2026-27', '2026-2027', DATE '2026-04-01', DATE '2027-03-31', TRUE, 'flyway', 'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM academic_years WHERE deleted = FALSE AND LOWER(code) = LOWER('AY-2026-27')
);

WITH default_year AS (
    SELECT id FROM academic_years WHERE deleted = FALSE AND LOWER(code) = LOWER('AY-2026-27')
)
INSERT INTO classes (academic_year_id, code, name, display_order, active, created_by, updated_by)
SELECT default_year.id, 'CLASS-' || grade.grade_no, 'Class ' || grade.grade_no, grade.grade_no, TRUE, 'flyway', 'flyway'
FROM default_year
CROSS JOIN generate_series(1, 10) AS grade(grade_no)
WHERE NOT EXISTS (
    SELECT 1
    FROM classes existing
    WHERE existing.deleted = FALSE
      AND existing.academic_year_id = default_year.id
      AND LOWER(existing.code) = LOWER('CLASS-' || grade.grade_no)
);

WITH class_rows AS (
    SELECT id, display_order
    FROM classes
    WHERE deleted = FALSE
      AND academic_year_id = (SELECT id FROM academic_years WHERE deleted = FALSE AND LOWER(code) = LOWER('AY-2026-27'))
)
INSERT INTO sections (class_id, code, name, capacity, display_order, active, created_by, updated_by)
SELECT class_rows.id, section_seed.code, section_seed.name, 40, section_seed.display_order, TRUE, 'flyway', 'flyway'
FROM class_rows
CROSS JOIN (VALUES ('A', 'Division A', 1), ('B', 'Division B', 2), ('C', 'Division C', 3)) AS section_seed(code, name, display_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM sections existing
    WHERE existing.deleted = FALSE
      AND existing.class_id = class_rows.id
      AND LOWER(existing.code) = LOWER(section_seed.code)
);

INSERT INTO subjects (code, name, description, active, created_by, updated_by)
SELECT subject_seed.code, subject_seed.name, subject_seed.description, TRUE, 'flyway', 'flyway'
FROM (VALUES
    ('ENG', 'English', 'Language and literature'),
    ('MATH', 'Mathematics', 'Core mathematics'),
    ('SCI', 'Science', 'General science'),
    ('SST', 'Social Studies', 'History, civics, and geography'),
    ('HIN', 'Hindi', 'Second language')
) AS subject_seed(code, name, description)
WHERE NOT EXISTS (
    SELECT 1 FROM subjects existing WHERE existing.deleted = FALSE AND LOWER(existing.code) = LOWER(subject_seed.code)
);

INSERT INTO teachers (employee_number, first_name, last_name, email, phone_number, active, created_by, updated_by)
SELECT teacher_seed.employee_number, teacher_seed.first_name, teacher_seed.last_name, teacher_seed.email, teacher_seed.phone_number, TRUE, 'flyway', 'flyway'
FROM (VALUES
    ('TCH-001', 'Amit', 'Sharma', 'amit.sharma@school.test', '+919810100001'),
    ('TCH-002', 'Meera', 'Iyer', 'meera.iyer@school.test', '+919810100002'),
    ('TCH-003', 'Arjun', 'Menon', 'arjun.menon@school.test', '+919810100003')
) AS teacher_seed(employee_number, first_name, last_name, email, phone_number)
WHERE NOT EXISTS (
    SELECT 1 FROM teachers existing WHERE existing.deleted = FALSE AND LOWER(existing.employee_number) = LOWER(teacher_seed.employee_number)
);

UPDATE student_class_assignments assignment
SET academic_year_id = academic_years.id,
    class_id = classes.id,
    section_id = sections.id
FROM academic_years
JOIN classes ON classes.academic_year_id = academic_years.id
JOIN sections ON sections.class_id = classes.id
WHERE assignment.deleted = FALSE
  AND assignment.academic_year_id IS NULL
  AND LOWER(academic_years.name) = LOWER(assignment.academic_year)
  AND LOWER(classes.name) = LOWER(assignment.class_name)
  AND (
      LOWER(sections.code) = LOWER(assignment.section_name)
      OR LOWER(sections.name) = LOWER(assignment.section_name)
      OR LOWER(sections.name) = LOWER('Division ' || assignment.section_name)
  );

WITH section_a AS (
    SELECT classes.id AS class_id, sections.id AS section_id
    FROM classes
    JOIN academic_years ON academic_years.id = classes.academic_year_id
    JOIN sections ON sections.class_id = classes.id
    WHERE academic_years.deleted = FALSE
      AND classes.deleted = FALSE
      AND sections.deleted = FALSE
      AND LOWER(academic_years.code) = LOWER('AY-2026-27')
      AND LOWER(classes.code) = LOWER('CLASS-6')
      AND LOWER(sections.code) = LOWER('A')
),
class_teacher AS (
    SELECT id AS teacher_id FROM teachers WHERE deleted = FALSE AND LOWER(employee_number) = LOWER('TCH-001')
)
INSERT INTO class_teacher_mapping (class_id, section_id, teacher_id, effective_from, active, created_by, updated_by)
SELECT section_a.class_id, section_a.section_id, class_teacher.teacher_id, DATE '2026-04-01', TRUE, 'flyway', 'flyway'
FROM section_a
CROSS JOIN class_teacher
WHERE NOT EXISTS (
    SELECT 1
    FROM class_teacher_mapping existing
    WHERE existing.deleted = FALSE
      AND existing.active = TRUE
      AND existing.class_id = section_a.class_id
      AND existing.section_id = section_a.section_id
);

WITH section_a AS (
    SELECT classes.id AS class_id, sections.id AS section_id
    FROM classes
    JOIN academic_years ON academic_years.id = classes.academic_year_id
    JOIN sections ON sections.class_id = classes.id
    WHERE academic_years.deleted = FALSE
      AND classes.deleted = FALSE
      AND sections.deleted = FALSE
      AND LOWER(academic_years.code) = LOWER('AY-2026-27')
      AND LOWER(classes.code) = LOWER('CLASS-6')
      AND LOWER(sections.code) = LOWER('A')
),
teacher_rows AS (
    SELECT employee_number, id AS teacher_id FROM teachers WHERE deleted = FALSE
),
subject_rows AS (
    SELECT code, id AS subject_id FROM subjects WHERE deleted = FALSE
),
subject_teacher_seed AS (
    SELECT 'MATH' AS subject_code, 'TCH-001' AS employee_number
    UNION ALL SELECT 'ENG', 'TCH-002'
    UNION ALL SELECT 'SCI', 'TCH-003'
)
INSERT INTO subject_teacher_mapping (class_id, section_id, subject_id, teacher_id, effective_from, active, created_by, updated_by)
SELECT section_a.class_id, section_a.section_id, subject_rows.subject_id, teacher_rows.teacher_id, DATE '2026-04-01', TRUE, 'flyway', 'flyway'
FROM subject_teacher_seed
JOIN subject_rows ON LOWER(subject_rows.code) = LOWER(subject_teacher_seed.subject_code)
JOIN teacher_rows ON LOWER(teacher_rows.employee_number) = LOWER(subject_teacher_seed.employee_number)
CROSS JOIN section_a
WHERE NOT EXISTS (
    SELECT 1
    FROM subject_teacher_mapping existing
    WHERE existing.deleted = FALSE
      AND existing.active = TRUE
      AND existing.class_id = section_a.class_id
      AND existing.section_id = section_a.section_id
      AND existing.subject_id = subject_rows.subject_id
      AND existing.teacher_id = teacher_rows.teacher_id
);
