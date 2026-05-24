package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.fees.domain.LateFeeCalculationType;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Late fee rule response.")
public record LateFeeRuleResponse(
		UUID id,
		String name,
		String academicYear,
		String className,
		String sectionName,
		int graceDays,
		LateFeeCalculationType calculationType,
		BigDecimal amount,
		BigDecimal maxAmount,
		boolean active,
		Instant createdAt,
		Instant updatedAt) {
}
