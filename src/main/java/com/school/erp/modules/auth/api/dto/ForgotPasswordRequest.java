package com.school.erp.modules.auth.api.dto;

import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
		@Size(max = 160) String emailOrMobile,
		@Size(max = 160) String email) {
}
