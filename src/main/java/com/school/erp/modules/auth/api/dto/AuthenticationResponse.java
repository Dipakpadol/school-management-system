package com.school.erp.modules.auth.api.dto;

import java.time.Instant;

public record AuthenticationResponse(
		String tokenType,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt,
		UserSessionResponse user) {
}
