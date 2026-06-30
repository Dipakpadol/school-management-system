package com.school.erp.modules.teachers.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.academic.domain.TeacherStatus;
import com.school.erp.modules.students.domain.Gender;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher response.")
public record TeacherResponse(
		UUID id,
		String employeeCode,
		String employeeNumber,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		Gender gender,
		LocalDate dateOfBirth,
		String mobileNumber,
		String phoneNumber,
		String email,
		String qualification,
		Integer experienceYears,
		LocalDate joiningDate,
		TeacherStatus status,
		UUID userId,
		long assignedClassesCount,
		long assignedSubjectsCount,
		Instant createdAt,
		Instant updatedAt) {
}
