package com.school.erp.modules.backup.domain;

import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "system_backups")
@SQLRestriction("deleted = false")
public class BackupRecord extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(name = "backup_type", nullable = false, length = 30)
	private BackupType backupType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private BackupStatus status;

	@Column(name = "file_name", nullable = false, length = 240)
	private String fileName;

	@Column(name = "storage_path", nullable = false, length = 700)
	private String storagePath;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "size_bytes")
	private Long sizeBytes;

	@Column(name = "created_by_user", length = 120)
	private String createdByUser;

	@Column(name = "database_version", length = 80)
	private String databaseVersion;

	@Column(name = "application_version", length = 120)
	private String applicationVersion;

	@Column(name = "checksum_sha256", length = 64)
	private String checksumSha256;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(length = 1000)
	private String notes;

	@Column(name = "postgres_tool_version", length = 160)
	private String postgresToolVersion;

	@Column(length = 40)
	private String format;

	@Column(name = "pre_restore_safety", nullable = false)
	private boolean preRestoreSafety;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_backup_id")
	private BackupRecord sourceBackup;

	public BackupRecord(
			BackupType backupType,
			String fileName,
			String storagePath,
			String createdByUser,
			String databaseVersion,
			String applicationVersion,
			String format,
			boolean preRestoreSafety,
			BackupRecord sourceBackup,
			String notes) {
		this.backupType = backupType;
		this.status = BackupStatus.RUNNING;
		this.fileName = trim(fileName);
		this.storagePath = trim(storagePath);
		this.createdByUser = trim(createdByUser);
		this.databaseVersion = trim(databaseVersion);
		this.applicationVersion = trim(applicationVersion);
		this.format = trim(format);
		this.preRestoreSafety = preRestoreSafety;
		this.sourceBackup = sourceBackup;
		this.notes = truncate(trim(notes), 1000);
		this.startedAt = Instant.now();
	}

	public void markCompleted(long sizeBytes, String checksumSha256, String postgresToolVersion) {
		this.status = BackupStatus.COMPLETED;
		this.completedAt = Instant.now();
		this.sizeBytes = sizeBytes;
		this.checksumSha256 = trim(checksumSha256);
		this.postgresToolVersion = truncate(trim(postgresToolVersion), 160);
		this.errorMessage = null;
	}

	public void markFailed(String errorMessage) {
		this.status = BackupStatus.FAILED;
		this.completedAt = Instant.now();
		this.errorMessage = truncate(trim(errorMessage), 1000);
	}

	public void markRestored() {
		this.status = BackupStatus.RESTORED;
		this.completedAt = this.completedAt == null ? Instant.now() : this.completedAt;
	}

	public void markDeleted(String actor) {
		this.status = BackupStatus.DELETED;
		if (this.completedAt == null) {
			this.completedAt = Instant.now();
		}
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
