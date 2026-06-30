package com.school.erp.modules.transport.api.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Vehicle details with route, pickup point, driver, and assigned students.")
public record TransportVehicleDetailsResponse(
		TransportVehicleResponse vehicle,
		TransportDriverResponse driver,
		List<TransportRouteResponse> routes,
		List<TransportPickupPointResponse> pickupPoints,
		List<TransportStudentAssignmentResponse> assignedStudents) {
}
