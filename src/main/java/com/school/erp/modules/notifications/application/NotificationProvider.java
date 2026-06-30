package com.school.erp.modules.notifications.application;

public interface NotificationProvider {

	NotificationDeliveryResult sendEmail(String to, String subject, String message);

	NotificationDeliveryResult sendSms(String mobileNumber, String message);

	NotificationDeliveryResult sendWhatsApp(String mobileNumber, String message);
}
