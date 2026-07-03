package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher daily attendance response.")
public record TeacherDailyAttendanceResponse(
		UUID academicYearId,
		LocalDate attendanceDate,
		long totalRecords,
		long present,
		long absent,
		long late,
		long halfDay,
		long leave,
		List<TeacherAttendanceRecordResponse> records) {
}
