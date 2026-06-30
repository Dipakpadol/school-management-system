CREATE TABLE hostels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(160) NOT NULL,
    address VARCHAR(500),
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

CREATE UNIQUE INDEX ux_hostels_code_active
    ON hostels (LOWER(code))
    WHERE deleted = FALSE;

CREATE TABLE hostel_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hostel_id UUID NOT NULL REFERENCES hostels (id),
    room_number VARCHAR(60) NOT NULL,
    room_type VARCHAR(80) NOT NULL,
    capacity INTEGER NOT NULL,
    has_beds BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_hostel_rooms_capacity CHECK (capacity > 0)
);

CREATE UNIQUE INDEX ux_hostel_rooms_number_active
    ON hostel_rooms (hostel_id, LOWER(room_number))
    WHERE deleted = FALSE;
CREATE INDEX idx_hostel_rooms_hostel_active
    ON hostel_rooms (hostel_id, active)
    WHERE deleted = FALSE;

CREATE TABLE hostel_beds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES hostel_rooms (id),
    bed_number VARCHAR(60) NOT NULL,
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

CREATE UNIQUE INDEX ux_hostel_beds_number_active
    ON hostel_beds (room_id, LOWER(bed_number))
    WHERE deleted = FALSE;

CREATE TABLE hostel_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    hostel_id UUID NOT NULL REFERENCES hostels (id),
    room_id UUID NOT NULL REFERENCES hostel_rooms (id),
    bed_id UUID REFERENCES hostel_beds (id),
    allocation_date DATE NOT NULL,
    vacate_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_hostel_allocations_status CHECK (status IN ('ACTIVE', 'VACATED', 'TRANSFERRED')),
    CONSTRAINT chk_hostel_allocations_dates CHECK (vacate_date IS NULL OR vacate_date >= allocation_date)
);

CREATE UNIQUE INDEX ux_hostel_allocations_student_year_active
    ON hostel_allocations (student_id, academic_year_id)
    WHERE deleted = FALSE AND status = 'ACTIVE';
CREATE UNIQUE INDEX ux_hostel_allocations_bed_year_active
    ON hostel_allocations (academic_year_id, bed_id)
    WHERE deleted = FALSE AND status = 'ACTIVE' AND bed_id IS NOT NULL;
CREATE INDEX idx_hostel_allocations_room_year_active
    ON hostel_allocations (room_id, academic_year_id, status)
    WHERE deleted = FALSE;

ALTER TABLE fee_structures
    ADD COLUMN IF NOT EXISTS fee_scope VARCHAR(30) NOT NULL DEFAULT 'CLASS',
    ADD COLUMN IF NOT EXISTS hostel_id UUID REFERENCES hostels (id),
    ADD COLUMN IF NOT EXISTS hostel_room_id UUID REFERENCES hostel_rooms (id),
    ADD COLUMN IF NOT EXISTS room_type VARCHAR(80);

ALTER TABLE student_fee_assignments
    ADD COLUMN IF NOT EXISTS fee_scope VARCHAR(30) NOT NULL DEFAULT 'CLASS',
    ADD COLUMN IF NOT EXISTS hostel_id UUID REFERENCES hostels (id),
    ADD COLUMN IF NOT EXISTS hostel_room_id UUID REFERENCES hostel_rooms (id),
    ADD COLUMN IF NOT EXISTS room_type VARCHAR(80);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_fee_structures_scope') THEN
        ALTER TABLE fee_structures
            ADD CONSTRAINT chk_fee_structures_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_student_fee_assignments_scope') THEN
        ALTER TABLE student_fee_assignments
            ADD CONSTRAINT chk_student_fee_assignments_scope CHECK (fee_scope IN ('CLASS', 'HOSTEL'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_fee_structures_scope_active
    ON fee_structures (fee_scope, academic_year_id, hostel_id, hostel_room_id, room_type, status)
    WHERE deleted = FALSE;
CREATE INDEX IF NOT EXISTS idx_student_fee_assignments_scope_active
    ON student_fee_assignments (fee_scope, academic_year_id, hostel_id, hostel_room_id, room_type, status)
    WHERE deleted = FALSE;

CREATE TABLE hostel_fee_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    hostel_id UUID NOT NULL REFERENCES hostels (id),
    room_id UUID REFERENCES hostel_rooms (id),
    room_type VARCHAR(80),
    fee_category_id UUID NOT NULL REFERENCES fee_categories (id),
    backing_fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    amount NUMERIC(12, 2) NOT NULL,
    due_date DATE NOT NULL,
    installment_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    number_of_installments INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_hostel_fee_structures_amount CHECK (amount >= 0),
    CONSTRAINT chk_hostel_fee_structures_installments CHECK (number_of_installments > 0),
    CONSTRAINT chk_hostel_fee_structures_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE'))
);

CREATE UNIQUE INDEX ux_hostel_fee_structures_scope_active
    ON hostel_fee_structures (
        academic_year_id,
        hostel_id,
        fee_category_id,
        COALESCE(room_id, '00000000-0000-0000-0000-000000000000'::uuid),
        LOWER(COALESCE(room_type, ''))
    )
    WHERE deleted = FALSE;
CREATE INDEX idx_hostel_fee_structures_lookup_active
    ON hostel_fee_structures (academic_year_id, hostel_id, status)
    WHERE deleted = FALSE;

INSERT INTO hostels (code, name, address, active, created_by, updated_by)
SELECT 'MAIN-HOSTEL', 'Main Hostel', 'Campus Block A', TRUE, 'flyway', 'flyway'
WHERE NOT EXISTS (
    SELECT 1 FROM hostels WHERE deleted = FALSE AND LOWER(code) = LOWER('MAIN-HOSTEL')
);

WITH hostel_row AS (
    SELECT id FROM hostels WHERE deleted = FALSE AND LOWER(code) = LOWER('MAIN-HOSTEL')
)
INSERT INTO hostel_rooms (hostel_id, room_number, room_type, capacity, has_beds, active, created_by, updated_by)
SELECT hostel_row.id, seed.room_number, seed.room_type, seed.capacity, TRUE, TRUE, 'flyway', 'flyway'
FROM hostel_row
CROSS JOIN (VALUES
    ('101', 'STANDARD', 4),
    ('102', 'STANDARD', 4),
    ('201', 'DELUXE', 2)
) AS seed(room_number, room_type, capacity)
WHERE NOT EXISTS (
    SELECT 1
    FROM hostel_rooms existing
    WHERE existing.deleted = FALSE
      AND existing.hostel_id = hostel_row.id
      AND LOWER(existing.room_number) = LOWER(seed.room_number)
);

WITH room_rows AS (
    SELECT id, capacity FROM hostel_rooms WHERE deleted = FALSE AND has_beds = TRUE
)
INSERT INTO hostel_beds (room_id, bed_number, active, created_by, updated_by)
SELECT room_rows.id, 'B' || bed_no, TRUE, 'flyway', 'flyway'
FROM room_rows
CROSS JOIN generate_series(1, room_rows.capacity) AS bed_no
WHERE NOT EXISTS (
    SELECT 1
    FROM hostel_beds existing
    WHERE existing.deleted = FALSE
      AND existing.room_id = room_rows.id
      AND LOWER(existing.bed_number) = LOWER('B' || bed_no)
);

UPDATE menu_items
SET route_path = '/hostels',
    updated_by = 'flyway',
    updated_at = CURRENT_TIMESTAMP
WHERE module_id = 'hostel'
  AND deleted = FALSE;
