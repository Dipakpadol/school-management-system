package com.school.erp.modules.auth.application;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.auth.api.dto.AuthenticationResponse;
import com.school.erp.modules.auth.api.dto.ChangePasswordRequest;
import com.school.erp.modules.auth.api.dto.ForgotPasswordRequest;
import com.school.erp.modules.auth.api.dto.ForgotPasswordResponse;
import com.school.erp.modules.auth.api.dto.LoginRequest;
import com.school.erp.modules.auth.api.dto.LogoutRequest;
import com.school.erp.modules.auth.api.dto.RefreshTokenRequest;
import com.school.erp.modules.auth.api.dto.ResetPasswordRequest;
import com.school.erp.modules.auth.api.dto.SignupRequest;
import com.school.erp.modules.auth.api.dto.SignupResponse;
import com.school.erp.modules.auth.domain.PasswordResetToken;
import com.school.erp.modules.auth.domain.RefreshToken;
import com.school.erp.modules.auth.infrastructure.PasswordResetTokenRepository;
import com.school.erp.modules.auth.infrastructure.RefreshTokenRepository;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.domain.UserSource;
import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SecureTokenService secureTokenService;
	private final AuthMapper authMapper;
	private final AuthenticationFailureService authenticationFailureService;
	private final AuthProperties authProperties;
	private final com.school.erp.common.security.JwtProperties jwtProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final AuditLogService auditLogService;

	private static final java.util.Set<RoleName> PUBLIC_SIGNUP_BLOCKED_ROLES = java.util.Set.of(
			RoleName.SUPER_ADMIN,
			RoleName.ADMIN,
			RoleName.PRINCIPAL,
			RoleName.ACCOUNTANT,
			RoleName.TEACHER);

	@Transactional
	public SignupResponse signup(SignupRequest request, ClientRequestInfo client) {
		validatePasswordConfirmation(request.password(), request.confirmPassword());
		String email = normalizeEmail(request.email());
		String mobileNumber = trimToNull(request.mobileNumber());
		validatePublicSignupUniqueness(email, mobileNumber);
		RoleName signupRole = request.role();

		if (PUBLIC_SIGNUP_BLOCKED_ROLES.contains(signupRole)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_RULE_VIOLATION,
					"Selected role is not allowed for public sign-up.");
		}
		Role role = roleRepository.findByNameAndDeletedFalse(signupRole)
				.orElseThrow(() -> new ResourceNotFoundException("Role", signupRole));

		UserAccount user = new UserAccount(
				email,
				email,
				passwordEncoder.encode(request.password()),
				request.firstName().trim(),
				request.lastName().trim());
		user.updateProfile(
				email,
				email,
				request.firstName().trim(),
				trimToNull(request.middleName()),
				request.lastName().trim(),
				mobileNumber);
		user.markSource(UserSource.SIGN_UP);
		user.changeStatus(authProperties.signupDefaultStatus());
		user.addRole(role);
		UserAccount saved = userAccountRepository.save(user);

		auditAuth(saved, "SIGN_UP", null, authMetadata(saved, client), client);
		String message = saved.getStatus() == UserStatus.ACTIVE
				? "Registration successful. You can now login."
				: "Registration successful. Please wait for admin approval.";
		return new SignupResponse(saved.getId(), saved.getEmail(), signupRole, saved.getStatus(), message);
	}

	@Transactional
	public AuthenticationResponse login(LoginRequest request, ClientRequestInfo client) {
		String email = normalizeEmail(request.email());
		UserAccount user = userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(email)
				.orElse(null);
		if (user == null) {
			authenticationFailureService.recordFailedLogin(email, client);
			throw invalidCredentials();
		}

		user.unlockIfTemporaryLockExpired();
		if (!user.canAuthenticate()) {
			authenticationFailureService.recordBlockedLogin(user, loginBlockedReason(user), client);
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "User account cannot authenticate.");
		}

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, request.password()));
		}
		catch (AuthenticationException ex) {
			authenticationFailureService.recordFailedLogin(email, client);
			throw invalidCredentials();
		}

		user.recordSuccessfulLogin();
		AuthenticationResponse response = issueSession(user, client);
		auditAuth(user, AuditAction.LOGIN, null, authMetadata(user, client), client);
		return response;
	}

	@Transactional
	public void logout(LogoutRequest request) {
		String tokenHash = secureTokenService.hash(request.refreshToken());
		refreshTokenRepository.findByTokenHashAndDeletedFalse(tokenHash).ifPresent(token -> {
			token.revoke();
			auditAuth(token.getUser(), AuditAction.LOGOUT, null, authMetadata(token.getUser(), null), null);
		});
	}

	@Transactional
	public AuthenticationResponse refresh(RefreshTokenRequest request, ClientRequestInfo client) {
		String oldTokenHash = secureTokenService.hash(request.refreshToken());
		RefreshToken oldToken = refreshTokenRepository.findByTokenHashAndDeletedFalse(oldTokenHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

		if (oldToken.isExpired()) {
			oldToken.revoke();
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		}
		if (oldToken.isRevoked()) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		UserAccount user = oldToken.getUser();
		user.unlockIfTemporaryLockExpired();
		if (!user.canAuthenticate()) {
			oldToken.revoke();
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "User account is not active.");
		}

		GeneratedRefreshToken newRefreshToken = generateRefreshToken();
		oldToken.revokeAndReplaceWith(newRefreshToken.hash());
		refreshTokenRepository.save(new RefreshToken(
				user,
				newRefreshToken.hash(),
				newRefreshToken.expiresAt(),
				client.ipAddress(),
				client.userAgent()));
		GeneratedAccessToken accessToken = jwtService.generateAccessToken(user);
		return authMapper.toAuthenticationResponse(user, accessToken, newRefreshToken.value(), newRefreshToken.expiresAt());
	}

	@Transactional
	public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request, ClientRequestInfo client) {
		String lookup = forgotPasswordLookup(request);
		findUserForPasswordReset(lookup)
				.filter(UserAccount::isActive)
				.ifPresent(user -> {
					GeneratedRefreshToken token = generatePasswordResetToken();
					passwordResetTokenRepository.save(new PasswordResetToken(
							user,
							token.hash(),
							token.expiresAt(),
							client.ipAddress()));
					eventPublisher.publishEvent(new PasswordResetRequestedEvent(
							user.getId(),
							user.getEmail(),
							user.getDisplayName(),
							token.value(),
							token.expiresAt()));
					auditAuth(user, "FORGOT_PASSWORD_REQUEST", null, authMetadata(user, client), client);
				});
		return new ForgotPasswordResponse(true);
	}

	@Transactional
	public void resetPassword(ResetPasswordRequest request) {
		validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
		String tokenHash = secureTokenService.hash(request.token());
		PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndDeletedFalse(tokenHash)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

		if (resetToken.isExpired()) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		}
		if (resetToken.isUsed()) {
			throw new BusinessException(ErrorCode.INVALID_TOKEN);
		}

		UserAccount user = resetToken.getUser();
		user.changePassword(passwordEncoder.encode(request.newPassword()));
		if (user.getStatus() == UserStatus.LOCKED) {
			user.activate();
		}
		resetToken.markUsed();
		refreshTokenRepository.revokeActiveTokensForUser(user.getId(), Instant.now());
		auditAuth(user, "PASSWORD_RESET", null, Map.of("userId", user.getId().toString(), "email", user.getEmail()), null);
	}

	@Transactional
	public void changePassword(UUID userId, ChangePasswordRequest request) {
		validatePasswordConfirmation(request.newPassword(), request.confirmPassword());
		UserAccount user = userAccountRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User", userId));

		if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Current password is incorrect.");
		}
		if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
			throw new BusinessException(
					ErrorCode.PASSWORD_POLICY_VIOLATION,
					"New password must be different from the current password.");
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshTokenRepository.revokeActiveTokensForUser(userId, Instant.now());
		auditAuth(user, "PASSWORD_CHANGE", null, Map.of("userId", user.getId().toString(), "email", user.getEmail()), null);
	}

	private AuthenticationResponse issueSession(UserAccount user, ClientRequestInfo client) {
		GeneratedAccessToken accessToken = jwtService.generateAccessToken(user);
		GeneratedRefreshToken refreshToken = generateRefreshToken();
		refreshTokenRepository.save(new RefreshToken(
				user,
				refreshToken.hash(),
				refreshToken.expiresAt(),
				client.ipAddress(),
				client.userAgent()));
		return authMapper.toAuthenticationResponse(user, accessToken, refreshToken.value(), refreshToken.expiresAt());
	}

	private GeneratedRefreshToken generateRefreshToken() {
		String token = secureTokenService.generateOpaqueToken();
		return new GeneratedRefreshToken(
				token,
				secureTokenService.hash(token),
				Instant.now().plus(jwtProperties.refreshTokenTtl()));
	}

	private GeneratedRefreshToken generatePasswordResetToken() {
		String token = secureTokenService.generateOpaqueToken();
		return new GeneratedRefreshToken(
				token,
				secureTokenService.hash(token),
				Instant.now().plus(jwtProperties.passwordResetTokenTtl()));
	}

	private void validatePasswordConfirmation(String password, String confirmPassword) {
		if (!password.equals(confirmPassword)) {
			throw new BusinessException(
					ErrorCode.PASSWORD_POLICY_VIOLATION,
					"Password confirmation does not match.");
		}
	}

	private void validatePublicSignupUniqueness(String email, String mobileNumber) {
		if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)
				|| userAccountRepository.existsByUsernameIgnoreCaseAndDeletedFalse(email)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Email already exists: " + email);
		}
		if (StringUtils.hasText(mobileNumber)
				&& userAccountRepository.existsByPhoneNumberAndDeletedFalse(mobileNumber)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Mobile number already exists: " + mobileNumber);
		}
	}

	private java.util.Optional<UserAccount> findUserForPasswordReset(String lookup) {
		if (lookup.contains("@")) {
			return userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(lookup);
		}
		return userAccountRepository.findByPhoneNumberAndDeletedFalse(lookup);
	}

	private String forgotPasswordLookup(ForgotPasswordRequest request) {
		String lookup = StringUtils.hasText(request.emailOrMobile()) ? request.emailOrMobile() : request.email();
		if (!StringUtils.hasText(lookup)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email or mobile number is required.");
		}
		return normalizeEmail(lookup);
	}

	private String normalizeEmail(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}

	private BusinessException invalidCredentials() {
		return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
	}

	private String loginBlockedReason(UserAccount user) {
		if (user.isLocked()) {
			return "ACCOUNT_LOCKED";
		}
		return "ACCOUNT_" + user.getStatus().name();
	}

	private Map<String, Object> authMetadata(UserAccount user, ClientRequestInfo client) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("userId", user.getId() == null ? null : user.getId().toString());
		metadata.put("email", user.getEmail());
		if (client != null) {
			metadata.put("ipAddress", client.ipAddress());
			metadata.put("userAgent", client.userAgent());
		}
		return metadata;
	}

	private void auditAuth(UserAccount user, String action, Object oldValue, Object newValue, ClientRequestInfo client) {
		auditLogService.recordAs(
				new AuditLogEvent(
						"AUTH",
						"UserAccount",
						user.getId() == null ? null : user.getId().toString(),
						action,
						oldValue,
						newValue),
				user.getEmail(),
				client == null ? null : client.ipAddress());
	}
}
