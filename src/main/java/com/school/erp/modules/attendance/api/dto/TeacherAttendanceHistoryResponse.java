package com.school.erp.modules.attendance.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.school.erp.common.api.PageResponse;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Teacher attendance summary and paginated history.")
public record TeacherAttendanceHistoryResponse(
		UUID teacherId,
		String teacherName,
		String employeeNumber,
		UUID academicYearId,
		String academicYear,
		long totalWorkingDays,
		long present,
		long absent,
		long late,
		long halfDay,
		long leave,
		BigDecimal attendancePercentage,
		PageResponse<TeacherAttendanceHistoryRecordResponse> records) {
}
