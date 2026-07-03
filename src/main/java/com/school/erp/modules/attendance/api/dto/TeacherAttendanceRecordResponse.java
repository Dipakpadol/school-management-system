package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.attendance.domain.AttendanceStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher attendance record response.")
public record TeacherAttendanceRecordResponse(
		UUID id,
		UUID academicYearId,
		LocalDate attendanceDate,
		TeacherAttendanceTeacherResponse teacher,
		AttendanceStatus status,
		String remarks) {
}
