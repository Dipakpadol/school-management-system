package com.school.erp.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.PermissionStatus;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

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

	@Test
	void mapsOnlyActiveDatabaseRolesAndPermissionsWhenRepositoryIsConfigured() {
		UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
		UserAccount user = user();
		Role activeRole = new Role(RoleName.ADMIN, "Admin", "Administrator");
		activeRole.addPermission(new Permission("USERS_READ", "Read users", "Read users"));
		Permission inactivePermission = new Permission("USERS_DELETE", "Delete users", "Delete users");
		inactivePermission.update("USERS_DELETE", "Delete users", "USERS", "Delete users", PermissionStatus.INACTIVE);
		activeRole.addPermission(inactivePermission);
		Role inactiveRole = new Role("Dormant Role", "Dormant Role", "Inactive role");
		inactiveRole.addPermission(new Permission("FEES_READ", "Read fees", "Read fees"));
		inactiveRole.deactivate();
		user.addRole(activeRole);
		user.addRole(inactiveRole);

		when(userAccountRepository.findWithRolesByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		var converter = new SchoolJwtGrantedAuthoritiesConverter(userAccountRepository);
		var jwt = new Jwt(
				"token",
				Instant.now(),
				Instant.now().plusSeconds(300),
				Map.of("alg", "HS256"),
				Map.of(
						"sub", user.getId().toString(),
						"roles", List.of("SUPER_ADMIN"),
						"authorities", List.of("USERS_DELETE")));

		var authorities = converter.convert(jwt).stream()
				.map(authority -> authority.getAuthority())
				.toList();

		assertThat(authorities).contains("ROLE_ADMIN", "USERS_READ");
		assertThat(authorities).doesNotContain("ROLE_SUPER_ADMIN", "ROLE_DORMANT_ROLE", "USERS_DELETE", "FEES_READ");
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
