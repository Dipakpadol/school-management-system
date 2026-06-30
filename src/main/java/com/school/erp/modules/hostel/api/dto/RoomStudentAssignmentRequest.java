package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoomStudentAssignmentRequest(
		@NotNull UUID studentId,
		@NotNull UUID academicYearId,
		UUID bedId,
		@Size(max = 60) String bedNumber,
		@NotNull LocalDate allocationDate,
		boolean hostelFeeApplicable) {
}
