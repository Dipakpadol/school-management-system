package com.school.erp.modules.library.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryBookCopyStatus;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibraryBookCopyRequest(
		@NotNull UUID bookId,
		@NotBlank @Size(max = 60) String accessionNumber,
		@Size(max = 80) String shelfLocation,
		LocalDate acquiredOn,
		@DecimalMin("0.00") BigDecimal price,
		@Size(max = 500) String conditionNote,
		LibraryBookCopyStatus status) {
}
