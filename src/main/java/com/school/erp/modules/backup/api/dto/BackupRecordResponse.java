package com.school.erp.modules.backup.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.backup.domain.BackupStatus;
import com.school.erp.modules.backup.domain.BackupType;

public record BackupRecordResponse(
		UUID id,
		BackupType backupType,
		BackupStatus status,
		String fileName,
		Instant startedAt,
		Instant completedAt,
		Long sizeBytes,
		String createdByUser,
		String databaseVersion,
		String applicationVersion,
		String checksumSha256,
		String errorMessage,
		String notes,
		boolean preRestoreSafety,
		UUID sourceBackupId) {
}
