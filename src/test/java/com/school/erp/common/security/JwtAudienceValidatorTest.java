package com.school.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceValidatorTest {

	@Test
	void acceptsConfiguredAudience() {
		var validator = new JwtAudienceValidator(List.of("school-erp-api"));
		var jwt = jwtWithAudience(List.of("school-erp-api"));

		assertThat(validator.validate(jwt).hasErrors()).isFalse();
	}

	@Test
	void rejectsUnknownAudience() {
		var validator = new JwtAudienceValidator(List.of("school-erp-api"));
		var jwt = jwtWithAudience(List.of("other-api"));

		assertThat(validator.validate(jwt).hasErrors()).isTrue();
	}

	private Jwt jwtWithAudience(List<String> audience) {
		return new Jwt(
				"token",
				Instant.now(),
				Instant.now().plusSeconds(300),
				Map.of("alg", "HS256"),
				Map.of("sub", "user-1", "aud", audience));
	}
}
