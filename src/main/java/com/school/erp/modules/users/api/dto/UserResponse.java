package com.school.erp.modules.users.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.domain.UserSource;

public record UserResponse(
		UUID id,
		String email,
		String username,
		String firstName,
		String middleName,
		String lastName,
		String displayName,
		String phoneNumber,
		UserStatus status,
		UserSource source,
		List<RoleResponse> roles,
		Instant lastLoginAt,
		Instant createdAt,
		Instant updatedAt) {
}
