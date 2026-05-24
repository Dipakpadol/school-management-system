package com.school.erp.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.school.erp.common.security.JwtProperties;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

	@Test
	void generatesAccessTokenWithRolesAndPermissionAuthorities() {
		String secret = "test-secret-test-secret-test-secret-32";
		var properties = new JwtProperties(
				"school-erp-test",
				List.of("school-erp-api"),
				secret,
				Duration.ofMinutes(15),
				Duration.ofDays(7),
				Duration.ofMinutes(30));
		var secretKey = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
		var service = new JwtService(encoder, properties);
		UserAccount user = userWithRole();

		GeneratedAccessToken accessToken = service.generateAccessToken(user);

		var decoder = NimbusJwtDecoder
				.withSecretKey(secretKey)
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		var jwt = decoder.decode(accessToken.value());

		assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
		assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@school.test");
		assertThat(jwt.getAudience()).containsExactly("school-erp-api");
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
		assertThat(jwt.getClaimAsStringList("authorities")).containsExactly("USERS_READ");
		assertThat(accessToken.expiresAt()).isAfter(jwt.getIssuedAt());
	}

	private UserAccount userWithRole() {
		UserAccount user = new UserAccount(
				"admin@school.test",
				"admin",
				"hash",
				"Admin",
				"User");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		Role role = new Role(RoleName.ADMIN, "Admin", "Administrator");
		role.addPermission(new Permission("USERS_READ", "Read users", "Read users"));
		user.addRole(role);
		return user;
	}
}
