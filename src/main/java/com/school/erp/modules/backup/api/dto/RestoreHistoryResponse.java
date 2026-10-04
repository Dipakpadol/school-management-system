package com.school.erp.modules.backup.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.backup.domain.RestoreStatus;

public record RestoreHistoryResponse(
		UUID id,
		UUID backupId,
		String backupFileName,
		UUID safetyBackupId,
		String safetyBackupFileName,
		RestoreStatus status,
		Instant startedAt,
		Instant completedAt,
		String initiatedBy,
		String errorMessage,
		String notes) {
}
