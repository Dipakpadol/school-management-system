package com.school.erp.modules.library.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibraryIssueRequest(
		@NotNull UUID copyId,
		@NotNull UUID membershipId,
		LocalDate issueDate,
		LocalDate dueDate,
		@Size(max = 500) String remarks) {
}
