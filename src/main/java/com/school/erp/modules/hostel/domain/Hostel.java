package com.school.erp.modules.hostel.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hostels")
@SQLRestriction("deleted = false")
public class Hostel extends BaseEntity {

	@Column(nullable = false, unique = true, length = 60)
	private String code;

	@Column(nullable = false, length = 160)
	private String name;

	@Column(length = 500)
	private String address;

	@Column(nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "hostel", cascade = CascadeType.ALL)
	private Set<HostelRoom> rooms = new LinkedHashSet<>();

	public Hostel(String code, String name, String address) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.address = trimToNull(address);
	}

	public void update(String code, String name, String address, boolean active) {
		this.code = normalizeCode(code);
		this.name = trim(name);
		this.address = trimToNull(address);
		this.active = active;
	}

	private String normalizeCode(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
