package com.school.erp.modules.auth.application;

import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(
		UUID userId,
		String email,
		String displayName,
		String resetToken,
		Instant expiresAt) {
}
