package com.school.erp.common.audit.application;

public record AuditLogEvent(
		String moduleName,
		String entityName,
		String entityId,
		String action,
		Object oldValue,
		Object newValue) {
}
