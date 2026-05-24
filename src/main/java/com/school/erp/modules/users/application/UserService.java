package com.school.erp.modules.users.application;

import java.time.Instant;
import java.util.LinkedHashSet;
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
import com.school.erp.modules.users.api.dto.AdminResetPasswordRequest;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.UserCreateRequest;
import com.school.erp.modules.users.api.dto.UserResponse;
import com.school.erp.modules.users.api.dto.UserSearchRequest;
import com.school.erp.modules.users.api.dto.UserUpdateRequest;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;
import com.school.erp.modules.users.infrastructure.UserAccountSpecifications;

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

	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;
	private final AuditLogService auditLogService;

	@Transactional
	public UserResponse create(UserCreateRequest request) {
		validateUnique(request.email(), request.username(), request.phoneNumber(), null);
		validateSuperAdminRole(request.roles());
		Set<Role> roles = resolveRoles(request.roles());
		UserAccount user = new UserAccount(
				request.email(),
				request.username(),
				passwordEncoder.encode(request.password()),
				request.firstName(),
				request.lastName());
		user.updateProfile(request.email(), request.username(), request.firstName(), request.lastName(), request.phoneNumber());
		user.replaceRoles(roles);
		UserResponse response = userMapper.toResponse(userAccountRepository.save(user));
		audit(response.id(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public UserResponse update(UUID userId, UserUpdateRequest request) {
		UserAccount user = load(userId);
		UserResponse oldValue = userMapper.toResponse(user);
		validateUnique(request.email(), request.username(), request.phoneNumber(), userId);
		validateSuperAdminRole(request.roles());
		user.updateProfile(request.email(), request.username(), request.firstName(), request.lastName(), request.phoneNumber());
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
				.sorted(java.util.Comparator.comparing(role -> role.name().name()))
				.toList();
	}

	@Transactional
	public UserResponse activate(UUID userId) {
		UserAccount user = load(userId);
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
		UserResponse oldValue = userMapper.toResponse(user);
		user.deactivate();
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "STATUS_CHANGE", oldValue, response);
		return response;
	}

	@Transactional
	public UserResponse assignRole(UUID userId, RoleName roleName) {
		validateSuperAdminRole(Set.of(roleName));
		UserAccount user = load(userId);
		UserResponse oldValue = userMapper.toResponse(user);
		user.addRole(resolveRole(roleName));
		UserResponse response = userMapper.toResponse(user);
		audit(userId, "ROLE_ASSIGNED", oldValue, response);
		return response;
	}

	@Transactional
	public UserResponse removeRole(UUID userId, RoleName roleName) {
		preventSelfMutation(userId, "Cannot remove roles from your own user.");
		UserAccount user = load(userId);
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
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		audit(userId, "PASSWORD_RESET", null, Map.of("resetAt", Instant.now(), "userId", userId));
	}

	@Transactional
	public void delete(UUID userId) {
		preventSelfMutation(userId, "Cannot delete your own user.");
		UserAccount user = load(userId);
		UserResponse oldValue = userMapper.toResponse(user);
		user.softDelete(currentActor());
		audit(userId, "DELETE", oldValue, Map.of("deleted", true, "userId", userId));
	}

	private UserAccount load(UUID userId) {
		return userAccountRepository.findWithRolesByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));
	}

	private Set<Role> resolveRoles(Set<RoleName> roleNames) {
		if (roleNames == null || roleNames.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "At least one role is required.");
		}
		Set<Role> roles = new LinkedHashSet<>();
		roleNames.forEach(roleName -> roles.add(resolveRole(roleName)));
		return roles;
	}

	private Role resolveRole(RoleName roleName) {
		return roleRepository.findByNameAndDeletedFalse(roleName)
				.orElseThrow(() -> new ResourceNotFoundException("Role", roleName));
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

	private void validateSuperAdminRole(Set<RoleName> roles) {
		if (roles != null && roles.contains(RoleName.SUPER_ADMIN) && !currentUserHasRole("ROLE_SUPER_ADMIN")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Only SUPER_ADMIN can assign SUPER_ADMIN role.");
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
}
