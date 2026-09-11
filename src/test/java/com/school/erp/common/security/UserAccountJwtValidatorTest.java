package com.school.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

class UserAccountJwtValidatorTest {

	@Test
	void rejectsTokenForDisabledUser() {
		UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
		UserAccount user = user();
		user.deactivate();
		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		var result = new UserAccountJwtValidator(userAccountRepository).validate(jwt(user, Instant.now()));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void rejectsTokenIssuedBeforePasswordChange() {
		UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
		UserAccount user = user();
		Instant issuedAt = Instant.now().minusSeconds(1);
		user.changePassword("new-hash");
		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		var result = new UserAccountJwtValidator(userAccountRepository).validate(jwt(user, issuedAt));

		assertThat(result.hasErrors()).isTrue();
	}

	@Test
	void acceptsActiveUserWithCurrentPasswordVersion() {
		UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
		UserAccount user = user();
		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		var result = new UserAccountJwtValidator(userAccountRepository).validate(jwt(user, Instant.now()));

		assertThat(result.hasErrors()).isFalse();
	}

	private Jwt jwt(UserAccount user, Instant issuedAt) {
		return new Jwt(
				"token",
				issuedAt,
				issuedAt.plusSeconds(300),
				Map.of("alg", "HS256"),
				Map.of("sub", user.getId().toString()));
	}

	private UserAccount user() {
		UserAccount user = new UserAccount(
				"admin@school.test",
				"admin",
				"hash",
				"Admin",
				"User");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}
}
