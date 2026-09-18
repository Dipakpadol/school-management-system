package com.school.erp.modules.communications.api.dto;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CommunicationRequest(
		@NotNull CommunicationType type,
		@NotBlank String title,
		@NotBlank String message,
		@NotNull CommunicationAudienceType audienceType,
		UUID academicYearId,
		UUID classId,
		UUID sectionId,
		Instant publishAt,
		Instant expiryAt,
		Instant eventStartAt,
		Instant eventEndAt,
		String location,
		CommunicationStatus status) {
}
