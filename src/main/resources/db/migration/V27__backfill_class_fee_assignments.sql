INSERT INTO class_fee_assignments (
    academic_year_id,
    class_id,
    fee_structure_id,
    assigned_date,
    status,
    assigned_by,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    structure.academic_year_id,
    structure.class_id,
    structure.id,
    COALESCE(structure.created_at::date, CURRENT_DATE),
    'ACTIVE',
    COALESCE(NULLIF(structure.created_by, ''), 'flyway'),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'flyway',
    'flyway'
FROM fee_structures structure
WHERE structure.deleted = FALSE
  AND structure.status = 'ACTIVE'
  AND structure.fee_scope = 'CLASS'
  AND structure.academic_year_id IS NOT NULL
  AND structure.class_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM class_fee_assignments existing
      WHERE existing.deleted = FALSE
        AND existing.status = 'ACTIVE'
        AND existing.academic_year_id = structure.academic_year_id
        AND existing.class_id = structure.class_id
        AND existing.fee_structure_id = structure.id
  );
