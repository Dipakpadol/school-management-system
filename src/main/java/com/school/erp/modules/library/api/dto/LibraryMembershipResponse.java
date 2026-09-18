package com.school.erp.modules.library.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryMemberType;

public record LibraryMembershipResponse(
		UUID id,
		LibraryMemberType memberType,
		UUID memberId,
		String memberCode,
		String memberName,
		String membershipNumber,
		LocalDate startDate,
		LocalDate expiryDate,
		boolean active,
		String notes) {
}
