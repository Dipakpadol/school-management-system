package com.school.erp.modules.library.api.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record LibraryReturnRequest(
		LocalDate returnDate,
		Boolean markDamaged,
		@Size(max = 500) String remarks) {
}
