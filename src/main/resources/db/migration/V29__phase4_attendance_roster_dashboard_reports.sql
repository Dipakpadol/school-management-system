DROP INDEX IF EXISTS ux_student_attendance_daily_active;

CREATE UNIQUE INDEX IF NOT EXISTS ux_student_attendance_student_date_active
    ON student_attendance (student_id, attendance_date)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_student_class_assignments_effective_roster
    ON student_class_assignments (academic_year_id, class_id, section_id, effective_from, effective_to)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_student_attendance_class_summary
    ON student_attendance (academic_year_id, class_id, section_id, attendance_date)
    WHERE deleted = FALSE;
