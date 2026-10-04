package com.school.erp.modules.portal.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.library.domain.LibraryMemberType;

public record PortalLibraryMembershipResponse(
		UUID id,
		LibraryMemberType memberType,
		String membershipNumber,
		LocalDate startDate,
		LocalDate expiryDate,
		boolean active) {
}
