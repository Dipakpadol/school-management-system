package com.school.erp.modules.library.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryMemberType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibraryMembershipRequest(
		@NotNull LibraryMemberType memberType,
		UUID studentId,
		UUID teacherId,
		UUID staffId,
		@NotBlank @Size(max = 60) String membershipNumber,
		@NotNull LocalDate startDate,
		LocalDate expiryDate,
		Boolean active,
		@Size(max = 500) String notes) {
}
