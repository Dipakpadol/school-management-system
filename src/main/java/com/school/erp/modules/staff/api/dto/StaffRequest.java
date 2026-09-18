package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.students.domain.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffRequest(
		@NotBlank String employeeCode,
		@NotBlank String firstName,
		String middleName,
		String lastName,
		Gender gender,
		LocalDate dateOfBirth,
		@Email String email,
		String phoneNumber,
		UUID userAccountId,
		UUID teacherId,
		@NotNull UUID departmentId,
		@NotNull UUID designationId,
		@NotNull LocalDate joiningDate,
		StaffType staffType,
		EmploymentStatus status) {
}
