package com.school.erp.modules.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRoleUpdateRequest(@NotBlank @Size(max = 60) String role) {
}
