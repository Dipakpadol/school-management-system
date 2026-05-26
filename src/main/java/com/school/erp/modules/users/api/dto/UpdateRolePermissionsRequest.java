package com.school.erp.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateRolePermissionsRequest(
		@NotNull
		List<@NotNull UUID> permissionIds) {
}
