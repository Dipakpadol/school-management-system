package com.school.erp.modules.hostel.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional hostel assignment during student admission or import.")
public record HostelAssignmentRequest(
		@Schema(example = "true") Boolean hostelRequired,
		UUID academicYearId,
		UUID hostelId,
		@Size(max = 160) String hostelName,
		UUID roomId,
		@Size(max = 60) String roomNumber,
		UUID bedId,
		@Size(max = 60) String bedNumber,
		LocalDate allocationDate,
		@Schema(example = "true") Boolean hostelFeeApplicable) {

	public boolean requiresHostel() {
		return Boolean.TRUE.equals(hostelRequired);
	}

	public boolean appliesHostelFee() {
		return Boolean.TRUE.equals(hostelFeeApplicable);
	}
}
