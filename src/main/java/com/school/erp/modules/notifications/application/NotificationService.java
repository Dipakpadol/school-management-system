package com.school.erp.modules.notifications.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.api.PageRequestDto;
import com.school.erp.common.api.PageResponse;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.notifications.api.dto.NotificationLogResponse;
import com.school.erp.modules.notifications.api.dto.NotificationTemplateRequest;
import com.school.erp.modules.notifications.api.dto.NotificationTemplateResponse;
import com.school.erp.modules.notifications.api.dto.TestEmailNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestSmsNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestWhatsAppNotificationRequest;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationLog;
import com.school.erp.modules.notifications.domain.NotificationStatus;
import com.school.erp.modules.notifications.domain.NotificationTemplate;
import com.school.erp.modules.notifications.infrastructure.NotificationLogRepository;
import com.school.erp.modules.notifications.infrastructure.NotificationTemplateRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private static final String MODULE_NAME = "NOTIFICATIONS";

	private final NotificationProvider notificationProvider;
	private final NotificationLogRepository logRepository;
	private final NotificationTemplateRepository templateRepository;
	private final AuditLogService auditLogService;

	@Transactional
	public NotificationLogResponse sendTestEmail(TestEmailNotificationRequest request) {
		NotificationLog log = sendNotification(NotificationChannel.EMAIL, request.to(), request.subject(), request.message(), null, null);
		NotificationLogResponse response = toLogResponse(log);
		audit("NotificationLog", log.getId(), "TEST_EMAIL_SENT", null, response);
		return response;
	}

	@Transactional
	public NotificationLogResponse sendTestSms(TestSmsNotificationRequest request) {
		NotificationLog log = sendNotification(NotificationChannel.SMS, request.mobileNumber(), null, request.message(), null, null);
		NotificationLogResponse response = toLogResponse(log);
		audit("NotificationLog", log.getId(), "TEST_SMS_SENT", null, response);
		return response;
	}

	@Transactional
	public NotificationLogResponse sendTestWhatsApp(TestWhatsAppNotificationRequest request) {
		NotificationLog log = sendNotification(NotificationChannel.WHATSAPP, request.mobileNumber(), null, request.message(), null, null);
		NotificationLogResponse response = toLogResponse(log);
		audit("NotificationLog", log.getId(), "TEST_WHATSAPP_SENT", null, response);
		return response;
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationLogResponse> logs(
			NotificationChannel channel,
			NotificationStatus status,
			String referenceType,
			UUID referenceId,
			PageRequestDto pageRequest) {
		return PageResponse.from(
				logRepository.search(
						channel,
						status,
						normalizeReferenceType(referenceType),
						referenceId == null ? null : referenceId.toString(),
						pageRequest.toPageable("createdAt")),
				this::toLogResponse);
	}

	@Transactional(readOnly = true)
	public PageResponse<NotificationTemplateResponse> templates(PageRequestDto pageRequest) {
		return PageResponse.from(
				templateRepository.findAllByDeletedFalse(pageRequest.toPageable("templateCode")),
				this::toTemplateResponse);
	}

	@Transactional(readOnly = true)
	public NotificationTemplateResponse getTemplate(UUID templateId) {
		return toTemplateResponse(loadTemplate(templateId));
	}

	@Transactional
	public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
		ensureTemplateCodeAvailable(request.templateCode(), null);
		NotificationTemplate template = templateRepository.save(new NotificationTemplate(
				request.templateCode(),
				request.templateName(),
				request.channel(),
				request.subject(),
				request.body(),
				request.variables(),
				request.status()));
		NotificationTemplateResponse response = toTemplateResponse(template);
		audit("NotificationTemplate", template.getId(), "CREATE", null, response);
		return response;
	}

	@Transactional
	public NotificationTemplateResponse updateTemplate(UUID templateId, NotificationTemplateRequest request) {
		NotificationTemplate template = loadTemplate(templateId);
		NotificationTemplateResponse oldValue = toTemplateResponse(template);
		ensureTemplateCodeAvailable(request.templateCode(), templateId);
		template.update(
				request.templateCode(),
				request.templateName(),
				request.channel(),
				request.subject(),
				request.body(),
				request.variables(),
				request.status());
		NotificationTemplateResponse response = toTemplateResponse(template);
		audit("NotificationTemplate", templateId, "UPDATE", oldValue, response);
		return response;
	}

	@Transactional
	public void deleteTemplate(UUID templateId) {
		NotificationTemplate template = loadTemplate(templateId);
		NotificationTemplateResponse oldValue = toTemplateResponse(template);
		template.softDelete("system");
		audit("NotificationTemplate", templateId, "DELETE", oldValue, Map.of("deleted", true, "templateId", templateId));
	}

	@Transactional
	public NotificationLog sendNotification(
			NotificationChannel channel,
			String recipient,
			String subject,
			String message,
			String referenceType,
			UUID referenceId) {
		NotificationLog log = savePending(channel, recipient, subject, message, referenceType, referenceId);
		NotificationDeliveryResult result = deliver(() -> switch (channel) {
			case EMAIL -> notificationProvider.sendEmail(recipient, subject, message);
			case SMS -> notificationProvider.sendSms(recipient, message);
			case WHATSAPP -> notificationProvider.sendWhatsApp(recipient, message);
			case PUSH -> NotificationDeliveryResult.failed("push", "Push provider not configured");
		});
		applyDeliveryResult(log, result);
		return log;
	}

	@Transactional
	public NotificationLog recordSkipped(
			NotificationChannel channel,
			String recipient,
			String subject,
			String message,
			String referenceType,
			UUID referenceId,
			String reason) {
		NotificationLog log = savePending(channel, recipient, subject, message, referenceType, referenceId);
		log.markSkipped(reason);
		return log;
	}

	@Transactional(readOnly = true)
	public boolean wasSentBetween(
			String referenceType,
			UUID referenceId,
			NotificationChannel channel,
			Instant from,
			Instant to) {
		return logRepository.existsByReferenceTypeAndReferenceIdAndChannelAndStatusAndCreatedAtBetweenAndDeletedFalse(
				normalizeReferenceType(referenceType),
				referenceId.toString(),
				channel,
				NotificationStatus.SENT,
				from,
				to);
	}

	private NotificationLog savePending(
			NotificationChannel channel,
			String recipient,
			String subject,
			String message,
			String referenceType,
			UUID referenceId) {
		return logRepository.save(new NotificationLog(channel, recipient, subject, message, referenceType, referenceId));
	}

	private NotificationDeliveryResult deliver(NotificationDelivery delivery) {
		try {
			return delivery.send();
		}
		catch (RuntimeException ex) {
			return NotificationDeliveryResult.failed(ex.getMessage());
		}
	}

	private void applyDeliveryResult(NotificationLog log, NotificationDeliveryResult result) {
		if (result.success()) {
			log.markSent(result.provider(), result.providerReference(), result.providerResponse());
		}
		else {
			log.markFailed(result.provider(), result.errorMessage());
		}
	}

	private void ensureTemplateCodeAvailable(String templateCode, UUID currentId) {
		String normalized = templateCode.trim().toUpperCase();
		boolean exists = currentId == null
				? templateRepository.existsByTemplateCodeIgnoreCaseAndDeletedFalse(normalized)
				: templateRepository.existsByTemplateCodeIgnoreCaseAndIdNotAndDeletedFalse(normalized, currentId);
		if (exists) {
			throw new BusinessException(ErrorCode.CONFLICT, "Notification template code already exists: " + normalized);
		}
	}

	private NotificationTemplate loadTemplate(UUID templateId) {
		return templateRepository.findByIdAndDeletedFalse(templateId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification template", templateId));
	}

	private NotificationLogResponse toLogResponse(NotificationLog log) {
		return new NotificationLogResponse(
				log.getId(),
				log.getChannel(),
				log.getRecipient(),
				log.getSubject(),
				log.getMessage(),
				log.getStatus(),
				log.getProvider(),
				log.getProviderReference(),
				log.getProviderResponse(),
				log.getErrorMessage(),
				log.getReferenceType(),
				log.getReferenceId(),
				log.getSentAt(),
				log.getFailedAt(),
				log.getRetryCount(),
				log.getCreatedAt());
	}

	private String normalizeReferenceType(String value) {
		return value == null || value.trim().isEmpty() ? null : value.trim().toUpperCase();
	}

	private NotificationTemplateResponse toTemplateResponse(NotificationTemplate template) {
		return new NotificationTemplateResponse(
				template.getId(),
				template.getTemplateCode(),
				template.getTemplateName(),
				template.getChannel(),
				template.getSubject(),
				template.getBody(),
				template.getVariables(),
				template.getStatus(),
				template.getCreatedAt(),
				template.getUpdatedAt());
	}

	private void audit(String entityName, UUID entityId, String action, Object oldValue, Object newValue) {
		auditLogService.record(new AuditLogEvent(
				MODULE_NAME,
				entityName,
				entityId == null ? null : entityId.toString(),
				action,
				oldValue,
				newValue));
	}

	@FunctionalInterface
	private interface NotificationDelivery {

		NotificationDeliveryResult send();
	}
}
