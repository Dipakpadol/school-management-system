package com.school.erp.modules.communications.application;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.academic.application.AcademicHierarchyService;
import com.school.erp.modules.communications.api.dto.CommunicationRequest;
import com.school.erp.modules.communications.api.dto.CommunicationResponse;
import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.domain.CommunicationRecord;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;
import com.school.erp.modules.communications.infrastructure.CommunicationRecordRepository;
import com.school.erp.modules.staff.domain.EmploymentStatus;
import com.school.erp.modules.staff.domain.StaffType;
import com.school.erp.modules.staff.infrastructure.StaffRepository;
import com.school.erp.modules.students.domain.StudentStatus;
import com.school.erp.modules.students.infrastructure.StudentClassAssignmentRepository;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunicationService {

	private static final String MODULE_NAME = "COMMUNICATION";

	private final CommunicationRecordRepository communicationRepository;
	private final StaffRepository staffRepository;
	private final StudentClassAssignmentRepository studentClassAssignmentRepository;
	private final AcademicHierarchyService academicHierarchyService;
	private final CommunicationMapper mapper;
	private final AuditLogService auditLogService;

	@Transactional(readOnly = true)
	public PageResponse<CommunicationResponse> communications(
			CommunicationType type,
			CommunicationStatus status,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				communicationRepository.search(type, status, pageRequest.toPageable("createdAt")),
				mapper::toResponse);
	}

	@Transactional
	public CommunicationResponse create(CommunicationRequest request) {
		requireAnyAuthority("COMMUNICATION_CREATE", "COMMUNICATION_UPDATE", "COMMUNICATION_PUBLISH");
		validateRequest(request);
		CommunicationRecord record = communicationRepository.save(new CommunicationRecord(
				request.type(),
				request.title(),
				request.message(),
				request.audienceType(),
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.publishAt(),
				request.expiryAt(),
				request.eventStartAt(),
				request.eventEndAt(),
				request.location(),
				request.status()));
		CommunicationResponse response = mapper.toResponse(record);
		audit("CommunicationRecord", record.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public CommunicationResponse update(UUID communicationId, CommunicationRequest request) {
		requireAnyAuthority("COMMUNICATION_UPDATE", "COMMUNICATION_PUBLISH");
		validateRequest(request);
		CommunicationRecord record = loadCommunication(communicationId);
		CommunicationResponse oldValue = mapper.toResponse(record);
		if (record.getStatus() == CommunicationStatus.ARCHIVED) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Archived communication cannot be updated.");
		}
		record.update(
				request.type(),
				request.title(),
				request.message(),
				request.audienceType(),
				request.academicYearId(),
				request.classId(),
				request.sectionId(),
				request.publishAt(),
				request.expiryAt(),
				request.eventStartAt(),
				request.eventEndAt(),
				request.location(),
				request.status());
		CommunicationResponse response = mapper.toResponse(record);
		audit("CommunicationRecord", communicationId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public CommunicationResponse publish(UUID communicationId) {
		requireAnyAuthority("COMMUNICATION_PUBLISH");
		CommunicationRecord record = loadCommunication(communicationId);
		CommunicationResponse oldValue = mapper.toResponse(record);
		if (record.getStatus() == CommunicationStatus.ARCHIVED) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Archived communication cannot be published.");
		}
		long recipients = recipientCount(record);
		record.publish(currentActor(), recipients);
		CommunicationResponse response = mapper.toResponse(record);
		audit("CommunicationRecord", communicationId, "PUBLISH", oldValue, response);
		return response;
	}

	@Transactional
	public CommunicationResponse unpublish(UUID communicationId) {
		requireAnyAuthority("COMMUNICATION_PUBLISH", "COMMUNICATION_UPDATE");
		CommunicationRecord record = loadCommunication(communicationId);
		CommunicationResponse oldValue = mapper.toResponse(record);
		if (record.getStatus() != CommunicationStatus.PUBLISHED) {
			throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Only published communication can be unpublished.");
		}
		record.unpublish();
		CommunicationResponse response = mapper.toResponse(record);
		audit("CommunicationRecord", communicationId, "UNPUBLISH", oldValue, response);
		return response;
	}

	@Transactional
	public CommunicationResponse archive(UUID communicationId) {
		requireAnyAuthority("COMMUNICATION_UPDATE", "COMMUNICATION_PUBLISH");
		CommunicationRecord record = loadCommunication(communicationId);
		CommunicationResponse oldValue = mapper.toResponse(record);
		record.archive();
		CommunicationResponse response = mapper.toResponse(record);
		audit("CommunicationRecord", communicationId, "ARCHIVE", oldValue, response);
		return response;
	}

	private CommunicationRecord loadCommunication(UUID communicationId) {
		return communicationRepository.findByIdAndDeletedFalse(communicationId)
				.orElseThrow(() -> new ResourceNotFoundException("Communication", communicationId));
	}

	private void validateRequest(CommunicationRequest request) {
		if (request.expiryAt() != null && request.publishAt() != null && request.expiryAt().isBefore(request.publishAt())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Expiry date cannot be before publish date.");
		}
		if (request.type() == CommunicationType.EVENT) {
			if (request.eventStartAt() == null || request.eventEndAt() == null) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Event start and end date/time are required.");
			}
			if (request.eventEndAt().isBefore(request.eventStartAt())) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Event end date/time cannot be before start date/time.");
			}
		}
		if (request.audienceType() == CommunicationAudienceType.CLASS && request.classId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class audience requires class id.");
		}
		if (request.audienceType() == CommunicationAudienceType.DIVISION
				&& (request.classId() == null || request.sectionId() == null)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Division audience requires class id and section id.");
		}
		if (request.academicYearId() != null) {
			academicHierarchyService.loadAcademicYear(request.academicYearId());
		}
		if (request.classId() != null) {
			academicHierarchyService.loadClass(request.classId());
		}
		if (request.sectionId() != null && request.classId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Class is required when section is provided.");
		}
		if (request.sectionId() != null) {
			academicHierarchyService.loadSectionForClass(request.classId(), request.sectionId());
		}
	}

	private long recipientCount(CommunicationRecord record) {
		LocalDate today = LocalDate.now();
		return switch (record.getAudienceType()) {
			case STAFF -> staffRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE);
			case TEACHERS -> staffRepository.countByStaffTypeAndStatusAndDeletedFalse(StaffType.TEACHING, EmploymentStatus.ACTIVE);
			case STUDENTS, PARENTS, CLASS, DIVISION -> studentClassAssignmentRepository.countEligibleStudents(
					record.getAcademicYearId(),
					record.getClassId(),
					record.getSectionId(),
					today,
					StudentStatus.INACTIVE);
			case ALL -> staffRepository.countByStatusAndDeletedFalse(EmploymentStatus.ACTIVE)
					+ studentClassAssignmentRepository.countEligibleStudents(
							record.getAcademicYearId(),
							record.getClassId(),
							record.getSectionId(),
							today,
							StudentStatus.INACTIVE);
		};
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		return authentication.getName();
	}

	private void requireAnyAuthority(String... authorities) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return;
		}
		for (String authority : authorities) {
			boolean allowed = authentication.getAuthorities().stream()
					.anyMatch(granted -> granted.getAuthority().equals(authority));
			if (allowed) {
				return;
			}
		}
		throw new BusinessException(ErrorCode.FORBIDDEN, "Communication permission is required.");
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue == null ? Map.of() : newValue));
	}
}
