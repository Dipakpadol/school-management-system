package com.school.erp.modules.notifications.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.modules.notifications.api.dto.NotificationLogResponse;
import com.school.erp.modules.notifications.api.dto.TestEmailNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestSmsNotificationRequest;
import com.school.erp.modules.notifications.api.dto.TestWhatsAppNotificationRequest;
import com.school.erp.modules.notifications.domain.NotificationLog;
import com.school.erp.modules.notifications.domain.NotificationStatus;
import com.school.erp.modules.notifications.infrastructure.NotificationLogRepository;
import com.school.erp.modules.notifications.infrastructure.NotificationTemplateRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

	@Mock
	private NotificationProvider notificationProvider;

	@Mock
	private NotificationLogRepository logRepository;

	@Mock
	private NotificationTemplateRepository templateRepository;

	@Mock
	private AuditLogService auditLogService;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(notificationProvider, logRepository, templateRepository, auditLogService);
		when(logRepository.save(any(NotificationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void testEmailSuccessMarksLogSent() {
		when(notificationProvider.sendEmail("test@example.com", "Test", "Hello"))
				.thenReturn(NotificationDeliveryResult.sent("smtp", "smtp", "Email accepted"));

		NotificationLogResponse response = notificationService.sendTestEmail(
				new TestEmailNotificationRequest("test@example.com", "Test", "Hello"));

		assertThat(response.status()).isEqualTo(NotificationStatus.SENT);
		assertThat(response.provider()).isEqualTo("smtp");
		assertThat(response.providerResponse()).isEqualTo("Email accepted");
		verify(notificationProvider).sendEmail("test@example.com", "Test", "Hello");
	}

	@Test
	void testEmailFailureMarksLogFailed() {
		when(notificationProvider.sendEmail("test@example.com", "Test", "Hello"))
				.thenReturn(NotificationDeliveryResult.failed("smtp", "SMTP rejected message"));

		NotificationLogResponse response = notificationService.sendTestEmail(
				new TestEmailNotificationRequest("test@example.com", "Test", "Hello"));

		assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(response.provider()).isEqualTo("smtp");
		assertThat(response.errorMessage()).isEqualTo("SMTP rejected message");
		assertThat(response.failedAt()).isNotNull();
	}

	@Test
	void testSmsNotConfiguredMarksLogFailed() {
		when(notificationProvider.sendSms("9999999999", "Hello"))
				.thenReturn(NotificationDeliveryResult.failed("none", "SMS provider not configured"));

		NotificationLogResponse response = notificationService.sendTestSms(
				new TestSmsNotificationRequest("9999999999", "Hello"));

		assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(response.errorMessage()).isEqualTo("SMS provider not configured");
	}

	@Test
	void testWhatsAppNotConfiguredMarksLogFailed() {
		when(notificationProvider.sendWhatsApp("9999999999", "Hello"))
				.thenReturn(NotificationDeliveryResult.failed("none", "WhatsApp provider not configured"));

		NotificationLogResponse response = notificationService.sendTestWhatsApp(
				new TestWhatsAppNotificationRequest("9999999999", "Hello"));

		assertThat(response.status()).isEqualTo(NotificationStatus.FAILED);
		assertThat(response.errorMessage()).isEqualTo("WhatsApp provider not configured");
	}
}
