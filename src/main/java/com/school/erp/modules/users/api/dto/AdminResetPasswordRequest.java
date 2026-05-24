package com.school.erp.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminResetPasswordRequest(
		@NotBlank @Size(min = 8, max = 72) String newPassword,
		@NotBlank @Size(min = 8, max = 72) String confirmPassword) {
}
