CREATE TABLE IF NOT EXISTS generated_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    academic_year_id UUID NOT NULL REFERENCES academic_years (id),
    document_type VARCHAR(40) NOT NULL,
    document_number VARCHAR(80) NOT NULL,
    issue_date DATE NOT NULL,
    purpose VARCHAR(300),
    remarks VARCHAR(500),
    file_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    content BYTEA NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    reprint_of_document_id UUID REFERENCES generated_documents (id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_generated_documents_type CHECK (document_type IN ('BONAFIDE_CERTIFICATE', 'LEAVING_CERTIFICATE', 'STUDENT_ID_CARD')),
    CONSTRAINT chk_generated_documents_status CHECK (status IN ('GENERATED', 'REPRINTED', 'CANCELLED')),
    CONSTRAINT chk_generated_documents_file_size CHECK (file_size >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_generated_documents_number_active
    ON generated_documents (document_number)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_generated_documents_student
    ON generated_documents (student_id, document_type, issue_date)
    WHERE deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_generated_documents_active_lc
    ON generated_documents (student_id, document_type)
    WHERE deleted = FALSE
      AND status = 'GENERATED'
      AND document_type = 'LEAVING_CERTIFICATE';
