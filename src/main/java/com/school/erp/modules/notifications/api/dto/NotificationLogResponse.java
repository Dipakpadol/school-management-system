package com.school.erp.modules.notifications.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationStatus;

public record NotificationLogResponse(
		UUID id,
		NotificationChannel channel,
		String recipient,
		String subject,
		String message,
		NotificationStatus status,
		String provider,
		String providerReference,
		String providerResponse,
		String errorMessage,
		String referenceType,
		String referenceId,
		Instant sentAt,
		Instant failedAt,
		int retryCount,
		Instant createdAt) {
}
