package com.school.erp.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateRolePermissionsRequest(
		@NotEmpty
		List<@NotNull UUID> permissionIds) {
}
