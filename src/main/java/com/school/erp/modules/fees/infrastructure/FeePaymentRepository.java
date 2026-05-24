package com.school.erp.modules.fees.infrastructure;

import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeePayment;

public interface FeePaymentRepository extends BaseRepository<FeePayment, UUID> {
}
