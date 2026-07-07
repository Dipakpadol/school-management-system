package com.school.erp.modules.users.domain;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "permissions")
@SQLRestriction("deleted = false")
public class Permission extends BaseEntity {

	@Column(nullable = false, unique = true, length = 120)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "module_name", nullable = false, length = 80)
	private String moduleName;

	@Column(length = 500)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private PermissionStatus status = PermissionStatus.ACTIVE;

	public Permission(String code, String name, String description) {
		this(code, name, moduleFromCode(code), description, PermissionStatus.ACTIVE);
	}

	public Permission(String code, String name, String moduleName, String description, PermissionStatus status) {
		update(code, name, moduleName, description, status);
	}

	public void update(String code, String name, String moduleName, String description, PermissionStatus status) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.moduleName = normalizeModule(moduleName);
		this.description = trimToNull(description);
		this.status = status == null ? PermissionStatus.ACTIVE : status;
	}

	public boolean isActive() {
		return status == PermissionStatus.ACTIVE;
	}

	private static String moduleFromCode(String code) {
		if (!StringUtils.hasText(code)) {
			return null;
		}
		int separator = code.indexOf('_');
		return separator > 0 ? code.substring(0, separator) : code;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String normalizeModule(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
