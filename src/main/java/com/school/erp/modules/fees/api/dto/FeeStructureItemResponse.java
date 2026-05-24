package com.school.erp.modules.fees.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee structure item response.")
public record FeeStructureItemResponse(
		UUID id,
		UUID categoryId,
		String categoryCode,
		String categoryName,
		BigDecimal amount,
		boolean mandatory,
		int sortOrder) {
}
