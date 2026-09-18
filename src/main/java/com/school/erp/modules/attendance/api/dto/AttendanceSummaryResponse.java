package com.school.erp.modules.attendance.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceSummaryResponse(
		UUID academicYearId,
		String academicYear,
		UUID classId,
		String className,
		UUID sectionId,
		String sectionName,
		LocalDate fromDate,
		LocalDate toDate,
		long eligibleStudentCount,
		long totalRecords,
		long presentCount,
		long absentCount,
		long lateCount,
		long halfDayCount,
		long leaveCount,
		BigDecimal attendancePercentage) {
}
