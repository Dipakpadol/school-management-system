package com.school.erp.modules.transport.api.dto;

import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Transport vehicle create/update payload.")
public record TransportVehicleRequest(
		@NotNull UUID academicYearId,
		@NotBlank @Size(max = 60) String vehicleNumber,
		@NotBlank @Size(max = 160) String vehicleName,
		@NotBlank @Size(max = 80) String vehicleType,
		@Min(1) int capacity,
		UUID driverId,
		TransportStatus status) {
}
