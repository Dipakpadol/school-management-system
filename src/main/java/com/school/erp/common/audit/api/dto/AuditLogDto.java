package com.school.erp.common.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
		UUID id,
		String moduleName,
		String entityName,
		String entityId,
		String action,
		String oldValue,
		String newValue,
		String performedBy,
		Instant performedAt,
		String ipAddress,
		Instant createdAt) {
}
