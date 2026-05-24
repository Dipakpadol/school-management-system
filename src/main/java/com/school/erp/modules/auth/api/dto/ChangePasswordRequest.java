package com.school.erp.modules.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
		@NotBlank @Size(min = 8, max = 128) String currentPassword,
		@NotBlank @Size(min = 8, max = 128) String newPassword,
		@NotBlank @Size(min = 8, max = 128) String confirmPassword) {
}
