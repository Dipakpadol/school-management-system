package com.school.erp.modules.attendance.api.dto;

import java.util.UUID;

import com.school.erp.modules.students.domain.StudentStatus;

public record AttendanceStudentResponse(
		UUID studentId,
		String admissionNumber,
		String rollNumber,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		StudentStatus status) {
}
