package com.school.erp.modules.transport.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transport route response.")
public record TransportRouteResponse(
		UUID id,
		UUID academicYearId,
		String academicYear,
		String routeName,
		String routeCode,
		String startLocation,
		String endLocation,
		UUID vehicleId,
		String vehicleNumber,
		String vehicleName,
		TransportStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
