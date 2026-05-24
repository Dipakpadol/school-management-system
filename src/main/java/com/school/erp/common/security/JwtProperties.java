package com.school.erp.common.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
		String issuer,
		List<String> audiences,
		String secret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl,
		Duration passwordResetTokenTtl) {

	public JwtProperties {
		issuer = StringUtils.hasText(issuer) ? issuer : "school-erp";
		audiences = audiences == null ? List.of() : List.copyOf(audiences);
		accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
		refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
		passwordResetTokenTtl = passwordResetTokenTtl == null ? Duration.ofMinutes(30) : passwordResetTokenTtl;
		if (!StringUtils.hasText(secret)) {
			throw new IllegalArgumentException("app.security.jwt.secret is required.");
		}
	}
}
