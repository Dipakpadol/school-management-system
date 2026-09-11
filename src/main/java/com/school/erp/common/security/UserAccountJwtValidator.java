package com.school.erp.common.security;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserAccountJwtValidator implements OAuth2TokenValidator<Jwt> {

	private static final OAuth2Error INVALID_USER = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"JWT subject does not map to an active user account.",
			null);
	private static final OAuth2Error STALE_PASSWORD = new OAuth2Error(
			OAuth2ErrorCodes.INVALID_TOKEN,
			"JWT was issued before the current password version.",
			null);

	private final UserAccountRepository userAccountRepository;

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		UUID userId = parseSubject(token.getSubject());
		if (userId == null) {
			return OAuth2TokenValidatorResult.failure(INVALID_USER);
		}

		return userAccountRepository.findWithRolesByIdAndDeletedFalse(userId)
				.map(user -> validateUser(token, user))
				.orElseGet(() -> OAuth2TokenValidatorResult.failure(INVALID_USER));
	}

	private OAuth2TokenValidatorResult validateUser(Jwt token, UserAccount user) {
		if (!user.canAuthenticate()) {
			return OAuth2TokenValidatorResult.failure(INVALID_USER);
		}
		Instant issuedAt = token.getIssuedAt();
		Instant passwordChangedAt = user.getPasswordChangedAt();
		if (issuedAt != null && passwordChangedAt != null && issuedAt.isBefore(passwordChangedAt)) {
			return OAuth2TokenValidatorResult.failure(STALE_PASSWORD);
		}
		return OAuth2TokenValidatorResult.success();
	}

	private UUID parseSubject(String subject) {
		if (!StringUtils.hasText(subject)) {
			return null;
		}
		try {
			return UUID.fromString(subject);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
