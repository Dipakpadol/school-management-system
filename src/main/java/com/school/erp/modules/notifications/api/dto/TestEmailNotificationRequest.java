package com.school.erp.modules.notifications.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestEmailNotificationRequest(
		@NotBlank @Email @Size(max = 180) String to,
		@NotBlank @Size(max = 180) String subject,
		@NotBlank @Size(max = 2000) String message) {
}
