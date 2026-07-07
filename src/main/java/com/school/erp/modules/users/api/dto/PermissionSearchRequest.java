package com.school.erp.modules.users.api.dto;

import com.school.erp.modules.users.domain.PermissionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Permission search filters.")
public record PermissionSearchRequest(
		@Size(max = 120) String query,
		@Size(max = 80) String moduleName,
		PermissionStatus status) {
}
