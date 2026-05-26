package com.school.erp.common.modules.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ModuleRecordResponse(
		UUID id,
		String moduleName,
		String recordType,
		String code,
		String name,
		String description,
		String status,
		UUID parentId,
		UUID ownerId,
		LocalDate recordDate,
		BigDecimal amount,
		String metadataJson,
		boolean active,
		Instant createdAt,
		Instant updatedAt,
		String createdBy,
		String updatedBy) {
}
