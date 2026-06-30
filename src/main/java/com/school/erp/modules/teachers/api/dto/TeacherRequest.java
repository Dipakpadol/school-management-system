package com.school.erp.modules.teachers.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.academic.domain.TeacherStatus;
import com.school.erp.modules.students.domain.Gender;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "Teacher create/update payload.")
public record TeacherRequest(
		@NotBlank @Size(max = 40) String employeeCode,
		@NotBlank @Size(max = 80) String firstName,
		@Size(max = 80) String middleName,
		@Size(max = 80) String lastName,
		Gender gender,
		@Past LocalDate dateOfBirth,
		@Size(max = 30) String mobileNumber,
		@Email @Size(max = 160) String email,
		@Size(max = 160) String qualification,
		@Min(0) Integer experienceYears,
		LocalDate joiningDate,
		TeacherStatus status,
		UUID userId) {
}
