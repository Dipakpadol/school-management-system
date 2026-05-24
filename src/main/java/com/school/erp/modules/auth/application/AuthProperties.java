package com.school.erp.modules.auth.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.auth")
public record AuthProperties(int maxFailedLoginAttempts, Duration lockDuration) {

	public AuthProperties {
		maxFailedLoginAttempts = maxFailedLoginAttempts <= 0 ? 5 : maxFailedLoginAttempts;
		lockDuration = lockDuration == null ? Duration.ofMinutes(15) : lockDuration;
	}
}
