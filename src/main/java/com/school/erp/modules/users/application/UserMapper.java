package com.school.erp.modules.users.application;

import java.util.Comparator;
import java.util.List;

import com.school.erp.modules.users.api.dto.PermissionResponse;
import com.school.erp.modules.users.api.dto.RolePermissionMatrixResponse;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.UserResponse;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.UserAccount;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public UserResponse toResponse(UserAccount user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				user.getFirstName(),
				user.getMiddleName(),
				user.getLastName(),
				user.getDisplayName(),
				user.getPhoneNumber(),
				user.getStatus(),
				user.getSource(),
				user.getRoles().stream()
						.sorted(Comparator.comparing(Role::getName))
						.map(this::toRoleResponse)
						.toList(),
				user.getLastLoginAt(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

	public RoleResponse toRoleResponse(Role role) {
		return new RoleResponse(
				role.getId(),
				role.getName(),
				role.getName(),
				role.getDisplayName(),
				role.getDescription(),
				role.getStatus(),
				role.isSystemRole(),
				role.getPermissions().stream()
						.sorted(Comparator.comparing(Permission::getCode))
						.map(permission -> toPermissionResponse(permission, true))
						.toList());
	}

	public PermissionResponse toPermissionResponse(Permission permission, boolean assigned) {
		return new PermissionResponse(
				permission.getId(),
				permission.getCode(),
				permission.getName(),
				permission.getModuleName(),
				permission.getDescription(),
				permission.getStatus().name(),
				assigned);
	}

	public RolePermissionMatrixResponse toRolePermissionMatrixResponse(
			Role role,
			List<PermissionResponse> permissions) {
		return new RolePermissionMatrixResponse(
				role.getId(),
				role.getName(),
				role.getDisplayName(),
				role.getDescription(),
				role.getStatus(),
				role.isSystemRole(),
				permissions);
	}
}
