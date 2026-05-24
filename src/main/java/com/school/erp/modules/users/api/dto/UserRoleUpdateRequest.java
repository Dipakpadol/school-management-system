package com.school.erp.modules.users.api.dto;

import com.school.erp.modules.users.domain.RoleName;

import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(@NotNull RoleName role) {
}
