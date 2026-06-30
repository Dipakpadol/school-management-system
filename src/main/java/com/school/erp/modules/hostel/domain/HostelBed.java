package com.school.erp.modules.hostel.domain;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "hostel_beds")
@SQLRestriction("deleted = false")
public class HostelBed extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_id", nullable = false)
	private HostelRoom room;

	@Column(name = "bed_number", nullable = false, length = 60)
	private String bedNumber;

	@Column(nullable = false)
	private boolean active = true;

	HostelBed(HostelRoom room, String bedNumber) {
		this.room = room;
		this.bedNumber = trim(bedNumber);
	}

	public void update(String bedNumber, boolean active) {
		this.bedNumber = trim(bedNumber);
		this.active = active;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
