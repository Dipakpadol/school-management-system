package com.school.erp.modules.notifications.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationTemplateStatus;

public record NotificationTemplateResponse(
		UUID id,
		String templateCode,
		String templateName,
		NotificationChannel channel,
		String subject,
		String body,
		String variables,
		NotificationTemplateStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
