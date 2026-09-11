package com.school.erp.modules.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.PermissionStatus;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.auth.infrastructure.RefreshTokenRepository;
import com.school.erp.modules.users.infrastructure.PermissionRepository;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceRolePermissionsTest {

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private PermissionRepository permissionRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuditLogService auditLogService;

	private UserService userService;

	@BeforeEach
	void setUp() {
		userService = new UserService(
				userAccountRepository,
				roleRepository,
				permissionRepository,
				refreshTokenRepository,
				passwordEncoder,
				new UserMapper(),
				auditLogService);
	}

	@Test
	void getRolePermissionsMarksAssignedPermissions() {
		Permission readUsers = permission("USERS_READ", "Read users");
		Permission updateUsers = permission("USERS_UPDATE", "Update users");
		Role admin = role(RoleName.ADMIN);
		admin.addPermission(readUsers);

		when(roleRepository.findByIdAndDeletedFalse(admin.getId())).thenReturn(Optional.of(admin));
		when(permissionRepository.findAllByDeletedFalse(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(updateUsers, readUsers)));

		var response = userService.getRolePermissions(admin.getId());

		assertThat(response.roleName()).isEqualTo(RoleName.ADMIN.name());
		assertThat(response.permissions()).extracting("code").containsExactly("USERS_READ", "USERS_UPDATE");
		assertThat(response.permissions())
				.filteredOn(permission -> permission.code().equals("USERS_READ"))
				.first()
				.extracting("assigned")
				.isEqualTo(true);
		assertThat(response.permissions())
				.filteredOn(permission -> permission.code().equals("USERS_UPDATE"))
				.first()
				.extracting("assigned")
				.isEqualTo(false);
	}

	@Test
	void updateRolePermissionsReplacesJoinTablePermissionsAndAuditsChange() {
		Permission readUsers = permission("USERS_READ", "Read users");
		Permission updateUsers = permission("USERS_UPDATE", "Update users");
		Role admin = customRole("CUSTOM_ADMIN");
		admin.addPermission(readUsers);

		when(roleRepository.findByIdAndDeletedFalse(admin.getId())).thenReturn(Optional.of(admin));
		when(permissionRepository.findAllById(any())).thenReturn(List.of(updateUsers));
		when(permissionRepository.findAllByDeletedFalse(any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(readUsers, updateUsers)));

		var response = userService.updateRolePermissions(
				admin.getId(),
				new UpdateRolePermissionsRequest(List.of(updateUsers.getId())));

		assertThat(admin.getPermissions()).containsExactly(updateUsers);
		assertThat(response.permissions())
				.filteredOn(permission -> permission.code().equals("USERS_UPDATE"))
				.first()
				.extracting("assigned")
				.isEqualTo(true);
		verify(roleRepository).save(admin);
		verify(auditLogService).record(any(AuditLogEvent.class));
	}

	@Test
	void updateRolePermissionsRejectsUnknownPermissionIds() {
		UUID missingPermissionId = UUID.randomUUID();
		Role admin = customRole("CUSTOM_ADMIN");

		when(roleRepository.findByIdAndDeletedFalse(admin.getId())).thenReturn(Optional.of(admin));
		when(permissionRepository.findAllById(any())).thenReturn(List.of());

		assertThatThrownBy(() -> userService.updateRolePermissions(
				admin.getId(),
				new UpdateRolePermissionsRequest(List.of(missingPermissionId))))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void updateRolePermissionsRejectsInactivePermissionsOnLegacyEndpoint() {
		Permission inactivePermission = permission("USERS_UPDATE", "Update users");
		inactivePermission.update("USERS_UPDATE", "Update users", "USERS", "Update users", PermissionStatus.INACTIVE);
		Role admin = customRole("CUSTOM_ADMIN");

		when(roleRepository.findByIdAndDeletedFalse(admin.getId())).thenReturn(Optional.of(admin));
		when(permissionRepository.findAllById(any())).thenReturn(List.of(inactivePermission));

		assertThatThrownBy(() -> userService.updateRolePermissions(
				admin.getId(),
				new UpdateRolePermissionsRequest(List.of(inactivePermission.getId()))))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATION);
	}

	@Test
	void assignRoleAcceptsCustomRoleNames() {
		UserAccount user = user();
		Role customRole = customRole("Library Assistant");

		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));
		when(roleRepository.findByNameIgnoreCaseAndDeletedFalse("LIBRARY_ASSISTANT")).thenReturn(Optional.of(customRole));

		var response = userService.assignRole(user.getId(), "Library Assistant");

		assertThat(response.roles()).extracting("roleName").containsExactly("LIBRARY_ASSISTANT");
		verify(auditLogService).record(any(AuditLogEvent.class));
	}

	@Test
	void deactivateRevokesRefreshTokens() {
		UserAccount user = user();
		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		userService.deactivate(user.getId());

		verify(refreshTokenRepository).revokeActiveTokensForUser(any(UUID.class), any(Instant.class));
		assertThat(user.isActive()).isFalse();
	}

	private Role role(RoleName roleName) {
		Role role = new Role(roleName, roleName.name(), roleName.name());
		ReflectionTestUtils.setField(role, "id", UUID.randomUUID());
		return role;
	}

	private Role customRole(String roleName) {
		Role role = new Role(roleName, roleName, roleName);
		ReflectionTestUtils.setField(role, "id", UUID.randomUUID());
		return role;
	}

	private Permission permission(String code, String name) {
		Permission permission = new Permission(code, name, name);
		ReflectionTestUtils.setField(permission, "id", UUID.randomUUID());
		return permission;
	}

	private UserAccount user() {
		UserAccount user = new UserAccount(
				"admin@school.test",
				"admin",
				"hash",
				"Admin",
				"User");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}
}
