package com.school.erp.common.audit.api.dto;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

public record AuditLogSearchRequest(
		String query,
		String moduleName,
		String entityName,
		String entityId,
		String action,
		String performedBy,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant performedFrom,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant performedTo,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
}
