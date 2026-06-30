package com.school.erp.modules.notifications.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.school.erp.modules.notifications.application.NotificationDeliveryResult;
import com.school.erp.modules.notifications.application.NotificationProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class DefaultNotificationProviderTest {

	@Mock
	private ObjectProvider<JavaMailSender> mailSenderProvider;

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private Environment environment;

	private NotificationProperties properties;
	private DefaultNotificationProvider provider;

	@BeforeEach
	void setUp() {
		properties = new NotificationProperties();
		provider = new DefaultNotificationProvider(
				mailSenderProvider,
				properties,
				new ConsoleNotificationProvider(),
				environment);
	}

	@Test
	void smtpEmailSuccessReturnsSentOnlyAfterMailSenderAcceptsMessage() {
		when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

		NotificationDeliveryResult result = provider.sendEmail("test@example.com", "Subject", "Body");

		assertThat(result.success()).isTrue();
		assertThat(result.provider()).isEqualTo("smtp");
		verify(mailSender).send(any(SimpleMailMessage.class));
	}

	@Test
	void smsWithoutGatewayConfigurationReturnsFailed() {
		NotificationDeliveryResult result = provider.sendSms("9999999999", "Body");

		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("SMS provider not configured");
	}

	@Test
	void whatsappWithoutGatewayConfigurationReturnsFailed() {
		NotificationDeliveryResult result = provider.sendWhatsApp("9999999999", "Body");

		assertThat(result.success()).isFalse();
		assertThat(result.errorMessage()).isEqualTo("WhatsApp provider not configured");
	}
}
