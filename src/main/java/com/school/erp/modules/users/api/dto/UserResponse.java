package com.school.erp.modules.users.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.users.domain.UserStatus;

public record UserResponse(
		UUID id,
		String email,
		String username,
		String firstName,
		String lastName,
		String displayName,
		String phoneNumber,
		UserStatus status,
		List<RoleResponse> roles,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt) {
}
