package com.school.erp.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.security.JwtProperties;
import com.school.erp.modules.auth.api.dto.AuthenticationResponse;
import com.school.erp.modules.auth.api.dto.ChangePasswordRequest;
import com.school.erp.modules.auth.api.dto.LoginRequest;
import com.school.erp.modules.auth.api.dto.RefreshTokenRequest;
import com.school.erp.modules.auth.domain.RefreshToken;
import com.school.erp.modules.auth.infrastructure.PasswordResetTokenRepository;
import com.school.erp.modules.auth.infrastructure.RefreshTokenRepository;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@Mock
	private PasswordResetTokenRepository passwordResetTokenRepository;

	@Mock
	private JwtService jwtService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
	private final SecureTokenService secureTokenService = new SecureTokenService();
	private AuthService authService;

	@BeforeEach
	void setUp() {
		var authMapper = new AuthMapper(jwtService);
		var jwtProperties = new JwtProperties(
				"school-erp-test",
				List.of("school-erp-api"),
				"test-secret-test-secret-test-secret-32",
				Duration.ofMinutes(15),
				Duration.ofDays(7),
				Duration.ofMinutes(30));
		authService = new AuthService(
				authenticationManager,
				userAccountRepository,
				refreshTokenRepository,
				passwordResetTokenRepository,
				passwordEncoder,
				jwtService,
				secureTokenService,
				authMapper,
				new AuthProperties(5, Duration.ofMinutes(15)),
				jwtProperties,
				eventPublisher);
	}

	@Test
	void loginAuthenticatesUserAndIssuesRefreshToken() {
		UserAccount user = user("Password123");
		when(userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse("admin@school.test")).thenReturn(Optional.of(user));
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenReturn(new TestingAuthenticationToken(user.getEmail(), null));
		when(jwtService.generateAccessToken(user))
				.thenReturn(new GeneratedAccessToken("access-token", Instant.now().plusSeconds(900)));
		when(jwtService.roles(user)).thenReturn(List.of("ADMIN"));
		when(jwtService.permissions(user)).thenReturn(List.of("USERS_READ"));
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuthenticationResponse response = authService.login(
				new LoginRequest("admin@school.test", "Password123"),
				new ClientRequestInfo("127.0.0.1", "JUnit"));

		assertThat(response.accessToken()).isEqualTo("access-token");
		assertThat(response.refreshToken()).isNotBlank();
		assertThat(response.user().roles()).containsExactly("ADMIN");
		assertThat(user.getLastLoginAt()).isNotNull();

		ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokenRepository).save(tokenCaptor.capture());
		assertThat(tokenCaptor.getValue().getUser()).isSameAs(user);
		assertThat(tokenCaptor.getValue().getTokenHash()).hasSize(64);
	}

	@Test
	void loginRecordsFailedAttemptWhenAuthenticationFails() {
		UserAccount user = user("Password123");
		when(userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse("admin@school.test")).thenReturn(Optional.of(user));
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("bad credentials"));

		assertThatThrownBy(() -> authService.login(
				new LoginRequest("admin@school.test", "wrong-password"),
				new ClientRequestInfo("127.0.0.1", "JUnit")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.INVALID_CREDENTIALS);

		assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
	}

	@Test
	void refreshRotatesRefreshTokenAndIssuesNewAccessToken() {
		UserAccount user = user("Password123");
		String rawRefreshToken = "existing-refresh-token";
		RefreshToken existingToken = new RefreshToken(
				user,
				secureTokenService.hash(rawRefreshToken),
				Instant.now().plusSeconds(3600),
				"127.0.0.1",
				"JUnit");
		when(refreshTokenRepository.findByTokenHashAndDeletedFalse(secureTokenService.hash(rawRefreshToken)))
				.thenReturn(Optional.of(existingToken));
		when(jwtService.generateAccessToken(user))
				.thenReturn(new GeneratedAccessToken("new-access-token", Instant.now().plusSeconds(900)));
		when(jwtService.roles(user)).thenReturn(List.of("ADMIN"));
		when(jwtService.permissions(user)).thenReturn(List.of("USERS_READ"));
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

		AuthenticationResponse response = authService.refresh(
				new RefreshTokenRequest(rawRefreshToken),
				new ClientRequestInfo("127.0.0.1", "JUnit"));

		assertThat(response.accessToken()).isEqualTo("new-access-token");
		assertThat(response.refreshToken()).isNotEqualTo(rawRefreshToken);
		assertThat(existingToken.isRevoked()).isTrue();
		assertThat(existingToken.getReplacedByTokenHash()).hasSize(64);
		verify(refreshTokenRepository).save(any(RefreshToken.class));
	}

	@Test
	void changePasswordRehashesPasswordAndRevokesRefreshTokens() {
		UserAccount user = user("OldPassword123");
		when(userAccountRepository.findByIdAndDeletedFalse(user.getId())).thenReturn(Optional.of(user));

		authService.changePassword(
				user.getId(),
				new ChangePasswordRequest("OldPassword123", "NewPassword123", "NewPassword123"));

		assertThat(passwordEncoder.matches("NewPassword123", user.getPasswordHash())).isTrue();
		verify(refreshTokenRepository).revokeActiveTokensForUser(any(UUID.class), any(Instant.class));
	}

	private UserAccount user(String rawPassword) {
		UserAccount user = new UserAccount(
				"admin@school.test",
				"admin",
				passwordEncoder.encode(rawPassword),
				"Admin",
				"User");
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		Role role = new Role(RoleName.ADMIN, "Admin", "Administrator");
		role.addPermission(new Permission("USERS_READ", "Read users", "Read users"));
		user.addRole(role);
		return user;
	}
}
