package com.school.erp.modules.library.domain;

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
@Table(name = "library_publishers")
@SQLRestriction("deleted = false")
public class LibraryPublisher extends BaseEntity {

	@Column(nullable = false, length = 160)
	private String name;

	@Column(name = "contact_info", length = 500)
	private String contactInfo;

	@Column(nullable = false)
	private boolean active = true;

	public LibraryPublisher(String name, String contactInfo, boolean active) {
		update(name, contactInfo, active);
	}

	public void update(String name, String contactInfo, boolean active) {
		this.name = trim(name);
		this.contactInfo = trimToNull(contactInfo);
		this.active = active;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
