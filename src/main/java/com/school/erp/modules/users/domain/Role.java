package com.school.erp.modules.users.domain;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "roles")
@SQLRestriction("deleted = false")
public class Role extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private RoleStatus status = RoleStatus.ACTIVE;

	@Column(nullable = false, unique = true, length = 60)
	private String name;

	@Column(name = "display_name", nullable = false, length = 120)
	private String displayName;

	@Column(length = 500)
	private String description;

	@Column(name = "system_role", nullable = false)
	private boolean systemRole = true;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "role_permissions",
			joinColumns = @JoinColumn(name = "role_id"),
			inverseJoinColumns = @JoinColumn(name = "permission_id"))
	private Set<Permission> permissions = new LinkedHashSet<>();

	public Role(RoleName name, String displayName, String description) {
		this(name.name(), displayName, description, true);
	}

	public Role(String name, String displayName, String description) {
		this(name, displayName, description, false);
	}

	private Role(String name, String displayName, String description, boolean systemRole) {
		this.name = normalizeRoleName(name);
		this.displayName = displayName;
		this.description = description;
		this.systemRole = systemRole;
	}

	public void updateDetails(String displayName, String description, RoleStatus status) {
		this.displayName = displayName;
		this.description = description;
		this.status = status == null ? RoleStatus.ACTIVE : status;
	}

	public void rename(String name) {
		this.name = normalizeRoleName(name);
	}

	public void activate() {
		status = RoleStatus.ACTIVE;
	}

	public void deactivate() {
		status = RoleStatus.INACTIVE;
	}

	public boolean isActive() {
		return status == RoleStatus.ACTIVE;
	}

	public void addPermission(Permission permission) {
		if (permission != null) {
			permissions.add(permission);
		}
	}

	public void replacePermissions(Set<Permission> permissions) {
		this.permissions.clear();
		this.permissions.addAll(permissions);
	}

	public static String normalizeRoleName(String value) {
		return value == null
				? null
				: value.trim().replaceAll("\\s+", "_").toUpperCase(Locale.ROOT);
	}
}
