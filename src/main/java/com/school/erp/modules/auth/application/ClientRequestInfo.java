package com.school.erp.modules.auth.application;

import jakarta.servlet.http.HttpServletRequest;

public record ClientRequestInfo(String ipAddress, String userAgent) {

	public static ClientRequestInfo from(HttpServletRequest request) {
		String ipAddress = request.getRemoteAddr();
		return new ClientRequestInfo(truncate(ipAddress, 80), truncate(request.getHeader("User-Agent"), 500));
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
