package com.school.erp.modules.attendance.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.school.erp.common.api.PageResponse;

public record StudentAttendanceHistoryResponse(
		UUID studentId,
		String studentName,
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		long totalWorkingDays,
		long presentDays,
		long absentDays,
		long lateDays,
		long halfDays,
		long leaveDays,
		BigDecimal attendancePercentage,
		PageResponse<StudentAttendanceHistoryRecordResponse> attendanceRecords) {
}
