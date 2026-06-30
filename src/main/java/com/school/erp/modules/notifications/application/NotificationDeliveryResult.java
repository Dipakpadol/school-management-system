package com.school.erp.modules.notifications.application;

public record NotificationDeliveryResult(
		boolean success,
		String provider,
		String providerReference,
		String providerResponse,
		String errorMessage) {

	public static NotificationDeliveryResult sent(String providerResponse) {
		return sent(null, null, providerResponse);
	}

	public static NotificationDeliveryResult sent(String provider, String providerReference, String providerResponse) {
		return new NotificationDeliveryResult(true, provider, providerReference, providerResponse, null);
	}

	public static NotificationDeliveryResult failed(String errorMessage) {
		return failed(null, errorMessage);
	}

	public static NotificationDeliveryResult failed(String provider, String errorMessage) {
		return new NotificationDeliveryResult(false, provider, null, null, errorMessage);
	}
}
