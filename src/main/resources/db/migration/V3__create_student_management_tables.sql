CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admission_number VARCHAR(40) NOT NULL,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80),
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL DEFAULT 'UNSPECIFIED',
    blood_group VARCHAR(10),
    email VARCHAR(160),
    phone_number VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    admission_date DATE NOT NULL,
    previous_school VARCHAR(160),
    address_line_1 VARCHAR(160),
    address_line_2 VARCHAR(160),
    city VARCHAR(80),
    state VARCHAR(80),
    postal_code VARCHAR(20),
    country VARCHAR(80),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED')),
    CONSTRAINT chk_students_gender CHECK (gender IN ('MALE', 'FEMALE', 'OTHER', 'UNSPECIFIED'))
);

CREATE UNIQUE INDEX ux_students_admission_number_active ON students (LOWER(admission_number)) WHERE deleted = FALSE;
CREATE INDEX idx_students_status_active ON students (status) WHERE deleted = FALSE;
CREATE INDEX idx_students_admission_date_active ON students (admission_date) WHERE deleted = FALSE;
CREATE INDEX idx_students_name_active ON students (LOWER(first_name), LOWER(last_name)) WHERE deleted = FALSE;

CREATE TABLE parent_guardians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80),
    email VARCHAR(160),
    phone_number VARCHAR(30) NOT NULL,
    alternate_phone_number VARCHAR(30),
    occupation VARCHAR(120),
    address_line_1 VARCHAR(160),
    address_line_2 VARCHAR(160),
    city VARCHAR(80),
    state VARCHAR(80),
    postal_code VARCHAR(20),
    country VARCHAR(80),
    user_account_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_parent_guardians_email_active
    ON parent_guardians (LOWER(email))
    WHERE deleted = FALSE AND email IS NOT NULL;
CREATE INDEX idx_parent_guardians_phone_active ON parent_guardians (phone_number) WHERE deleted = FALSE;

CREATE TABLE student_parents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    parent_id UUID NOT NULL REFERENCES parent_guardians (id),
    relation_type VARCHAR(30) NOT NULL,
    primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    emergency_contact BOOLEAN NOT NULL DEFAULT FALSE,
    pickup_allowed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_parents_relation CHECK (relation_type IN ('FATHER', 'MOTHER', 'GUARDIAN', 'OTHER'))
);

CREATE UNIQUE INDEX ux_student_parent_relation_active
    ON student_parents (student_id, parent_id, relation_type)
    WHERE deleted = FALSE;
CREATE UNIQUE INDEX ux_student_primary_parent_active
    ON student_parents (student_id)
    WHERE deleted = FALSE AND primary_contact = TRUE;
CREATE INDEX idx_student_parents_parent_active ON student_parents (parent_id) WHERE deleted = FALSE;

CREATE TABLE student_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    document_type VARCHAR(40) NOT NULL,
    document_number VARCHAR(80),
    file_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(120),
    file_size BIGINT,
    storage_key VARCHAR(300),
    file_url VARCHAR(500),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    remarks VARCHAR(500),
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_documents_type CHECK (document_type IN (
        'BIRTH_CERTIFICATE',
        'TRANSFER_CERTIFICATE',
        'MARKSHEET',
        'ADDRESS_PROOF',
        'PHOTO',
        'ID_PROOF',
        'MEDICAL_RECORD',
        'OTHER'
    )),
    CONSTRAINT chk_student_documents_verification CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED')),
    CONSTRAINT chk_student_documents_file_size CHECK (file_size IS NULL OR file_size >= 0),
    CONSTRAINT chk_student_documents_location CHECK (storage_key IS NOT NULL OR file_url IS NOT NULL)
);

CREATE INDEX idx_student_documents_student_active ON student_documents (student_id) WHERE deleted = FALSE;
CREATE INDEX idx_student_documents_type_active ON student_documents (document_type) WHERE deleted = FALSE;

CREATE TABLE student_class_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    academic_year VARCHAR(20) NOT NULL,
    class_name VARCHAR(80) NOT NULL,
    section_name VARCHAR(80) NOT NULL,
    roll_number VARCHAR(30),
    effective_from DATE NOT NULL,
    effective_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_assignments_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);

CREATE UNIQUE INDEX ux_student_current_assignment_active
    ON student_class_assignments (student_id)
    WHERE deleted = FALSE AND active = TRUE;
CREATE UNIQUE INDEX ux_student_class_roll_active
    ON student_class_assignments (academic_year, LOWER(class_name), LOWER(section_name), LOWER(roll_number))
    WHERE deleted = FALSE AND active = TRUE AND roll_number IS NOT NULL;
CREATE INDEX idx_student_class_assignments_class_active
    ON student_class_assignments (academic_year, LOWER(class_name), LOWER(section_name))
    WHERE deleted = FALSE AND active = TRUE;
