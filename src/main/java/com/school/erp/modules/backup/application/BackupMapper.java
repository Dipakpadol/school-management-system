package com.school.erp.modules.backup.application;

import com.school.erp.modules.backup.api.dto.BackupRecordResponse;
import com.school.erp.modules.backup.api.dto.RestoreHistoryResponse;
import com.school.erp.modules.backup.domain.BackupRecord;
import com.school.erp.modules.backup.domain.RestoreHistory;

import org.springframework.stereotype.Component;

@Component
public class BackupMapper {

	public BackupRecordResponse toResponse(BackupRecord backup) {
		if (backup == null) {
			return null;
		}
		return new BackupRecordResponse(
				backup.getId(),
				backup.getBackupType(),
				backup.getStatus(),
				backup.getFileName(),
				backup.getStartedAt(),
				backup.getCompletedAt(),
				backup.getSizeBytes(),
				backup.getCreatedByUser(),
				backup.getDatabaseVersion(),
				backup.getApplicationVersion(),
				backup.getChecksumSha256(),
				backup.getErrorMessage(),
				backup.getNotes(),
				backup.isPreRestoreSafety(),
				backup.getSourceBackup() == null ? null : backup.getSourceBackup().getId());
	}

	public RestoreHistoryResponse toResponse(RestoreHistory restore) {
		if (restore == null) {
			return null;
		}
		return new RestoreHistoryResponse(
				restore.getId(),
				restore.getBackup().getId(),
				restore.getBackup().getFileName(),
				restore.getSafetyBackup() == null ? null : restore.getSafetyBackup().getId(),
				restore.getSafetyBackup() == null ? null : restore.getSafetyBackup().getFileName(),
				restore.getStatus(),
				restore.getStartedAt(),
				restore.getCompletedAt(),
				restore.getInitiatedBy(),
				restore.getErrorMessage(),
				restore.getNotes());
	}
}
