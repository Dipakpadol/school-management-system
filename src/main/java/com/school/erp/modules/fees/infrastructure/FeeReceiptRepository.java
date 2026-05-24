package com.school.erp.modules.fees.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.fees.domain.FeeReceipt;

import org.springframework.data.jpa.repository.EntityGraph;

public interface FeeReceiptRepository extends BaseRepository<FeeReceipt, UUID> {

	boolean existsByReceiptNumberAndDeletedFalse(String receiptNumber);

	@EntityGraph(attributePaths = { "student", "assignment" })
	Optional<FeeReceipt> findByReceiptNumberAndDeletedFalse(String receiptNumber);
}
