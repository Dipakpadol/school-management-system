CREATE TABLE IF NOT EXISTS teacher_attendance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    teacher_id UUID NOT NULL REFERENCES teachers (id),
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

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_teacher_attendance_status') THEN
        ALTER TABLE teacher_attendance
            ADD CONSTRAINT chk_teacher_attendance_status
            CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'HALF_DAY', 'LEAVE'));
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_teacher_attendance_daily_active
    ON teacher_attendance (academic_year_id, teacher_id, attendance_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_teacher_attendance_year_date
    ON teacher_attendance (academic_year_id, attendance_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_teacher_attendance_teacher_year_date
    ON teacher_attendance (teacher_id, academic_year_id, attendance_date)
    WHERE deleted = FALSE;
