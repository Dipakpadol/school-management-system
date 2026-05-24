package com.school.erp.modules.auth.application;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

public record ClientRequestInfo(String ipAddress, String userAgent) {

	public static ClientRequestInfo from(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String ipAddress = StringUtils.hasText(forwardedFor)
				? forwardedFor.split(",")[0].trim()
				: request.getRemoteAddr();
		return new ClientRequestInfo(truncate(ipAddress, 80), truncate(request.getHeader("User-Agent"), 500));
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
