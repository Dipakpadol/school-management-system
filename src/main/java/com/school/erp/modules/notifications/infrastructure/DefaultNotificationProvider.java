package com.school.erp.modules.notifications.infrastructure;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.school.erp.modules.notifications.application.NotificationDeliveryResult;
import com.school.erp.modules.notifications.application.NotificationProperties;
import com.school.erp.modules.notifications.application.NotificationProvider;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultNotificationProvider implements NotificationProvider {

	private static final Set<String> LOCAL_PROFILES = Set.of("local", "localqa", "test", "dev");
	private static final String SMS_NOT_CONFIGURED = "SMS provider not configured";
	private static final String WHATSAPP_NOT_CONFIGURED = "WhatsApp provider not configured";

	private final ObjectProvider<JavaMailSender> mailSenderProvider;
	private final NotificationProperties properties;
	private final ConsoleNotificationProvider consoleNotificationProvider;
	private final Environment environment;
	private final RestTemplate restTemplate = new RestTemplate();

	@Override
	public NotificationDeliveryResult sendEmail(String to, String subject, String message) {
		if (!properties.getEmail().isEnabled()) {
			return NotificationDeliveryResult.failed(providerName(properties.getEmail().getProvider()), "Email provider not enabled");
		}
		String provider = providerName(properties.getEmail().getProvider());
		if ("console".equalsIgnoreCase(provider)) {
			if (!isLocalProfile() || !properties.getEmail().isConsoleFallbackEnabled()) {
				return NotificationDeliveryResult.failed(provider, "Console email provider is allowed only for local testing");
			}
			consoleNotificationProvider.logEmail(to, subject, message);
			return NotificationDeliveryResult.sent(provider, "console-email", "Console email logged for local testing");
		}
		JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
		if (mailSender == null) {
			return NotificationDeliveryResult.failed(provider, "Email provider not configured");
		}
		try {
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			if (StringUtils.hasText(properties.getEmail().getFrom())) {
				mailMessage.setFrom(properties.getEmail().getFrom().trim());
			}
			mailMessage.setTo(to);
			mailMessage.setSubject(subject);
			mailMessage.setText(message);
			mailSender.send(mailMessage);
			return NotificationDeliveryResult.sent(provider, "smtp", "Email accepted by SMTP provider");
		}
		catch (MailException ex) {
			return NotificationDeliveryResult.failed(provider, safeMessage(ex));
		}
	}

	@Override
	public NotificationDeliveryResult sendSms(String mobileNumber, String message) {
		if (!isSmsConfigured()) {
			return NotificationDeliveryResult.failed(providerName(properties.getSms().getProvider()), SMS_NOT_CONFIGURED);
		}
		return postMessage(
				providerName(properties.getSms().getProvider()),
				properties.getSms().getBaseUrl(),
				properties.getSms().getApiKey(),
				Map.of(
						"to", mobileNumber,
						"message", message,
						"senderId", nullToEmpty(properties.getSms().getSenderId()),
						"templateId", nullToEmpty(properties.getSms().getTemplateId())),
				"SMS accepted by provider");
	}

	@Override
	public NotificationDeliveryResult sendWhatsApp(String mobileNumber, String message) {
		if (!isWhatsAppConfigured()) {
			return NotificationDeliveryResult.failed(providerName(properties.getWhatsapp().getProvider()), WHATSAPP_NOT_CONFIGURED);
		}
		return postMessage(
				providerName(properties.getWhatsapp().getProvider()),
				properties.getWhatsapp().getBaseUrl(),
				properties.getWhatsapp().getApiKey(),
				Map.of(
						"to", mobileNumber,
						"message", message,
						"senderNumber", nullToEmpty(properties.getWhatsapp().getSenderNumber())),
				"WhatsApp message accepted by provider");
	}

	private NotificationDeliveryResult postMessage(
			String provider,
			String baseUrl,
			String apiKey,
			Map<String, String> body,
			String successMessage) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);
			String response = restTemplate.postForObject(baseUrl, new HttpEntity<>(body, headers), String.class);
			return NotificationDeliveryResult.sent(provider, provider, StringUtils.hasText(response) ? response : successMessage);
		}
		catch (RestClientException ex) {
			return NotificationDeliveryResult.failed(provider, safeMessage(ex));
		}
	}

	private boolean isSmsConfigured() {
		return properties.getSms().isEnabled()
				&& !"none".equalsIgnoreCase(providerName(properties.getSms().getProvider()))
				&& StringUtils.hasText(properties.getSms().getApiKey())
				&& StringUtils.hasText(properties.getSms().getBaseUrl());
	}

	private boolean isWhatsAppConfigured() {
		return properties.getWhatsapp().isEnabled()
				&& !"none".equalsIgnoreCase(providerName(properties.getWhatsapp().getProvider()))
				&& StringUtils.hasText(properties.getWhatsapp().getApiKey())
				&& StringUtils.hasText(properties.getWhatsapp().getBaseUrl())
				&& StringUtils.hasText(properties.getWhatsapp().getSenderNumber());
	}

	private boolean isLocalProfile() {
		return Arrays.stream(environment.getActiveProfiles())
				.map(String::toLowerCase)
				.anyMatch(LOCAL_PROFILES::contains);
	}

	private String providerName(String provider) {
		return StringUtils.hasText(provider) ? provider.trim().toLowerCase() : "none";
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private String safeMessage(Exception ex) {
		return StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
	}
}
