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
import com.school.erp.modules.users.api.dto.PermissionRequest;
import com.school.erp.modules.users.api.dto.PermissionSearchRequest;
import com.school.erp.modules.users.api.dto.RolePermissionMatrixResponse;
import com.school.erp.modules.users.api.dto.RoleRequest;
import com.school.erp.modules.users.api.dto.RoleResponse;
import com.school.erp.modules.users.api.dto.RoleSearchRequest;
import com.school.erp.modules.users.api.dto.UpdateRolePermissionsRequest;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.PermissionStatus;
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
	private static final String PERMISSION_ENTITY_NAME = "Permission";

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
		return permissions(null);
	}

	@Transactional(readOnly = true)
	public List<PermissionResponse> permissions(PermissionSearchRequest request) {
		String query = request == null ? null : trimToNull(request.query());
		String moduleName = request == null ? null : trimToNull(request.moduleName());
		PermissionStatus status = request == null ? null : request.status();
		String normalizedQuery = query == null ? null : query.toLowerCase();
		String normalizedModule = moduleName == null ? null : moduleName.toUpperCase();
		return permissionRepository.findAllByDeletedFalse(Pageable.unpaged()).stream()
				.filter(permission -> status == null || permission.getStatus() == status)
				.filter(permission -> normalizedModule == null || permission.getModuleName().equalsIgnoreCase(normalizedModule))
				.filter(permission -> normalizedQuery == null
						|| permission.getCode().toLowerCase().contains(normalizedQuery)
						|| permission.getName().toLowerCase().contains(normalizedQuery)
						|| permission.getModuleName().toLowerCase().contains(normalizedQuery)
						|| (permission.getDescription() != null && permission.getDescription().toLowerCase().contains(normalizedQuery)))
				.sorted(Comparator.comparing(Permission::getCode))
				.map(permission -> userMapper.toPermissionResponse(permission, false))
				.toList();
	}

	@Transactional
	public PermissionResponse createPermission(PermissionRequest request) {
		String code = normalizePermissionCode(request.permissionCode());
		if (permissionRepository.existsByCodeIgnoreCaseAndDeletedFalse(code)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Permission code already exists: " + code);
		}
		Permission permission = permissionRepository.save(new Permission(
				code,
				request.permissionName().trim(),
				normalizeModuleName(request.moduleName()),
				trimToNull(request.description()),
				request.status()));
		PermissionResponse response = userMapper.toPermissionResponse(permission, false);
		auditPermission(permission.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public PermissionResponse updatePermission(UUID permissionId, PermissionRequest request) {
		Permission permission = loadPermission(permissionId);
		PermissionResponse oldValue = userMapper.toPermissionResponse(permission, false);
		String code = normalizePermissionCode(request.permissionCode());
		permissionRepository.findByCodeIgnoreCaseAndDeletedFalse(code)
				.filter(existing -> !existing.getId().equals(permissionId))
				.ifPresent(existing -> {
					throw new BusinessException(ErrorCode.CONFLICT, "Permission code already exists: " + code);
				});
		permission.update(
				code,
				request.permissionName().trim(),
				normalizeModuleName(request.moduleName()),
				trimToNull(request.description()),
				request.status());
		PermissionResponse response = userMapper.toPermissionResponse(permission, false);
		auditPermission(permissionId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PermissionResponse getPermission(UUID permissionId) {
		return userMapper.toPermissionResponse(loadPermission(permissionId), false);
	}

	@Transactional
	public void deletePermission(UUID permissionId) {
		Permission permission = loadPermission(permissionId);
		if (permissionRepository.countActiveRoleAssignments(permissionId) > 0) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Permission is assigned to an active role.");
		}
		PermissionResponse oldValue = userMapper.toPermissionResponse(permission, false);
		permission.softDelete(currentActor());
		permissionRepository.save(permission);
		auditPermission(permissionId, "DELETE", oldValue, Map.of("deleted", true, "permissionId", permissionId));
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

	private Permission loadPermission(UUID permissionId) {
		return permissionRepository.findByIdAndDeletedFalse(permissionId)
				.orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
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

	private String normalizePermissionCode(String value) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Permission code is required.");
		}
		return value.trim().replaceAll("\\s+", "_").toUpperCase();
	}

	private String normalizeModuleName(String value) {
		if (!StringUtils.hasText(value)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Module name is required.");
		}
		return value.trim().toUpperCase();
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

	private void auditPermission(UUID permissionId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				PERMISSION_ENTITY_NAME,
				permissionId == null ? null : permissionId.toString(),
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
