CREATE TABLE fee_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(60) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_fee_categories_code_active ON fee_categories (LOWER(code)) WHERE deleted = FALSE;
CREATE INDEX idx_fee_categories_active ON fee_categories (active, sort_order) WHERE deleted = FALSE;

CREATE TABLE fee_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year VARCHAR(20) NOT NULL,
    class_name VARCHAR(80) NOT NULL,
    section_name VARCHAR(80),
    name VARCHAR(140) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_structures_status CHECK (status IN ('DRAFT', 'ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_fee_structures_total CHECK (total_amount >= 0)
);

CREATE UNIQUE INDEX ux_fee_structures_class_active
    ON fee_structures (LOWER(academic_year), LOWER(class_name), COALESCE(LOWER(section_name), ''))
    WHERE deleted = FALSE;
CREATE INDEX idx_fee_structures_lookup_active
    ON fee_structures (LOWER(academic_year), LOWER(class_name), LOWER(section_name), status)
    WHERE deleted = FALSE;

CREATE TABLE fee_structure_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    fee_category_id UUID NOT NULL REFERENCES fee_categories (id),
    amount NUMERIC(12, 2) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_structure_items_amount CHECK (amount >= 0)
);

CREATE UNIQUE INDEX ux_fee_structure_items_category_active
    ON fee_structure_items (fee_structure_id, fee_category_id)
    WHERE deleted = FALSE;

CREATE TABLE fee_structure_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    sequence_no INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_structure_installments_amount CHECK (amount >= 0)
);

CREATE UNIQUE INDEX ux_fee_structure_installments_sequence_active
    ON fee_structure_installments (fee_structure_id, sequence_no)
    WHERE deleted = FALSE;

CREATE TABLE late_fee_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(120) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    class_name VARCHAR(80),
    section_name VARCHAR(80),
    grace_days INTEGER NOT NULL DEFAULT 0,
    calculation_type VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    max_amount NUMERIC(12, 2),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_late_fee_rules_type CHECK (calculation_type IN ('FLAT', 'PER_DAY', 'PERCENTAGE')),
    CONSTRAINT chk_late_fee_rules_grace_days CHECK (grace_days >= 0),
    CONSTRAINT chk_late_fee_rules_amount CHECK (amount >= 0),
    CONSTRAINT chk_late_fee_rules_max_amount CHECK (max_amount IS NULL OR max_amount >= 0)
);

CREATE INDEX idx_late_fee_rules_lookup_active
    ON late_fee_rules (LOWER(academic_year), LOWER(class_name), LOWER(section_name), active)
    WHERE deleted = FALSE;

CREATE TABLE student_fee_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students (id),
    fee_structure_id UUID NOT NULL REFERENCES fee_structures (id),
    academic_year VARCHAR(20) NOT NULL,
    class_name VARCHAR(80) NOT NULL,
    section_name VARCHAR(80),
    assigned_date DATE NOT NULL,
    gross_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    late_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_fee_assignments_status CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT chk_student_fee_assignments_amounts CHECK (
        gross_amount >= 0 AND discount_amount >= 0 AND late_fee_amount >= 0
        AND paid_amount >= 0 AND balance_amount >= 0
    )
);

CREATE UNIQUE INDEX ux_student_fee_assignment_structure_active
    ON student_fee_assignments (student_id, fee_structure_id)
    WHERE deleted = FALSE;
CREATE INDEX idx_student_fee_assignments_student_active
    ON student_fee_assignments (student_id, status)
    WHERE deleted = FALSE;
CREATE INDEX idx_student_fee_assignments_report_active
    ON student_fee_assignments (LOWER(academic_year), LOWER(class_name), LOWER(section_name), status)
    WHERE deleted = FALSE;
CREATE INDEX idx_student_fee_assignments_balance_active
    ON student_fee_assignments (balance_amount)
    WHERE deleted = FALSE;

CREATE TABLE student_fee_installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES student_fee_assignments (id),
    sequence_no INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    late_fee_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    payable_amount NUMERIC(12, 2) NOT NULL,
    paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    balance_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_student_fee_installments_status CHECK (status IN ('PENDING', 'PARTIALLY_PAID', 'PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT chk_student_fee_installments_amounts CHECK (
        amount >= 0 AND discount_amount >= 0 AND late_fee_amount >= 0
        AND payable_amount >= 0 AND paid_amount >= 0 AND balance_amount >= 0
    )
);

CREATE UNIQUE INDEX ux_student_fee_installments_sequence_active
    ON student_fee_installments (assignment_id, sequence_no)
    WHERE deleted = FALSE;
CREATE INDEX idx_student_fee_installments_due_active
    ON student_fee_installments (due_date, status, balance_amount)
    WHERE deleted = FALSE;

CREATE TABLE fee_discounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES student_fee_assignments (id),
    installment_id UUID REFERENCES student_fee_installments (id),
    discount_type VARCHAR(40) NOT NULL,
    calculation_type VARCHAR(30) NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    approved_by VARCHAR(100),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_discounts_type CHECK (discount_type IN ('SCHOLARSHIP', 'SIBLING', 'STAFF', 'WAIVER', 'PROMOTIONAL', 'OTHER')),
    CONSTRAINT chk_fee_discounts_calculation CHECK (calculation_type IN ('FLAT', 'PERCENTAGE')),
    CONSTRAINT chk_fee_discounts_amounts CHECK (discount_value >= 0 AND amount >= 0)
);

CREATE INDEX idx_fee_discounts_assignment_active ON fee_discounts (assignment_id) WHERE deleted = FALSE;

CREATE TABLE fee_receipts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    receipt_number VARCHAR(40) NOT NULL,
    student_id UUID NOT NULL REFERENCES students (id),
    assignment_id UUID NOT NULL REFERENCES student_fee_assignments (id),
    receipt_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_amount NUMERIC(12, 2) NOT NULL,
    payer_name VARCHAR(160) NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_receipts_payment_mode CHECK (payment_mode IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'ONLINE')),
    CONSTRAINT chk_fee_receipts_status CHECK (status IN ('ISSUED', 'CANCELLED')),
    CONSTRAINT chk_fee_receipts_total CHECK (total_amount > 0)
);

CREATE UNIQUE INDEX ux_fee_receipts_number_active ON fee_receipts (receipt_number) WHERE deleted = FALSE;
CREATE INDEX idx_fee_receipts_student_active ON fee_receipts (student_id, receipt_date) WHERE deleted = FALSE;

CREATE TABLE fee_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES student_fee_assignments (id),
    receipt_id UUID NOT NULL REFERENCES fee_receipts (id),
    amount NUMERIC(12, 2) NOT NULL,
    payment_date DATE NOT NULL,
    payment_mode VARCHAR(30) NOT NULL,
    reference_number VARCHAR(120),
    payer_name VARCHAR(160) NOT NULL,
    collected_by VARCHAR(100),
    remarks VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_payments_mode CHECK (payment_mode IN ('CASH', 'UPI', 'BANK_TRANSFER', 'CHEQUE', 'ONLINE')),
    CONSTRAINT chk_fee_payments_status CHECK (status IN ('COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_fee_payments_amount CHECK (amount > 0)
);

CREATE INDEX idx_fee_payments_assignment_active ON fee_payments (assignment_id, payment_date) WHERE deleted = FALSE;
CREATE UNIQUE INDEX ux_fee_payments_reference_active
    ON fee_payments (payment_mode, reference_number)
    WHERE deleted = FALSE AND reference_number IS NOT NULL;

CREATE TABLE fee_payment_allocations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES fee_payments (id),
    installment_id UUID NOT NULL REFERENCES student_fee_installments (id),
    amount NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_fee_payment_allocations_amount CHECK (amount > 0)
);

CREATE INDEX idx_fee_payment_allocations_payment_active ON fee_payment_allocations (payment_id) WHERE deleted = FALSE;
CREATE INDEX idx_fee_payment_allocations_installment_active ON fee_payment_allocations (installment_id) WHERE deleted = FALSE;
