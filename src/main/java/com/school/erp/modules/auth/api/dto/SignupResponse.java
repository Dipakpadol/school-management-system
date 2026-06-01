package com.school.erp.modules.auth.api.dto;

import java.util.UUID;

import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserStatus;

public record SignupResponse(
		UUID userId,
		String email,
		RoleName role,
		UserStatus status,
		String message) {
}
