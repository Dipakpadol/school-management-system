package com.school.erp.modules.portal.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.students.api.dto.ClassSectionAssignmentResponse;
import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.StudentStatus;

public record PortalStudentProfileResponse(
		UUID id,
		String admissionNumber,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		LocalDate dateOfBirth,
		Gender gender,
		String bloodGroup,
		String email,
		String phoneNumber,
		StudentStatus status,
		LocalDate admissionDate,
		String photoUrl,
		String photoContentType,
		String photoFileName,
		ClassSectionAssignmentResponse currentAssignment,
		List<ClassSectionAssignmentResponse> classAssignments,
		List<PortalParentContactResponse> parents,
		List<PortalStudentDocumentResponse> documents,
		Instant createdAt,
		Instant updatedAt) {
}
