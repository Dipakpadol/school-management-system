package com.school.erp.modules.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TodayAttendanceResponse(
		LocalDate date,
		long totalStudents,
		long present,
		long absent,
		long late,
		long halfDay,
		long leave,
		BigDecimal attendancePercentage,
		List<ClassWiseAttendanceResponse> classWiseSummary) {

	public TodayAttendanceResponse {
		attendancePercentage = attendancePercentage == null ? BigDecimal.ZERO : attendancePercentage;
		classWiseSummary = classWiseSummary == null ? List.of() : List.copyOf(classWiseSummary);
	}

	public record ClassWiseAttendanceResponse(
			UUID classId,
			String className,
			UUID sectionId,
			String sectionName,
			long totalStudents,
			long present,
			long absent,
			long late,
			long halfDay,
			long leave,
			BigDecimal attendancePercentage) {
	}
}
