ALTER TABLE academic_years
    ADD COLUMN IF NOT EXISTS is_current BOOLEAN NOT NULL DEFAULT FALSE;

WITH ranked_years AS (
    SELECT id,
           ROW_NUMBER() OVER (ORDER BY start_date DESC, name ASC) AS row_number
    FROM academic_years
    WHERE deleted = FALSE
      AND active = TRUE
)
UPDATE academic_years
SET is_current = TRUE
WHERE id IN (SELECT id FROM ranked_years WHERE row_number = 1)
  AND NOT EXISTS (
      SELECT 1
      FROM academic_years existing
      WHERE existing.deleted = FALSE
        AND existing.is_current = TRUE
  );

CREATE UNIQUE INDEX IF NOT EXISTS ux_academic_years_current_active
    ON academic_years (is_current)
    WHERE deleted = FALSE
      AND is_current = TRUE;

CREATE INDEX IF NOT EXISTS idx_parents_phone_name_active
    ON parents (phone_number, LOWER(first_name), LOWER(last_name))
    WHERE deleted = FALSE;

ALTER TABLE exam_schedule_subjects
    ADD COLUMN IF NOT EXISTS start_time TIME,
    ADD COLUMN IF NOT EXISTS end_time TIME,
    ADD COLUMN IF NOT EXISTS room VARCHAR(120);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_exam_schedule_subject_time_range'
          AND conrelid = 'exam_schedule_subjects'::regclass
    ) THEN
        ALTER TABLE exam_schedule_subjects
            ADD CONSTRAINT chk_exam_schedule_subject_time_range
            CHECK (start_time IS NULL OR end_time IS NULL OR end_time > start_time);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_exam_schedule_subject_marks'
          AND conrelid = 'exam_schedule_subjects'::regclass
    ) THEN
        ALTER TABLE exam_schedule_subjects
            ADD CONSTRAINT chk_exam_schedule_subject_marks
            CHECK (
                max_marks > 0
                AND (passing_marks IS NULL OR passing_marks >= 0)
                AND (passing_marks IS NULL OR passing_marks <= max_marks)
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_exam_schedule_subjects_date_time_active
    ON exam_schedule_subjects (exam_date, start_time, end_time)
    WHERE deleted = FALSE;
