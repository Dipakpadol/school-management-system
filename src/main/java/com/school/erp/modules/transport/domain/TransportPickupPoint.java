package com.school.erp.modules.transport.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "transport_pickup_points")
@SQLRestriction("deleted = false")
public class TransportPickupPoint extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "route_id", nullable = false)
	private TransportRoute route;

	@Column(name = "point_name", nullable = false, length = 160)
	private String pointName;

	@Column(name = "pickup_time")
	private LocalTime pickupTime;

	@Column(name = "drop_time")
	private LocalTime dropTime;

	@Column(name = "monthly_fee", precision = 12, scale = 2)
	private BigDecimal monthlyFee;

	@Column(name = "sequence_order", nullable = false)
	private int sequenceOrder;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TransportStatus status = TransportStatus.ACTIVE;

	public TransportPickupPoint(
			TransportRoute route,
			String pointName,
			LocalTime pickupTime,
			LocalTime dropTime,
			BigDecimal monthlyFee,
			int sequenceOrder,
			TransportStatus status) {
		update(route, pointName, pickupTime, dropTime, monthlyFee, sequenceOrder, status);
	}

	public boolean isActive() {
		return status == TransportStatus.ACTIVE;
	}

	public void update(
			TransportRoute route,
			String pointName,
			LocalTime pickupTime,
			LocalTime dropTime,
			BigDecimal monthlyFee,
			int sequenceOrder,
			TransportStatus status) {
		this.route = route;
		this.pointName = trim(pointName);
		this.pickupTime = pickupTime;
		this.dropTime = dropTime;
		this.monthlyFee = moneyOrNull(monthlyFee);
		this.sequenceOrder = sequenceOrder;
		this.status = status == null ? TransportStatus.ACTIVE : status;
	}

	private BigDecimal moneyOrNull(BigDecimal value) {
		return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
