package com.school.erp.modules.notifications.domain;

import java.time.Instant;

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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
@SQLRestriction("deleted = false")
public class Notification extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id")
	private NotificationTemplate template;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationChannel channel;

	@Column(length = 180)
	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationStatus status = NotificationStatus.PENDING;

	@Column(name = "scheduled_at")
	private Instant scheduledAt;

	@Column(name = "sent_at")
	private Instant sentAt;
}
