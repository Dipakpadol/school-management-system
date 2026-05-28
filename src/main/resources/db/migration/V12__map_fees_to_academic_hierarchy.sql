ALTER TABLE fee_structures
    ADD COLUMN IF NOT EXISTS academic_year_id UUID REFERENCES academic_years (id),
    ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES classes (id);

ALTER TABLE student_fee_assignments
    ADD COLUMN IF NOT EXISTS academic_year_id UUID REFERENCES academic_years (id),
    ADD COLUMN IF NOT EXISTS class_id UUID REFERENCES classes (id);

CREATE INDEX IF NOT EXISTS idx_fee_structures_hierarchy_active
    ON fee_structures (academic_year_id, class_id, status)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_student_fee_assignments_hierarchy_active
    ON student_fee_assignments (academic_year_id, class_id, status)
    WHERE deleted = FALSE;

UPDATE fee_structures structure
SET academic_year_id = academic_years.id,
    class_id = classes.id
FROM academic_years
JOIN classes ON classes.academic_year_id = academic_years.id
WHERE structure.deleted = FALSE
  AND structure.academic_year_id IS NULL
  AND LOWER(academic_years.name) = LOWER(structure.academic_year)
  AND LOWER(classes.name) = LOWER(structure.class_name);

UPDATE student_fee_assignments assignment
SET academic_year_id = fee_structures.academic_year_id,
    class_id = fee_structures.class_id
FROM fee_structures
WHERE assignment.deleted = FALSE
  AND assignment.fee_structure_id = fee_structures.id
  AND assignment.academic_year_id IS NULL;
