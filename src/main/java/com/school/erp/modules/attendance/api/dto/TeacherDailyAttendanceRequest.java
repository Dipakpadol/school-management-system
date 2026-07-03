package com.school.erp.modules.attendance.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Teacher daily attendance save request.")
public record TeacherDailyAttendanceRequest(
		@NotNull UUID academicYearId,
		@NotNull LocalDate attendanceDate,
		@NotEmpty List<@Valid TeacherDailyAttendanceRecordRequest> records) {
}
