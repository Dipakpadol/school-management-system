package com.school.erp.modules.auth.api.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
		UUID id,
		String email,
		String username,
		List<String> roles,
		List<String> permissions) {
}
