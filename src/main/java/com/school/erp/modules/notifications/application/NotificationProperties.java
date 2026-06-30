package com.school.erp.modules.notifications.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "notifications")
public class NotificationProperties {

	private Scheduler scheduler = new Scheduler();
	private String schoolName = "School ERP";
	private Email email = new Email();
	private Sms sms = new Sms();
	private WhatsApp whatsapp = new WhatsApp();

	@Getter
	@Setter
	public static class Scheduler {

		private boolean enabled = false;
		private String feeDueCron = "0 0 9 * * *";
		private String feeOverdueCron = "0 30 9 * * *";
		private String absenceCron = "0 0 17 * * *";
		private String examReminderCron = "0 0 18 * * *";
		private String hostelFeeCron = "0 15 9 * * *";
		private String zone = "Asia/Kolkata";
		private boolean emailEnabled = true;
		private boolean smsEnabled = false;
		private boolean whatsappEnabled = false;
	}

	@Getter
	@Setter
	public static class Email {

		private boolean enabled = true;
		private String provider = "smtp";
		private String from;
		private boolean consoleFallbackEnabled = false;
	}

	@Getter
	@Setter
	public static class Sms {

		private boolean enabled = false;
		private String provider = "none";
		private String apiKey;
		private String senderId;
		private String templateId;
		private String baseUrl;
	}

	@Getter
	@Setter
	public static class WhatsApp {

		private boolean enabled = false;
		private String provider = "none";
		private String apiKey;
		private String baseUrl;
		private String senderNumber;
	}
}
