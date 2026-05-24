package com.school.erp.modules.auth.api.dto;

import java.util.List;
import java.util.UUID;

public record UserSessionResponse(
		UUID id,
		String email,
		String displayName,
		List<String> roles,
		List<String> permissions) {
}
