package com.school.erp.modules.users.application;

import java.util.Comparator;

import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.UserResponse;
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
				user.getLastName(),
				user.getDisplayName(),
				user.getPhoneNumber(),
				user.getStatus(),
				user.getRoles().stream()
						.sorted(Comparator.comparing(role -> role.getName().name()))
						.map(this::toRoleResponse)
						.toList(),
				user.getLastLoginAt(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

	public RoleResponse toRoleResponse(Role role) {
		return new RoleResponse(role.getId(), role.getName(), role.getDisplayName(), role.getDescription());
	}
}
