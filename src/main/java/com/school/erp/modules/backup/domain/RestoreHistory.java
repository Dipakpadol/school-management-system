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
@Table(name = "system_restore_history")
@SQLRestriction("deleted = false")
public class RestoreHistory extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "backup_id", nullable = false)
	private BackupRecord backup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "safety_backup_id")
	private BackupRecord safetyBackup;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RestoreStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "initiated_by", length = 120)
	private String initiatedBy;

	@Column(name = "confirmation_accepted", nullable = false)
	private boolean confirmationAccepted;

	@Column(name = "error_message", length = 1000)
	private String errorMessage;

	@Column(length = 1000)
	private String notes;

	public RestoreHistory(BackupRecord backup, String initiatedBy, boolean confirmationAccepted, String notes) {
		this.backup = backup;
		this.status = RestoreStatus.RUNNING;
		this.startedAt = Instant.now();
		this.initiatedBy = trim(initiatedBy);
		this.confirmationAccepted = confirmationAccepted;
		this.notes = truncate(trim(notes), 1000);
	}

	public void attachSafetyBackup(BackupRecord safetyBackup) {
		this.safetyBackup = safetyBackup;
	}

	public void markCompleted() {
		this.status = RestoreStatus.COMPLETED;
		this.completedAt = Instant.now();
		this.errorMessage = null;
	}

	public void markFailed(String errorMessage) {
		this.status = RestoreStatus.FAILED;
		this.completedAt = Instant.now();
		this.errorMessage = truncate(trim(errorMessage), 1000);
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
