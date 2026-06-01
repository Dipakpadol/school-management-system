package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record DailyAttendanceRequest(
		@NotNull UUID academicYearId,
		@NotNull UUID classId,
		@NotNull UUID sectionId,
		@NotNull LocalDate attendanceDate,
		@Valid @NotEmpty List<DailyAttendanceRecordRequest> records) {
}
