package com.school.erp.modules.users.domain;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

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

	@Column(length = 500)
	private String description;

	public Permission(String code, String name, String description) {
		this.code = code;
		this.name = name;
		this.description = description;
	}
}
