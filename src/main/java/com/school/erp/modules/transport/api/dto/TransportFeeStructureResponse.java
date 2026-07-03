package com.school.erp.modules.transport.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.modules.fees.domain.FeeStructureStatus;

public record TransportFeeStructureResponse(
		UUID id,
		UUID academicYearId,
		String academicYear,
		UUID routeId,
		String routeName,
		String routeCode,
		UUID pickupPointId,
		String pickupPointName,
		UUID feeCategoryId,
		String feeCategoryCode,
		String feeCategoryName,
		UUID feeStructureId,
		String feeStructureName,
		BigDecimal amount,
		LocalDate dueDate,
		boolean installmentAllowed,
		int numberOfInstallments,
		FeeStructureStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
