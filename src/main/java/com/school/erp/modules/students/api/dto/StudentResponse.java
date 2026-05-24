package com.school.erp.modules.students.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.students.domain.Gender;
import com.school.erp.modules.students.domain.StudentStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Complete student profile response.")
public record StudentResponse(
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
		String previousSchool,
		String addressLine1,
		String addressLine2,
		String city,
		String state,
		String postalCode,
		String country,
		List<ParentMappingResponse> parents,
		List<StudentDocumentResponse> documents,
		ClassSectionAssignmentResponse currentAssignment,
		List<ClassSectionAssignmentResponse> classAssignments,
		Instant createdAt,
		Instant updatedAt) {
}
