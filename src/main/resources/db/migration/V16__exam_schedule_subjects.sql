ALTER TABLE exam_schedules
    ADD COLUMN IF NOT EXISTS exam_name VARCHAR(160);

UPDATE exam_schedules
SET exam_name = COALESCE(description, 'Exam Schedule')
WHERE exam_name IS NULL;

ALTER TABLE exam_schedules
    ALTER COLUMN subject_id DROP NOT NULL;

ALTER TABLE exam_schedules
    ALTER COLUMN exam_date DROP NOT NULL;

ALTER TABLE exam_schedules
    ALTER COLUMN max_marks DROP NOT NULL;

DROP INDEX IF EXISTS ux_exam_schedules_subject_active;

CREATE TABLE IF NOT EXISTS exam_schedule_subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_schedule_id UUID NOT NULL REFERENCES exam_schedules (id),
    subject_id UUID NOT NULL REFERENCES subjects (id),
    exam_date DATE NOT NULL,
    max_marks NUMERIC(6, 2) NOT NULL,
    passing_marks NUMERIC(6, 2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_exam_schedule_subjects_active
    ON exam_schedule_subjects (exam_schedule_id, subject_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_exam_schedule_subjects_schedule
    ON exam_schedule_subjects (exam_schedule_id)
    WHERE deleted = FALSE;

INSERT INTO exam_schedule_subjects (
    exam_schedule_id,
    subject_id,
    exam_date,
    max_marks,
    passing_marks,
    created_at,
    updated_at,
    created_by,
    updated_by,
    deleted,
    version
)
SELECT
    schedule.id,
    schedule.subject_id,
    schedule.exam_date,
    schedule.max_marks,
    NULL,
    schedule.created_at,
    schedule.updated_at,
    schedule.created_by,
    schedule.updated_by,
    FALSE,
    0
FROM exam_schedules schedule
WHERE schedule.subject_id IS NOT NULL
  AND schedule.exam_date IS NOT NULL
  AND schedule.max_marks IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM exam_schedule_subjects existing
      WHERE existing.exam_schedule_id = schedule.id
        AND existing.subject_id = schedule.subject_id
        AND existing.deleted = FALSE
  );
