package com.school.erp.modules.transport.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional transport assignment payload for admission, import, or profile assignment.")
public record TransportAssignmentRequest(
		Boolean transportRequired,
		UUID academicYearId,
		UUID vehicleId,
		@Size(max = 60) String vehicleNumber,
		UUID routeId,
		@Size(max = 160) String routeName,
		UUID pickupPointId,
		@Size(max = 160) String pickupPointName,
		LocalDate assignmentDate,
		Boolean transportFeeApplicable) {

	public boolean requiresTransport() {
		return Boolean.TRUE.equals(transportRequired);
	}

	public boolean appliesTransportFee() {
		return Boolean.TRUE.equals(transportFeeApplicable);
	}
}
