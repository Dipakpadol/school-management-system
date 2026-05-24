package com.school.erp.common.audit.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "audit_log")
public class AuditLog {

	private static final String SUCCESS_STATUS = "SUCCESS";

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@Column(name = "module_name", nullable = false, length = 80)
	private String moduleName;

	@Column(name = "entity_name", nullable = false, length = 120)
	private String entityName;

	@Column(name = "entity_id", length = 120)
	private String entityId;

	@Column(nullable = false, length = 120)
	private String action;

	@Column(name = "old_value", columnDefinition = "TEXT")
	private String oldValue;

	@Column(name = "new_value", columnDefinition = "TEXT")
	private String newValue;

	@Column(name = "performed_by", nullable = false, length = 120)
	private String performedBy;

	@Column(name = "performed_at", nullable = false)
	private Instant performedAt;

	@Column(name = "ip_address", length = 80)
	private String ipAddress;

	@Column(length = 120)
	private String actor;

	@Column(name = "resource_type", nullable = false, length = 120)
	private String resourceType;

	@Column(name = "resource_id", length = 120)
	private String resourceId;

	@Column(nullable = false, length = 40)
	private String status = SUCCESS_STATUS;

	@Column(columnDefinition = "TEXT")
	private String metadata;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public AuditLog(
			String moduleName,
			String entityName,
			String entityId,
			String action,
			String oldValue,
			String newValue,
			String performedBy,
			Instant performedAt,
			String ipAddress) {
		this.moduleName = normalize(moduleName, "UNKNOWN");
		this.entityName = normalize(entityName, "UNKNOWN");
		this.entityId = trimToNull(entityId);
		this.action = normalize(action, "UNKNOWN");
		this.oldValue = trimToNull(oldValue);
		this.newValue = trimToNull(newValue);
		this.performedBy = normalize(performedBy, "system");
		this.performedAt = performedAt == null ? Instant.now() : performedAt;
		this.ipAddress = trimToNull(ipAddress);
		synchronizeLegacyColumns();
	}

	@PrePersist
	void prePersist() {
		Instant now = Instant.now();
		if (performedAt == null) {
			performedAt = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		synchronizeLegacyColumns();
	}

	private void synchronizeLegacyColumns() {
		actor = performedBy;
		resourceType = entityName;
		resourceId = entityId;
		status = SUCCESS_STATUS;
		metadata = newValue;
	}

	private String normalize(String value, String fallback) {
		String trimmed = trimToNull(value);
		return trimmed == null ? fallback : trimmed;
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
