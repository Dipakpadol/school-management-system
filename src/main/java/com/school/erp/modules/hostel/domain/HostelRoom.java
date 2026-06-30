package com.school.erp.modules.hostel.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "hostel_rooms")
@SQLRestriction("deleted = false")
public class HostelRoom extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "hostel_id", nullable = false)
	private Hostel hostel;

	@Column(name = "room_number", nullable = false, length = 60)
	private String roomNumber;

	@Column(name = "room_type", nullable = false, length = 80)
	private String roomType;

	@Column(nullable = false)
	private int capacity;

	@Column(name = "has_beds", nullable = false)
	private boolean hasBeds;

	@Column(nullable = false)
	private boolean active = true;

	@OneToMany(mappedBy = "room", cascade = CascadeType.ALL)
	private Set<HostelBed> beds = new LinkedHashSet<>();

	public HostelRoom(Hostel hostel, String roomNumber, String roomType, int capacity, boolean hasBeds) {
		this.hostel = hostel;
		this.roomNumber = trim(roomNumber);
		this.roomType = normalizeRoomType(roomType);
		this.capacity = capacity;
		this.hasBeds = hasBeds;
	}

	public void update(String roomNumber, String roomType, int capacity, boolean hasBeds, boolean active) {
		this.roomNumber = trim(roomNumber);
		this.roomType = normalizeRoomType(roomType);
		this.capacity = capacity;
		this.hasBeds = hasBeds;
		this.active = active;
	}

	public HostelBed addBed(String bedNumber) {
		HostelBed bed = new HostelBed(this, bedNumber);
		beds.add(bed);
		return bed;
	}

	private String normalizeRoomType(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
