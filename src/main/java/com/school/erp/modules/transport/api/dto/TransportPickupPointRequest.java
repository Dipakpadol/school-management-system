package com.school.erp.modules.transport.api.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.school.erp.modules.transport.domain.TransportStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Transport pickup point create/update payload.")
public record TransportPickupPointRequest(
		@NotBlank @Size(max = 160) String pointName,
		LocalTime pickupTime,
		LocalTime dropTime,
		@DecimalMin(value = "0.00", inclusive = true) BigDecimal monthlyFee,
		@Min(1) int sequenceOrder,
		TransportStatus status) {
}
