package com.school.erp.modules.settings.domain;

import com.school.erp.common.domain.BaseEntity;

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
@Table(name = "application_settings")
@SQLRestriction("deleted = false")
public class ApplicationSetting extends BaseEntity {

	@Column(name = "group_name", nullable = false, length = 80)
	private String groupName;

	@Column(name = "setting_key", nullable = false, length = 120)
	private String settingKey;

	@Column(name = "setting_value", columnDefinition = "TEXT")
	private String settingValue;

	public ApplicationSetting(String groupName, String settingKey, String settingValue) {
		this.groupName = normalize(groupName);
		this.settingKey = normalize(settingKey);
		this.settingValue = trimToNull(settingValue);
	}

	public void updateValue(String settingValue) {
		this.settingValue = trimToNull(settingValue);
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
