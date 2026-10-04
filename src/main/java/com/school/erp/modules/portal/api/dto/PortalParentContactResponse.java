package com.school.erp.modules.portal.api.dto;

import java.util.UUID;

import com.school.erp.modules.students.domain.ParentRelation;

public record PortalParentContactResponse(
		UUID id,
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
		String occupation) {
}
