package com.school.erp.modules.communications.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;

public record CommunicationResponse(
		UUID id,
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
		CommunicationStatus status,
		long recipientCount,
		Instant publishedAt,
		String publishedBy,
		Instant archivedAt) {
}
