package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeePayment;

import org.springframework.data.jpa.repository.EntityGraph;

public interface FeePaymentRepository extends BaseRepository<FeePayment, UUID> {

	@EntityGraph(attributePaths = {
			"assignment",
			"assignment.student",
			"assignment.feeStructure",
			"assignment.installments",
			"assignment.discounts",
			"assignment.payments",
			"receipt",
			"allocations",
			"allocations.installment"
	})
	Optional<FeePayment> findDetailedByIdAndDeletedFalse(UUID id);
}
