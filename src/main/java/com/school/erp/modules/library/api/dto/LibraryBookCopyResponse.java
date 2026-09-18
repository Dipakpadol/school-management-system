package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryBookCopyStatus;

public record LibraryBookCopyResponse(
		UUID id,
		UUID bookId,
		String bookTitle,
		String accessionNumber,
		LibraryBookCopyStatus status,
		String shelfLocation,
		LocalDate acquiredOn,
		BigDecimal price,
		String conditionNote) {
}
