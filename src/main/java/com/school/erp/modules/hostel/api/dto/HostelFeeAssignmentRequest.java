package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record HostelFeeAssignmentRequest(
		@NotNull UUID studentId,
		UUID academicYearId,
		UUID hostelFeeStructureId,
		LocalDate assignedDate) {
}
