package com.school.erp.modules.users.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.auth.infrastructure.RefreshTokenRepository;
import com.school.erp.modules.users.api.dto.AdminResetPasswordRequest;
import com.school.erp.modules.users.api.dto.PermissionResponse;
import com.school.erp.modules.users.api.dto.RolePermissionMatrixResponse;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.school.erp.modules.users.api.dto.UserCreateRequest;
import com.school.erp.modules.users.api.dto.UserResponse;
import com.school.erp.modules.users.api.dto.UserSearchRequest;
import com.school.erp.modules.users.api.dto.UserUpdateRequest;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.PermissionRepository;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;
import com.school.erp.modules.users.infrastructure.UserAccountSpecifications;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final String MODULE_NAME = "USERS";
	private static final String ENTITY_NAME = "UserAccount";
	private static final String ROLE_ENTITY_NAME = "Role";
	private static final Set<String> DOMAIN_MANAGED_ROLES = Set.of(RoleName.STUDENT.name(), RoleName.TEACHER.name());

	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuditLogService auditLogService;

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		validateUnique(request.email(), request.username(), request.phoneNumber(), null);
		validateSuperAdminRole(request.roles());
		validateNoDomainManagedRoles(request.roles());
		Set<Role> roles = resolveRoles(request.roles());
		UserAccount user = new UserAccount(
				request.email(),
				request.username(),
				passwordEncoder.encode(request.password()),
				request.firstName(),
				request.lastName());
		user.updateProfile(
				request.email(),
				request.username(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.phoneNumber());
		user.replaceRoles(roles);
		UserResponse response = userMapper.toResponse(userAccountRepository.save(user));
		audit(response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public UserResponse update(UUID userId, UserUpdateRequest request) {
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can update SUPER_ADMIN users.");
		UserResponse oldValue = userMapper.toResponse(user);
		validateUnique(request.email(), request.username(), request.phoneNumber(), userId);
		validateSuperAdminRole(request.roles());
		validateDomainManagedRoleUpdate(user, request.roles());
		user.updateProfile(
				request.email(),
				request.username(),
				request.firstName(),
				request.middleName(),
				request.lastName(),
				request.phoneNumber());
		user.replaceRoles(resolveRoles(request.roles()));
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public UserResponse get(UUID userId) {
		return userMapper.toResponse(load(userId));
	}

	@Transactional(readOnly = true)
	public PageResponse<UserResponse> search(UserSearchRequest request, PageRequestDto pageRequest) {
		return PageResponse.from(
				userAccountRepository.findAll(UserAccountSpecifications.matching(request), pageRequest.toPageable("email")),
				userMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public java.util.List<RoleResponse> listRoles() {
		return roleRepository.findAllByDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).stream()
				.map(userMapper::toRoleResponse)
				.sorted(java.util.Comparator.comparing(RoleResponse::name))
				.toList();
	}

	@Transactional(readOnly = true)
	public RolePermissionMatrixResponse getRolePermissions(UUID roleId) {
		Role role = loadRole(roleId);
		Set<UUID> assignedPermissionIds = role.getPermissions().stream()
				.map(Permission::getId)
				.collect(java.util.stream.Collectors.toSet());
		List<PermissionResponse> permissions = permissionRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.sorted(Comparator.comparing(Permission::getCode))
				.map(permission -> userMapper.toPermissionResponse(
						permission,
						assignedPermissionIds.contains(permission.getId())))
				.toList();
		return userMapper.toRolePermissionMatrixResponse(role, permissions);
	}

	@Transactional
	public RolePermissionMatrixResponse updateRolePermissions(UUID roleId, UpdateRolePermissionsRequest request) {
		Role role = loadRole(roleId);
		validateSuperAdminPermissionMutation(role);
		List<String> oldPermissionCodes = permissionCodes(role.getPermissions());
		Set<Permission> permissions = resolvePermissions(new LinkedHashSet<>(request.permissionIds()));

		role.replacePermissions(permissions);
		roleRepository.save(role);

		List<String> newPermissionCodes = permissionCodes(role.getPermissions());
		auditRole(role.getId(), "UPDATE", Map.of("permissions", oldPermissionCodes), Map.of("permissions", newPermissionCodes));
		return getRolePermissions(role.getId());
	}

	@Transactional
	public UserResponse activate(UUID userId) {
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can activate SUPER_ADMIN users.");
		UserResponse oldValue = userMapper.toResponse(user);
		user.activate();
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "STATUS_CHANGE", oldValue, response);
		return response;
	}

	@Transactional
	public UserResponse deactivate(UUID userId) {
		preventSelfMutation(userId, "Cannot deactivate your own user.");
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can deactivate SUPER_ADMIN users.");
		UserResponse oldValue = userMapper.toResponse(user);
		user.deactivate();
		refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "STATUS_CHANGE", oldValue, response);
		return response;
	}

	@Transactional
	public UserResponse assignRole(UUID userId, String roleName) {
		validateSuperAdminRole(java.util.Collections.singleton(roleName));
		validateNoDomainManagedRoles(java.util.Collections.singleton(roleName));
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can update SUPER_ADMIN users.");
		UserResponse oldValue = userMapper.toResponse(user);
		user.addRole(resolveRole(roleName));
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "ROLE_ASSIGNED", oldValue, response);
		return response;
	}

	@Transactional
	public UserResponse removeRole(UUID userId, String roleName) {
		preventSelfMutation(userId, "Cannot remove roles from your own user.");
		validateSuperAdminRole(java.util.Collections.singleton(roleName));
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can update SUPER_ADMIN users.");
		if (user.getRoles().size() <= 1) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "At least one role is required.");
		}
		UserResponse oldValue = userMapper.toResponse(user);
		user.removeRole(roleName);
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "ROLE_REMOVED", oldValue, response);
		return response;
	}

	@Transactional
	public void resetPassword(UUID userId, AdminResetPasswordRequest request) {
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_POLICY_VIOLATION, "Password confirmation does not match.");
		}
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can reset SUPER_ADMIN passwords.");
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
		audit(userId, "PASSWORD_RESET", null, Map.of("resetAt", Instant.now(), "userId", userId));
	}

	@Transactional
	public void delete(UUID userId) {
		preventSelfMutation(userId, "Cannot delete your own user.");
		UserAccount user = load(userId);
		validateSuperAdminTargetMutation(user, "Only SUPER_ADMIN can delete SUPER_ADMIN users.");
		UserResponse oldValue = userMapper.toResponse(user);
		user.softDelete(currentActor());
		refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
		audit(userId, "DELETE", oldValue, Map.of("deleted", true, "userId", userId));
	}

	private UserAccount load(UUID userId) {
		return userAccountRepository.findWithRolesByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

	private Role loadRole(UUID roleId) {
		return roleRepository.findByIdAndDeletedFalse(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
	}

	private Set<Permission> resolvePermissions(Set<UUID> permissionIds) {
		if (permissionIds.isEmpty()) {
			return new LinkedHashSet<>();
		}
		List<Permission> permissions = permissionRepository.findAllById(permissionIds);
		Set<UUID> foundIds = permissions.stream()
				.map(Permission::getId)
				.collect(java.util.stream.Collectors.toSet());
		List<UUID> missingIds = permissionIds.stream()
				.filter(id -> !foundIds.contains(id))
				.toList();
		if (!missingIds.isEmpty()) {
			throw new ResourceNotFoundException("Permission", missingIds);
		}
		permissions.stream()
				.filter(permission -> !permission.isActive())
				.findFirst()
				.ifPresent(permission -> {
					throw new BusinessException(
							ErrorCode.BUSINESS_RULE_VIOLATION,
							"Inactive permission cannot be assigned: " + permission.getCode());
				});
		return permissions.stream()
				.sorted(Comparator.comparing(Permission::getCode))
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<Role> resolveRoles(Set<String> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "At least one role is required.");
		}
		Set<Role> roles = new LinkedHashSet<>();
		roleNames.forEach(roleName -> roles.add(resolveRole(roleName)));
		return roles;
	}

	private Role resolveRole(String roleName) {
		String normalizedRoleName = Role.normalizeRoleName(roleName);
		if (!StringUtils.hasText(normalizedRoleName)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role is required.");
		}
		Role role = roleRepository.findByNameIgnoreCaseAndDeletedFalse(normalizedRoleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role", normalizedRoleName));
		if (!role.isActive()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Inactive role cannot be assigned: " + role.getName());
		}
		return role;
	}

	private void validateUnique(String email, String username, String phoneNumber, UUID existingUserId) {
		userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
				.filter(user -> !user.getId().equals(existingUserId))
				.ifPresent(user -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Email already exists: " + email);
				});
		if (userAccountRepository.existsByUsernameIgnoreCaseAndDeletedFalse(username)) {
			userAccountRepository.findAll(UserAccountSpecifications.matching(new UserSearchRequest(username, null, null)))
					.stream()
					.filter(user -> user.getUsername().equalsIgnoreCase(username))
					.filter(user -> !user.getId().equals(existingUserId))
					.findFirst()
					.ifPresent(user -> {
						throw new BusinessException(ErrorCode.CONFLICT, "Username already exists: " + username);
					});
		}
		if (StringUtils.hasText(phoneNumber) && userAccountRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber.trim())) {
			userAccountRepository.findAll(UserAccountSpecifications.matching(new UserSearchRequest(phoneNumber, null, null)))
					.stream()
					.filter(user -> phoneNumber.trim().equals(user.getPhoneNumber()))
					.filter(user -> !user.getId().equals(existingUserId))
					.findFirst()
					.ifPresent(user -> {
						throw new BusinessException(ErrorCode.CONFLICT, "Mobile number already exists: " + phoneNumber);
					});
		}
	}

	private void validateSuperAdminRole(Set<String> roles) {
		if (normalizedRoles(roles).contains(RoleName.SUPER_ADMIN.name()) && !currentUserHasRole("ROLE_SUPER_ADMIN")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Only SUPER_ADMIN can assign SUPER_ADMIN role.");
		}
	}

	private void validateNoDomainManagedRoles(Set<String> roles) {
		if (normalizedRoles(roles).stream().noneMatch(DOMAIN_MANAGED_ROLES::contains)) {
			return;
		}
		throw new BusinessException(
				ErrorCode.BUSINESS_RULE_VIOLATION,
				"Student and Teacher user accounts must be initiated from Student Management or Teacher Management.");
	}

	private void validateDomainManagedRoleUpdate(UserAccount user, Set<String> requestedRoles) {
		Set<String> normalizedRequestedRoles = normalizedRoles(requestedRoles);
		if (normalizedRequestedRoles.stream().noneMatch(DOMAIN_MANAGED_ROLES::contains)) {
			return;
		}
		Set<String> existingRoles = user.getRoles().stream()
				.map(Role::getName)
				.collect(java.util.stream.Collectors.toSet());
		boolean introducesDomainRole = normalizedRequestedRoles.stream()
				.filter(DOMAIN_MANAGED_ROLES::contains)
				.anyMatch(roleName -> !existingRoles.contains(roleName));
		if (introducesDomainRole) {
			validateNoDomainManagedRoles(requestedRoles);
		}
	}

	private Set<String> normalizedRoles(Set<String> roles) {
		if (roles == null) {
			return Set.of();
		}
		return roles.stream()
				.map(Role::normalizeRoleName)
				.filter(StringUtils::hasText)
				.collect(java.util.stream.Collectors.toSet());
	}

	private void validateSuperAdminTargetMutation(UserAccount user, String message) {
		if (user.hasRole(RoleName.SUPER_ADMIN) && !currentUserHasRole("ROLE_SUPER_ADMIN")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, message);
		}
	}

	private void validateSuperAdminPermissionMutation(Role role) {
		if (role.isSystemRole() && !currentUserHasRole("ROLE_SUPER_ADMIN")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Only SUPER_ADMIN can update system role permissions.");
		}
	}

	private void preventSelfMutation(UUID userId, String message) {
		currentUserId().filter(userId::equals).ifPresent(id -> {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message);
		});
	}

	private java.util.Optional<UUID> currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return java.util.Optional.empty();
		}
		try {
			return java.util.Optional.of(UUID.fromString(authentication.getName()));
		}
		catch (IllegalArgumentException ex) {
			return java.util.Optional.empty();
		}
	}

	private boolean currentUserHasRole(String roleAuthority) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals(roleAuthority));
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void audit(UUID userId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				ENTITY_NAME,
				userId == null ? null : userId.toString(),
				action,
				oldValue,
				newValue));
	}

	private void auditRole(UUID roleId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				ROLE_ENTITY_NAME,
				roleId == null ? null : roleId.toString(),
				action,
				oldValue,
				newValue));
	}

	private List<String> permissionCodes(Set<Permission> permissions) {
		return permissions.stream()
				.map(Permission::getCode)
				.sorted()
				.toList();
	}
}
