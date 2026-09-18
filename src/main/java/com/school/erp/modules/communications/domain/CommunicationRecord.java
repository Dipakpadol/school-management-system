package com.school.erp.modules.communications.domain;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "communication_records")
@SQLRestriction("deleted = false")
public class CommunicationRecord extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CommunicationType type;

	@Column(nullable = false, length = 180)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(name = "audience_type", nullable = false, length = 30)
	private CommunicationAudienceType audienceType;

	@Column(name = "academic_year_id")
	private UUID academicYearId;

	@Column(name = "class_id")
	private UUID classId;

	@Column(name = "section_id")
	private UUID sectionId;

	@Column(name = "publish_at")
	private Instant publishAt;

	@Column(name = "expiry_at")
	private Instant expiryAt;

	@Column(name = "event_start_at")
	private Instant eventStartAt;

	@Column(name = "event_end_at")
	private Instant eventEndAt;

	@Column(length = 180)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private CommunicationStatus status = CommunicationStatus.DRAFT;

	@Column(name = "recipient_count", nullable = false)
	private long recipientCount;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "published_by", length = 120)
	private String publishedBy;

	@Column(name = "archived_at")
	private Instant archivedAt;

	public CommunicationRecord(
			CommunicationType type,
			String title,
			String message,
			CommunicationAudienceType audienceType,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			Instant publishAt,
			Instant expiryAt,
			Instant eventStartAt,
			Instant eventEndAt,
			String location,
			CommunicationStatus status) {
		update(type, title, message, audienceType, academicYearId, classId, sectionId, publishAt, expiryAt,
				eventStartAt, eventEndAt, location, status);
	}

	public void update(
			CommunicationType type,
			String title,
			String message,
			CommunicationAudienceType audienceType,
			UUID academicYearId,
			UUID classId,
			UUID sectionId,
			Instant publishAt,
			Instant expiryAt,
			Instant eventStartAt,
			Instant eventEndAt,
			String location,
			CommunicationStatus status) {
		this.type = type == null ? CommunicationType.ANNOUNCEMENT : type;
		this.title = trim(title);
		this.message = trim(message);
		this.audienceType = audienceType == null ? CommunicationAudienceType.ALL : audienceType;
		this.academicYearId = academicYearId;
		this.classId = classId;
		this.sectionId = sectionId;
		this.publishAt = publishAt;
		this.expiryAt = expiryAt;
		this.eventStartAt = eventStartAt;
		this.eventEndAt = eventEndAt;
		this.location = trimToNull(location);
		if (status != null) {
			this.status = status;
		}
	}

	public void publish(String actor, long recipientCount) {
		this.status = CommunicationStatus.PUBLISHED;
		this.publishedAt = Instant.now();
		this.publishedBy = trimToNull(actor);
		this.recipientCount = recipientCount;
	}

	public void unpublish() {
		this.status = CommunicationStatus.UNPUBLISHED;
	}

	public void archive() {
		this.status = CommunicationStatus.ARCHIVED;
		this.archivedAt = Instant.now();
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
