package com.school.erp.common.audit.application;

import com.school.erp.common.audit.api.dto.AuditLogDto;
import com.school.erp.common.audit.domain.AuditLog;

import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

	public AuditLogDto toDto(AuditLog auditLog) {
		return new AuditLogDto(
				auditLog.getId(),
				auditLog.getModuleName(),
				auditLog.getEntityName(),
				auditLog.getEntityId(),
				auditLog.getAction(),
				auditLog.getOldValue(),
				auditLog.getNewValue(),
				auditLog.getPerformedBy(),
				auditLog.getPerformedAt(),
				auditLog.getIpAddress(),
				auditLog.getCreatedAt());
	}
}
