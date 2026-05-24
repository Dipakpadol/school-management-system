package com.school.erp.modules.users.api.dto;

import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserStatus;

public record UserSearchRequest(
		String query,
		RoleName role,
		UserStatus status) {
}
