package com.school.erp.modules.hostel.api.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HostelRoomRequest(
		@NotNull UUID hostelId,
		@NotBlank @Size(max = 60) @Schema(example = "101") String roomNumber,
		@NotBlank @Size(max = 80) @Schema(example = "STANDARD") String roomType,
		@Min(1) int capacity,
		Boolean bedConceptEnabled,
		Boolean active) {

	public boolean usesBeds() {
		return bedConceptEnabled != null && bedConceptEnabled;
	}

	public boolean activeFlag() {
		return active == null || active;
	}
}
