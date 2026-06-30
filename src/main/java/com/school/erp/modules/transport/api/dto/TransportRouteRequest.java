package com.school.erp.modules.transport.api.dto;

import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Transport route create/update payload.")
public record TransportRouteRequest(
		@NotNull UUID academicYearId,
		@NotBlank @Size(max = 160) String routeName,
		@NotBlank @Size(max = 60) String routeCode,
		@NotBlank @Size(max = 160) String startLocation,
		@NotBlank @Size(max = 160) String endLocation,
		UUID vehicleId,
		TransportStatus status) {
}
