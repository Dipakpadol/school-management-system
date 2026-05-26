ALTER TABLE fee_payments
    DROP CONSTRAINT IF EXISTS chk_fee_payments_status;

ALTER TABLE fee_payments
    ADD CONSTRAINT chk_fee_payments_status
    CHECK (status IN ('COMPLETED', 'CANCELLED', 'REVERSED', 'VOIDED', 'REFUNDED'));
