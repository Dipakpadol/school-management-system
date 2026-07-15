CREATE TABLE IF NOT EXISTS class_fee_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    class_id UUID NOT NULL REFERENCES classes (id),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    assigned_date DATE NOT NULL DEFAULT CURRENT_DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    assigned_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_class_fee_assignments_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_class_fee_assignments_active
    ON class_fee_assignments (academic_year_id, class_id, fee_structure_id)
    WHERE deleted = FALSE AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_class_fee_assignments_lookup
    ON class_fee_assignments (academic_year_id, class_id, status)
    WHERE deleted = FALSE;

ALTER TABLE student_fee_assignments
    ADD COLUMN IF NOT EXISTS assignment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE student_fee_assignments DROP CONSTRAINT IF EXISTS chk_student_fee_assignments_assignment_status;
ALTER TABLE student_fee_assignments
    ADD CONSTRAINT chk_student_fee_assignments_assignment_status
    CHECK (assignment_status IN ('ACTIVE', 'INACTIVE', 'CANCELLED'));

DROP INDEX IF EXISTS ux_fee_structures_class_active;

CREATE UNIQUE INDEX IF NOT EXISTS ux_fee_structures_class_name_active
    ON fee_structures (
        COALESCE(academic_year_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(class_id, '00000000-0000-0000-0000-000000000000'::uuid),
        LOWER(academic_year),
        LOWER(class_name),
        COALESCE(LOWER(section_name), ''),
        LOWER(name)
    )
    WHERE deleted = FALSE AND status = 'ACTIVE';
