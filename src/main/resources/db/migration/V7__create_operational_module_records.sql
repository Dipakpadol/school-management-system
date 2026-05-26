CREATE TABLE module_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    module_name VARCHAR(60) NOT NULL,
    record_type VARCHAR(80) NOT NULL,
    code VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(40) NOT NULL DEFAULT 'ACTIVE',
    parent_id UUID,
    owner_id UUID,
    record_date DATE,
    amount NUMERIC(14, 2),
    metadata_json VARCHAR(4000),
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

CREATE UNIQUE INDEX ux_module_records_code_active
    ON module_records (module_name, record_type, LOWER(code))
    WHERE deleted = FALSE;

CREATE INDEX idx_module_records_module_type
    ON module_records (module_name, record_type)
    WHERE deleted = FALSE;

CREATE INDEX idx_module_records_status_date
    ON module_records (status, record_date)
    WHERE deleted = FALSE;

INSERT INTO module_records
    (module_name, record_type, code, name, description, status, record_date, metadata_json, created_by, updated_by)
VALUES
    ('ACADEMIC', 'YEARS', 'AY-2026-27', 'Academic Year 2026-27', 'Default academic year for demo operations.', 'ACTIVE', '2026-04-01', '{"startDate":"2026-04-01","endDate":"2027-03-31"}', 'flyway', 'flyway'),
    ('ACADEMIC', 'CLASSES', 'GRADE-10', 'Grade 10', 'Senior secondary class.', 'ACTIVE', NULL, '{"displayOrder":10}', 'flyway', 'flyway'),
    ('ACADEMIC', 'SECTIONS', '10-A', 'Grade 10 - Section A', 'Primary section for Grade 10.', 'ACTIVE', NULL, '{"classCode":"GRADE-10","capacity":40}', 'flyway', 'flyway'),
    ('ACADEMIC', 'SUBJECTS', 'MATH', 'Mathematics', 'Core mathematics subject.', 'ACTIVE', NULL, '{"subjectType":"CORE"}', 'flyway', 'flyway'),
    ('ACADEMIC', 'TIMETABLE_SLOTS', 'MON-10A-MATH-01', 'Monday Grade 10A Mathematics', 'Demo timetable slot.', 'ACTIVE', NULL, '{"day":"MONDAY","start":"09:00","end":"09:45","classCode":"GRADE-10","sectionCode":"10-A"}', 'flyway', 'flyway'),
    ('HOSTEL', 'HOSTELS', 'MAIN-HOSTEL', 'Main Hostel', 'Primary student hostel.', 'ACTIVE', NULL, '{"warden":"WARDEN"}', 'flyway', 'flyway'),
    ('HOSTEL', 'ROOMS', 'MH-101', 'Main Hostel Room 101', 'Four-bed room.', 'AVAILABLE', NULL, '{"hostelCode":"MAIN-HOSTEL","capacity":4,"occupied":2}', 'flyway', 'flyway'),
    ('HOSTEL', 'BEDS', 'MH-101-B1', 'Room 101 Bed 1', 'Allocated demo bed.', 'OCCUPIED', NULL, '{"roomCode":"MH-101"}', 'flyway', 'flyway'),
    ('ATTENDANCE', 'RECORDS', 'ATT-2026-05-26-10A', 'Grade 10A Attendance 2026-05-26', 'Demo daily attendance register.', 'MARKED', '2026-05-26', '{"classCode":"GRADE-10","sectionCode":"10-A","present":32,"absent":3,"late":1}', 'flyway', 'flyway'),
    ('EXAMS', 'TYPES', 'MIDTERM', 'Mid Term Examination', 'Mid-year assessment cycle.', 'ACTIVE', NULL, '{"weightage":40}', 'flyway', 'flyway'),
    ('EXAMS', 'SCHEDULES', 'MIDTERM-MATH-10A', 'Midterm Mathematics Grade 10A', 'Demo exam schedule.', 'SCHEDULED', '2026-09-15', '{"subjectCode":"MATH","classCode":"GRADE-10","sectionCode":"10-A","maxMarks":100}', 'flyway', 'flyway'),
    ('NOTIFICATIONS', 'TEMPLATES', 'FEE-REMINDER', 'Fee Reminder', 'Reminder for pending fee dues.', 'ACTIVE', NULL, '{"channel":"EMAIL_SMS","subject":"Fee reminder"}', 'flyway', 'flyway'),
    ('NOTIFICATIONS', 'HISTORY', 'WELCOME-DEMO', 'Welcome notification demo', 'Demo notification history item.', 'SENT', '2026-05-26', '{"channel":"EMAIL","recipientCount":3}', 'flyway', 'flyway'),
    ('SETTINGS', 'SCHOOL_PROFILE', 'DEFAULT-SCHOOL', 'Springfield Public School', 'Demo school profile.', 'ACTIVE', NULL, '{"address":"Main Road","city":"Pune","state":"Maharashtra","country":"India"}', 'flyway', 'flyway'),
    ('SETTINGS', 'LOOKUPS', 'PAYMENT-MODES', 'Payment Modes', 'Supported payment modes.', 'ACTIVE', NULL, '{"values":["CASH","UPI","BANK_TRANSFER","CHEQUE","ONLINE"]}', 'flyway', 'flyway'),
    ('REPORTS', 'DEFINITIONS', 'FEE-COLLECTION', 'Fee Collection Report', 'Standard fee collection report definition.', 'ACTIVE', NULL, '{"formats":["EXCEL","CSV","PDF"]}', 'flyway', 'flyway');
