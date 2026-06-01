package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

public record AttendanceRecordResponse(
		UUID id,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		LocalDate attendanceDate,
		AttendanceStudentResponse student,
		AttendanceStatus status,
		String remarks) {
}
