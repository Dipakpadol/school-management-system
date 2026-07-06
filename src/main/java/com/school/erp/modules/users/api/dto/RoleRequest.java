package com.school.erp.modules.users.api.dto;

import java.util.List;
import java.util.UUID;

import com.school.erp.modules.users.domain.RoleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RoleRequest(
		@NotBlank @Size(max = 60) String roleName,
		@NotBlank @Size(max = 120) String displayName,
		@Size(max = 500) String description,
		RoleStatus status,
		@NotEmpty List<@NotNull UUID> permissionIds) {
}
