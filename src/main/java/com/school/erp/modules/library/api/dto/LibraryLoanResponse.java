package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;

public record LibraryLoanResponse(
		UUID id,
		UUID copyId,
		String accessionNumber,
		UUID bookId,
		String bookTitle,
		UUID membershipId,
		String membershipNumber,
		LibraryMemberType memberType,
		String memberName,
		LocalDate issueDate,
		LocalDate dueDate,
		LocalDate returnDate,
		LibraryLoanStatus status,
		boolean overdue,
		BigDecimal pendingFine,
		String remarks) {
}
