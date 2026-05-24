package com.school.erp.modules.auth.application;

import java.time.Instant;

import com.school.erp.modules.auth.api.dto.AuthenticationResponse;
import com.school.erp.modules.auth.api.dto.UserSessionResponse;
import com.school.erp.modules.users.domain.UserAccount;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthMapper {

	private final JwtService jwtService;

	public AuthenticationResponse toAuthenticationResponse(
			UserAccount user,
			GeneratedAccessToken accessToken,
			String refreshToken,
			Instant refreshTokenExpiresAt) {
		return new AuthenticationResponse(
				"Bearer",
				accessToken.value(),
				accessToken.expiresAt(),
				refreshToken,
				refreshTokenExpiresAt,
				new UserSessionResponse(
						user.getId(),
						user.getEmail(),
						user.getDisplayName(),
						jwtService.roles(user),
						jwtService.permissions(user)));
	}
}
