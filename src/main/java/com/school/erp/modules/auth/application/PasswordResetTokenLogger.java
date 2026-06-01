package com.school.erp.modules.auth.application;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenLogger {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetTokenLogger.class);
	private static final java.util.Set<String> TOKEN_LOGGING_PROFILES = java.util.Set.of("dev", "local", "qa");

	private final Environment environment;

	@EventListener
	public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
		if (!shouldLogResetToken()) {
			return;
		}
		log.info(
				"Password reset token generated for {} ({}) expires at {}: {}",
				event.displayName(),
				event.email(),
				event.expiresAt(),
				event.resetToken());
	}

	private boolean shouldLogResetToken() {
		return Arrays.stream(environment.getActiveProfiles())
				.map(String::toLowerCase)
				.anyMatch(TOKEN_LOGGING_PROFILES::contains);
	}
}
