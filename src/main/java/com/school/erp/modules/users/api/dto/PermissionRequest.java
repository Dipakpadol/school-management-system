package com.school.erp.modules.users.api.dto;

import com.school.erp.modules.users.domain.PermissionStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Permission create/update request.")
public record PermissionRequest(
		@NotBlank @Size(max = 120) @Schema(example = "FEES_READ") String permissionCode,
		@NotBlank @Size(max = 160) @Schema(example = "Read fees") String permissionName,
		@NotBlank @Size(max = 80) @Schema(example = "FEES") String moduleName,
		@Size(max = 500) String description,
		PermissionStatus status) {
}
