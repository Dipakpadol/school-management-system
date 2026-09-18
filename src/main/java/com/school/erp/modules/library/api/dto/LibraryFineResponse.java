package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryFineStatus;

public record LibraryFineResponse(
		UUID id,
		UUID loanId,
		String accessionNumber,
		String bookTitle,
		String membershipNumber,
		String memberName,
		BigDecimal amount,
		BigDecimal paidAmount,
		String reason,
		LocalDate fineDate,
		LibraryFineStatus status) {
}
