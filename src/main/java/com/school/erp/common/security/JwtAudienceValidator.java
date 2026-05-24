package com.school.erp.common.security;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

	private final List<String> acceptedAudiences;

	public JwtAudienceValidator(List<String> acceptedAudiences) {
		this.acceptedAudiences = List.copyOf(acceptedAudiences);
	}

	@Override
	public OAuth2TokenValidatorResult validate(Jwt token) {
		boolean valid = token.getAudience().stream().anyMatch(acceptedAudiences::contains);
		if (valid) {
			return OAuth2TokenValidatorResult.success();
		}
		return OAuth2TokenValidatorResult.failure(new OAuth2Error(
				"invalid_token",
				"JWT audience is not accepted by this API",
				null));
	}
}
