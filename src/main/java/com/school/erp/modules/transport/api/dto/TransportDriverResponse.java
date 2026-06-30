package com.school.erp.modules.transport.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transport driver response.")
public record TransportDriverResponse(
		UUID id,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		String mobileNumber,
		String licenseNumber,
		LocalDate licenseExpiryDate,
		String address,
		TransportStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
