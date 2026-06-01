ALTER TABLE user_accounts
    ADD COLUMN IF NOT EXISTS middle_name VARCHAR(80);

ALTER TABLE user_accounts
    ADD COLUMN IF NOT EXISTS source VARCHAR(40) NOT NULL DEFAULT 'ADMIN_CREATED';

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_accounts_phone_active
    ON user_accounts (phone_number)
    WHERE deleted = FALSE AND phone_number IS NOT NULL;

CREATE TABLE IF NOT EXISTS student_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    class_id UUID NOT NULL REFERENCES classes (id),
    section_id UUID NOT NULL REFERENCES sections (id),
    student_id UUID NOT NULL REFERENCES students (id),
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
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_student_attendance_daily_active
    ON student_attendance (academic_year_id, class_id, section_id, student_id, attendance_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_student_attendance_hierarchy_date
    ON student_attendance (academic_year_id, class_id, section_id, attendance_date)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS exam_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_types_code_active
    ON exam_types (LOWER(code))
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS exam_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    class_id UUID NOT NULL REFERENCES classes (id),
    section_id UUID NOT NULL REFERENCES sections (id),
    exam_type_id UUID NOT NULL REFERENCES exam_types (id),
    subject_id UUID NOT NULL REFERENCES subjects (id),
    exam_date DATE NOT NULL,
    max_marks NUMERIC(10, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_schedules_subject_active
    ON exam_schedules (academic_year_id, class_id, section_id, exam_type_id, subject_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_exam_schedules_hierarchy
    ON exam_schedules (academic_year_id, class_id, section_id)
    WHERE deleted = FALSE;

CREATE TABLE IF NOT EXISTS exam_marks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    class_id UUID NOT NULL REFERENCES classes (id),
    section_id UUID NOT NULL REFERENCES sections (id),
    exam_schedule_id UUID NOT NULL REFERENCES exam_schedules (id),
    subject_id UUID NOT NULL REFERENCES subjects (id),
    student_id UUID NOT NULL REFERENCES students (id),
    marks_obtained NUMERIC(10, 2) NOT NULL,
    max_marks NUMERIC(10, 2) NOT NULL,
    remarks VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_marks_student_schedule_subject_active
    ON exam_marks (student_id, exam_schedule_id, subject_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_exam_marks_result_lookup
    ON exam_marks (academic_year_id, class_id, section_id, exam_schedule_id, subject_id)
    WHERE deleted = FALSE;
