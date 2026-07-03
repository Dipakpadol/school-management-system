ALTER TABLE student_fee_assignments
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30) NOT NULL DEFAULT 'CLASS',
    ADD COLUMN IF NOT EXISTS source_reference_id UUID;

UPDATE student_fee_assignments
SET source_type = fee_scope
WHERE source_type IS NULL
   OR source_type <> fee_scope;

UPDATE student_fee_assignments
SET source_reference_id = CASE
    WHEN fee_scope = 'CLASS' THEN class_id
    WHEN fee_scope = 'HOSTEL' THEN COALESCE(hostel_room_id, hostel_id)
    WHEN fee_scope = 'TRANSPORT' THEN COALESCE(transport_pickup_point_id, transport_route_id)
    ELSE fee_structure_id
END
WHERE source_reference_id IS NULL;

ALTER TABLE fee_structures DROP CONSTRAINT IF EXISTS chk_fee_structures_scope;
ALTER TABLE fee_structures
    ADD CONSTRAINT chk_fee_structures_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL', 'TRANSPORT', 'MANUAL'));

ALTER TABLE student_fee_assignments DROP CONSTRAINT IF EXISTS chk_student_fee_assignments_scope;
ALTER TABLE student_fee_assignments
    ADD CONSTRAINT chk_student_fee_assignments_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL', 'TRANSPORT', 'MANUAL'));

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_student_fee_assignments_source_type') THEN
        ALTER TABLE student_fee_assignments
            ADD CONSTRAINT chk_student_fee_assignments_source_type
            CHECK (source_type IN ('CLASS', 'HOSTEL', 'TRANSPORT', 'MANUAL'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_student_fee_assignments_source_active
    ON student_fee_assignments (source_type, source_reference_id, academic_year_id, status)
    WHERE deleted = FALSE;
