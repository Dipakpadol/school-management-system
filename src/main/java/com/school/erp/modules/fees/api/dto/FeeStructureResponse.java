package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeScope;
import com.school.erp.modules.fees.domain.FeeStructureStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee structure response.")
public record FeeStructureResponse(
		UUID id,
		UUID academicYearId,
		UUID classId,
		FeeScope feeScope,
		UUID hostelId,
		String hostelName,
		UUID hostelRoomId,
		String hostelRoomNumber,
		String roomType,
		UUID transportRouteId,
		String transportRouteName,
		UUID transportPickupPointId,
		String transportPickupPointName,
		String academicYear,
		String className,
		String sectionName,
		String name,
		String description,
		FeeStructureStatus status,
		BigDecimal totalAmount,
		List<FeeStructureItemResponse> items,
		List<FeeStructureInstallmentResponse> installments,
		Instant createdAt,
		Instant updatedAt) {
}
