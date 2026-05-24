package com.school.erp.common.audit.application;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.api.dto.AuditLogDto;
import com.school.erp.common.audit.api.dto.AuditLogSearchRequest;
import com.school.erp.common.audit.domain.AuditLog;
import com.school.erp.common.audit.infrastructure.AuditLogRepository;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

	private final AuditLogRepository auditLogRepository;
	private final AuditLogMapper auditLogMapper;
	private final ObjectMapper objectMapper;

	@Transactional(propagation = Propagation.MANDATORY)
	public AuditLogDto record(AuditLogEvent event) {
		AuditLog auditLog = new AuditLog(
				event.moduleName(),
				event.entityName(),
				event.entityId(),
				event.action(),
				serialize(event.oldValue()),
				serialize(event.newValue()),
				currentActor(),
				Instant.now(),
				currentIpAddress());
		return auditLogMapper.toDto(auditLogRepository.save(auditLog));
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditLogDto> search(AuditLogSearchRequest request, PageRequestDto pageRequest) {
		validateDateRange(request);
		return PageResponse.from(
				auditLogRepository.findAll(AuditLogSpecifications.matching(request), toPageable(pageRequest)),
				auditLogMapper::toDto);
	}

	@Transactional(readOnly = true)
	public AuditLogDto getById(UUID id) {
		return auditLogRepository.findById(id)
				.map(auditLogMapper::toDto)
				.orElseThrow(() -> new ResourceNotFoundException("Audit log", id));
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditLogDto> findByModule(String moduleName, PageRequestDto pageRequest) {
		return search(new AuditLogSearchRequest(null, moduleName, null, null, null, null, null, null), pageRequest);
	}

	@Transactional(readOnly = true)
	public PageResponse<AuditLogDto> findByEntity(String entityName, String entityId, PageRequestDto pageRequest) {
		return search(new AuditLogSearchRequest(null, null, entityName, entityId, null, null, null, null), pageRequest);
	}

	private void validateDateRange(AuditLogSearchRequest request) {
		if (request.performedFrom() != null
				&& request.performedTo() != null
				&& request.performedFrom().isAfter(request.performedTo())) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Audit performedFrom must be before or equal to performedTo.");
		}
	}

	private Pageable toPageable(PageRequestDto pageRequest) {
		String sortBy = StringUtils.hasText(pageRequest.sortBy()) ? pageRequest.sortBy() : "performedAt";
		Sort.Direction direction = StringUtils.hasText(pageRequest.sortBy())
				? pageRequest.direction()
				: Sort.Direction.DESC;
		return PageRequest.of(pageRequest.page(), pageRequest.size(), Sort.by(direction, sortBy));
	}

	private String serialize(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof String stringValue) {
			return stringValue;
		}
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (JsonProcessingException ex) {
			return String.valueOf(value);
		}
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return "system";
		}
		return authentication.getName();
	}

	private String currentIpAddress() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			HttpServletRequest request = attributes.getRequest();
			String forwardedFor = request.getHeader("X-Forwarded-For");
			String ipAddress = StringUtils.hasText(forwardedFor)
					? forwardedFor.split(",")[0].trim()
					: request.getRemoteAddr();
			return truncate(ipAddress, 80);
		}
		return null;
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
