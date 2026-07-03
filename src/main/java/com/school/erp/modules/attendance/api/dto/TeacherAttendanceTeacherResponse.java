package com.school.erp.modules.attendance.api.dto;

import java.util.UUID;

import com.school.erp.modules.academic.domain.TeacherStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher row available for attendance marking.")
public record TeacherAttendanceTeacherResponse(
		UUID id,
		String employeeNumber,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		String email,
		String mobileNumber,
		TeacherStatus status) {
}
