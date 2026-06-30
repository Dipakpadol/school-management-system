package com.school.erp.modules.notifications.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotificationProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleNotificationProvider.class);

	public void logEmail(String to, String subject, String message) {
		LOGGER.info("Console email notification to={} subject={} message={}", to, subject, message);
	}

	public void logSms(String mobileNumber, String message) {
		LOGGER.info("Console SMS notification to={} message={}", mobileNumber, message);
	}

	public void logWhatsApp(String mobileNumber, String message) {
		LOGGER.info("Console WhatsApp notification to={} message={}", mobileNumber, message);
	}
}
