package com.school.erp.modules.notifications.api.dto;

import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationTemplateStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationTemplateRequest(
		@NotBlank @Size(max = 100) String templateCode,
		@NotBlank @Size(max = 160) String templateName,
		@NotNull NotificationChannel channel,
		@Size(max = 180) String subject,
		@NotBlank @Size(max = 4000) String body,
		@Size(max = 2000) String variables,
		NotificationTemplateStatus status) {
}
