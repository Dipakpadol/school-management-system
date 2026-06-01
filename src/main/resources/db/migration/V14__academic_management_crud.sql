ALTER TABLE academic_years
    ADD COLUMN IF NOT EXISTS description VARCHAR(500);

CREATE TABLE IF NOT EXISTS division_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    section_id UUID NOT NULL REFERENCES sections (id),
    subject_id UUID NOT NULL REFERENCES subjects (id),
    teacher_id UUID REFERENCES teachers (id),
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

CREATE UNIQUE INDEX IF NOT EXISTS ux_division_subjects_active
    ON division_subjects (section_id, subject_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_division_subjects_section_active
    ON division_subjects (section_id, active)
    WHERE deleted = FALSE;

INSERT INTO division_subjects (section_id, subject_id, teacher_id, active, created_by, updated_by)
SELECT DISTINCT mapping.section_id, mapping.subject_id, mapping.teacher_id, mapping.active, 'flyway', 'flyway'
FROM subject_teacher_mapping mapping
WHERE mapping.deleted = FALSE
  AND NOT EXISTS (
      SELECT 1
      FROM division_subjects existing
      WHERE existing.deleted = FALSE
        AND existing.section_id = mapping.section_id
        AND existing.subject_id = mapping.subject_id
  );
