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
@Table(name = "library_authors")
@SQLRestriction("deleted = false")
public class LibraryAuthor extends BaseEntity {

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 1000)
	private String biography;

	@Column(nullable = false)
	private boolean active = true;

	public LibraryAuthor(String name, String biography, boolean active) {
		update(name, biography, active);
	}

	public void update(String name, String biography, boolean active) {
		this.name = trim(name);
		this.biography = trimToNull(biography);
		this.active = active;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
