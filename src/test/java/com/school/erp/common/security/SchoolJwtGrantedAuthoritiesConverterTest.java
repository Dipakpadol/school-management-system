package com.school.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SchoolJwtGrantedAuthoritiesConverterTest {

	@Test
	void mapsRolesAndPermissionsFromJwtClaims() {
		var converter = new SchoolJwtGrantedAuthoritiesConverter();
		var jwt = new Jwt(
				"token",
				Instant.now(),
				Instant.now().plusSeconds(300),
				Map.of("alg", "HS256"),
				Map.of(
						"sub", "user-1",
						"roles", List.of("ADMIN"),
						"authorities", List.of("USERS_READ", "STUDENTS_CREATE")));

		var authorities = converter.convert(jwt).stream()
				.map(authority -> authority.getAuthority())
				.toList();

		assertThat(authorities).contains("ROLE_ADMIN", "USERS_READ", "STUDENTS_CREATE");
	}
}
