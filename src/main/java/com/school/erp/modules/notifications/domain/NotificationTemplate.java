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
@Table(name = "notification_templates")
@SQLRestriction("deleted = false")
public class NotificationTemplate extends BaseEntity {

	@Column(name = "template_code", nullable = false, length = 100)
	private String templateCode;

	@Column(name = "template_name", nullable = false, length = 160)
	private String templateName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationChannel channel;

	@Column(length = 180)
	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Column(name = "variables_json", columnDefinition = "TEXT")
	private String variables;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationTemplateStatus status = NotificationTemplateStatus.ACTIVE;

	public NotificationTemplate(
			String templateCode,
			String templateName,
			NotificationChannel channel,
			String subject,
			String body,
			String variables,
			NotificationTemplateStatus status) {
		update(templateCode, templateName, channel, subject, body, variables, status);
	}

	public void update(
			String templateCode,
			String templateName,
			NotificationChannel channel,
			String subject,
			String body,
			String variables,
			NotificationTemplateStatus status) {
		this.templateCode = templateCode.trim().toUpperCase();
		this.templateName = templateName.trim();
		this.channel = channel;
		this.subject = trimToNull(subject);
		this.body = body.trim();
		this.variables = trimToNull(variables);
		this.status = status == null ? NotificationTemplateStatus.ACTIVE : status;
	}

	private String trimToNull(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim();
	}
}
