package com.school.erp.modules.notifications.domain;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_logs")
@SQLRestriction("deleted = false")
public class NotificationLog extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationChannel channel;

	@Column(nullable = false, length = 180)
	private String recipient;

	@Column(length = 180)
	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationStatus status = NotificationStatus.PENDING;

	@Column(name = "provider_response", columnDefinition = "TEXT")
	private String providerResponse;

	@Column(length = 80)
	private String provider;

	@Column(name = "provider_reference", length = 180)
	private String providerReference;

	@Column(name = "error_message", columnDefinition = "TEXT")
	private String errorMessage;

	@Column(name = "reference_type", length = 80)
	private String referenceType;

	@Column(name = "reference_id", length = 120)
	private String referenceId;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "failed_at")
	private Instant failedAt;

	@Column(name = "retry_count", nullable = false)
	private int retryCount;

	public NotificationLog(NotificationChannel channel, String recipient, String subject, String message) {
		this(channel, recipient, subject, message, null, null);
	}

	public NotificationLog(
			NotificationChannel channel,
			String recipient,
			String subject,
			String message,
			String referenceType,
			UUID referenceId) {
		this.channel = channel;
		this.recipient = recipient.trim();
		this.subject = trimToNull(subject);
		this.message = message.trim();
		this.referenceType = normalizeReferenceType(referenceType);
		this.referenceId = referenceId == null ? null : referenceId.toString();
	}

	public void markSent(String provider, String providerReference, String providerResponse) {
		this.status = NotificationStatus.SENT;
		this.provider = trimToNull(provider);
		this.providerReference = trimToNull(providerReference);
		this.providerResponse = trimToNull(providerResponse);
		this.errorMessage = null;
		this.sentAt = Instant.now();
		this.failedAt = null;
	}

	public void markFailed(String provider, String errorMessage) {
		this.status = NotificationStatus.FAILED;
		this.provider = trimToNull(provider);
		this.errorMessage = trimToNull(errorMessage);
		this.sentAt = null;
		this.failedAt = Instant.now();
	}

	public void markSkipped(String reason) {
		this.status = NotificationStatus.SKIPPED;
		this.errorMessage = trimToNull(reason);
		this.sentAt = null;
		this.failedAt = null;
	}

	public void markRetryPending(String errorMessage) {
		this.status = NotificationStatus.RETRY_PENDING;
		this.retryCount++;
		this.errorMessage = trimToNull(errorMessage);
	}

	private String trimToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}

	private String normalizeReferenceType(String value) {
		String trimmed = trimToNull(value);
		return trimmed == null ? null : trimmed.toUpperCase();
	}
}
