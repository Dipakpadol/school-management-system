package com.school.erp.modules.notifications.domain;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notification_channel_configs")
@SQLRestriction("deleted = false")
public class NotificationChannelConfig extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationChannel channel;

	@Column(name = "provider_name", nullable = false, length = 80)
	private String providerName;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "metadata_json", columnDefinition = "TEXT")
	private String metadataJson;
}
