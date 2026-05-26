package com.school.erp.common.modules.api.dto;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

public record ModuleRecordSearchRequest(
		String query,
		String status,
		UUID parentId,
		UUID ownerId,
		Boolean active,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
}
