package com.school.erp.common.modules.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Generic module record request used by setup modules.")
public record ModuleRecordRequest(
		@NotBlank @Size(max = 80) String code,
		@NotBlank @Size(max = 160) String name,
		@Size(max = 1000) String description,
		@Size(max = 40) String status,
		UUID parentId,
		UUID ownerId,
		LocalDate recordDate,
		@DecimalMin("0.00") BigDecimal amount,
		@Size(max = 4000) String metadataJson,
		Boolean active) {
}
