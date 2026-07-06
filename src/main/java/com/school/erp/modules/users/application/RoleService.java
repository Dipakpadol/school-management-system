package com.school.erp.modules.users.application;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.users.api.dto.PermissionResponse;
import com.school.erp.modules.users.api.dto.RolePermissionMatrixResponse;
import com.school.erp.modules.users.api.dto.RoleRequest;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.RoleSearchRequest;
import com.school.erp.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.infrastructure.PermissionRepository;
import com.school.erp.modules.users.infrastructure.RoleRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoleService {

	private static final String MODULE_NAME = "USERS";
	private static final String ROLE_ENTITY_NAME = "Role";

	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final UserMapper userMapper;
	private final AuditLogService auditLogService;

	@Transactional
	public RoleResponse create(RoleRequest request) {
		String roleName = normalizeAndValidateRoleName(request.roleName());
		if (roleRepository.existsByNameIgnoreCaseAndDeletedFalse(roleName)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Role name already exists: " + roleName);
		}
		Role role = new Role(roleName, request.displayName().trim(), trimToNull(request.description()));
		role.updateDetails(request.displayName().trim(), trimToNull(request.description()), request.status());
		role.replacePermissions(resolvePermissions(new LinkedHashSet<>(request.permissionIds())));
		Role saved = roleRepository.save(role);
		RoleResponse response = userMapper.toRoleResponse(saved);
		audit(saved.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public RoleResponse update(UUID roleId, RoleRequest request) {
		Role role = load(roleId);
		validateSystemRoleMutation(role);
		RoleResponse oldValue = userMapper.toRoleResponse(role);

		String roleName = normalizeAndValidateRoleName(request.roleName());
		if (role.isSystemRole() && !role.getName().equals(roleName)) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "System role names cannot be changed.");
		}
		if (!role.getName().equals(roleName) && roleRepository.existsByNameIgnoreCaseAndDeletedFalse(roleName)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Role name already exists: " + roleName);
		}

		if (!role.isSystemRole()) {
			role.rename(roleName);
		}
		role.updateDetails(request.displayName().trim(), trimToNull(request.description()), request.status());
		role.replacePermissions(resolvePermissions(new LinkedHashSet<>(request.permissionIds())));

		RoleResponse response = userMapper.toRoleResponse(roleRepository.save(role));
		audit(roleId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public RoleResponse get(UUID roleId) {
		return userMapper.toRoleResponse(load(roleId));
	}

	@Transactional(readOnly = true)
	public List<RoleResponse> list(RoleSearchRequest request) {
		String query = request == null ? null : request.query();
		var status = request == null ? null : request.status();
		String normalizedQuery = StringUtils.hasText(query) ? query.trim().toLowerCase() : null;
		return roleRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.filter(role -> status == null || role.getStatus() == status)
				.filter(role -> normalizedQuery == null
						|| role.getName().toLowerCase().contains(normalizedQuery)
						|| role.getDisplayName().toLowerCase().contains(normalizedQuery)
						|| (role.getDescription() != null && role.getDescription().toLowerCase().contains(normalizedQuery)))
				.map(userMapper::toRoleResponse)
				.sorted(Comparator.comparing(RoleResponse::name))
				.toList();
	}

	@Transactional
	public void delete(UUID roleId) {
		Role role = load(roleId);
		if (role.isSystemRole()) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "System roles cannot be deleted.");
		}
		RoleResponse oldValue = userMapper.toRoleResponse(role);
		role.softDelete(currentActor());
		roleRepository.save(role);
		audit(roleId, "DELETE", oldValue, Map.of("deleted", true, "roleId", roleId));
	}

	@Transactional(readOnly = true)
	public List<PermissionResponse> permissions() {
		return permissionRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.sorted(Comparator.comparing(Permission::getCode))
				.map(permission -> userMapper.toPermissionResponse(permission, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public RolePermissionMatrixResponse getRolePermissions(UUID roleId) {
		Role role = load(roleId);
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
		Role role = load(roleId);
		validateSystemRoleMutation(role);
		List<String> oldPermissionCodes = permissionCodes(role.getPermissions());
		role.replacePermissions(resolvePermissions(new LinkedHashSet<>(request.permissionIds())));
		roleRepository.save(role);
		List<String> newPermissionCodes = permissionCodes(role.getPermissions());
		audit(roleId, "PERMISSIONS_UPDATE", Map.of("permissions", oldPermissionCodes), Map.of("permissions", newPermissionCodes));
		return getRolePermissions(roleId);
	}

	private Role load(UUID roleId) {
		return roleRepository.findByIdAndDeletedFalse(roleId)
				.orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
	}

	private Set<Permission> resolvePermissions(Set<UUID> permissionIds) {
		if (permissionIds == null || permissionIds.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "At least one permission is required.");
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
		return permissions.stream()
				.sorted(Comparator.comparing(Permission::getCode))
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	private void validateSystemRoleMutation(Role role) {
		if (role.isSystemRole() && !currentUserHasRole("ROLE_SUPER_ADMIN")) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Only SUPER_ADMIN can edit system roles.");
		}
	}

	private String normalizeAndValidateRoleName(String value) {
		String roleName = Role.normalizeRoleName(value);
		if (!StringUtils.hasText(roleName)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Role name is required.");
		}
		return roleName;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
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

	private void audit(UUID roleId, String action, Object oldValue, Object newValue) {
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
