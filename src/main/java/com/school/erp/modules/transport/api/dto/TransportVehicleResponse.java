package com.school.erp.modules.transport.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transport vehicle response.")
public record TransportVehicleResponse(
		UUID id,
		UUID academicYearId,
		String academicYear,
		String vehicleNumber,
		String vehicleName,
		String vehicleType,
		int capacity,
		int occupiedCount,
		int availableSeats,
		UUID driverId,
		String driverName,
		String driverMobile,
		TransportStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
