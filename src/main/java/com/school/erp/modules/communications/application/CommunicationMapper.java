package com.school.erp.modules.communications.application;

import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.communications.domain.CommunicationRecord;

import org.springframework.stereotype.Component;

@Component
public class CommunicationMapper {

	public CommunicationResponse toResponse(CommunicationRecord record) {
		return new CommunicationResponse(
				record.getId(),
				record.getType(),
				record.getTitle(),
				record.getMessage(),
				record.getAudienceType(),
				record.getAcademicYearId(),
				record.getClassId(),
				record.getSectionId(),
				record.getPublishAt(),
				record.getExpiryAt(),
				record.getEventStartAt(),
				record.getEventEndAt(),
				record.getLocation(),
				record.getStatus(),
				record.getRecipientCount(),
				record.getPublishedAt(),
				record.getPublishedBy(),
				record.getArchivedAt());
	}
}
