package com.school.erp.common.modules.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.common.modules.api.dto.ModuleRecordRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "module_records")
@SQLRestriction("deleted = false")
public class ModuleRecord extends BaseEntity {

	@Column(name = "module_name", nullable = false, length = 60)
	private String moduleName;

	@Column(name = "record_type", nullable = false, length = 80)
	private String recordType;

	@Column(nullable = false, length = 80)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 1000)
	private String description;

	@Column(nullable = false, length = 40)
	private String status = "ACTIVE";

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(name = "owner_id")
	private UUID ownerId;

	@Column(name = "record_date")
	private LocalDate recordDate;

	@Column(precision = 14, scale = 2)
	private BigDecimal amount;

	@Column(name = "metadata_json", length = 4000)
	private String metadataJson;

	@Column(nullable = false)
	private boolean active = true;

	public ModuleRecord(String moduleName, String recordType, ModuleRecordRequest request) {
		this.moduleName = normalizeModule(moduleName);
		this.recordType = normalizeType(recordType);
		apply(request);
	}

	public void update(ModuleRecordRequest request) {
		apply(request);
	}

	private void apply(ModuleRecordRequest request) {
		this.code = normalizeCode(request.code());
		this.name = request.name().trim();
		this.description = trimToNull(request.description());
		this.status = StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase() : "ACTIVE";
		this.parentId = request.parentId();
		this.ownerId = request.ownerId();
		this.recordDate = request.recordDate();
		this.amount = request.amount();
		this.metadataJson = trimToNull(request.metadataJson());
		this.active = request.active() == null || request.active();
	}

	public static String normalizeModule(String value) {
		return normalizeToken(value, "GENERAL");
	}

	public static String normalizeType(String value) {
		return normalizeToken(value, "RECORD");
	}

	public static String normalizeCode(String value) {
		return normalizeToken(value, null);
	}

	private static String normalizeToken(String value, String defaultValue) {
		if (!StringUtils.hasText(value)) {
			return defaultValue;
		}
		return value.trim()
				.replace('-', '_')
				.replace(' ', '_')
				.toUpperCase();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
