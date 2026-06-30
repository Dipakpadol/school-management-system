package com.school.erp.modules.transport.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Transport pickup point response.")
public record TransportPickupPointResponse(
		UUID id,
		UUID routeId,
		String routeName,
		String pointName,
		LocalTime pickupTime,
		LocalTime dropTime,
		BigDecimal monthlyFee,
		int sequenceOrder,
		TransportStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
