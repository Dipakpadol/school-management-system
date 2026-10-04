package com.school.erp.modules.backup.api.dto;

public record BackupSummaryResponse(
		BackupRecordResponse lastSuccessfulBackup,
		BackupRecordResponse lastFailedBackup,
		long completedBackupCount,
		long failedBackupCount,
		long totalStorageBytes,
		Long backupAgeHours,
		boolean scheduledBackupsEnabled,
		String scheduleFrequency,
		String scheduleTime,
		int retentionDays,
		int retentionCount,
		boolean storageLocationConfigured,
		boolean pgDumpAvailable,
		boolean pgRestoreAvailable,
		boolean databaseBackupSupported,
		boolean uploadedFileBackupSupported,
		String latestDatabaseVersion) {
}
