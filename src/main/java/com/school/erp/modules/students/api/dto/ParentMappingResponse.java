package com.school.erp.modules.students.api.dto;

import java.util.UUID;

import com.school.erp.modules.students.domain.ParentRelation;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Mapped parent or guardian summary.")
public record ParentMappingResponse(
		UUID mappingId,
		UUID parentId,
		ParentRelation relationType,
		boolean primaryContact,
		boolean emergencyContact,
		boolean pickupAllowed,
		String displayName,
		String firstName,
		String lastName,
		String email,
		String phoneNumber,
		String alternatePhoneNumber,
		String occupation,
		String addressLine1,
		String addressLine2,
		String city,
		String state,
		String postalCode,
		String country,
		UUID userAccountId) {
}
