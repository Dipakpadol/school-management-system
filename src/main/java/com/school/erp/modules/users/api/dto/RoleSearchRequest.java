package com.school.erp.modules.users.api.dto;

import com.school.erp.modules.users.domain.RoleStatus;

public record RoleSearchRequest(
		String query,
		RoleStatus status) {
}
