package com.school.erp.modules.staff.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.students.domain.Gender;

public record StaffResponse(
		UUID id,
		String employeeCode,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		Gender gender,
		LocalDate dateOfBirth,
		String email,
		String phoneNumber,
		UUID userAccountId,
		UUID teacherId,
		UUID departmentId,
		String departmentName,
		UUID designationId,
		String designationName,
		LocalDate joiningDate,
		StaffType staffType,
		EmploymentStatus status,
		LocalDate relievingDate,
		String exitReason) {
}
