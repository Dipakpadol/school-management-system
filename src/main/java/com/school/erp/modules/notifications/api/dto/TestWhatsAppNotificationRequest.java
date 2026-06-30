package com.school.erp.modules.notifications.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TestWhatsAppNotificationRequest(
		@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String mobileNumber,
		@NotBlank @Size(max = 1000) String message) {
}
