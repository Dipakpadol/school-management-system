package com.school.erp.modules.auth.application;

import java.time.Instant;
import java.util.UUID;

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
import com.school.erp.modules.auth.domain.PasswordResetToken;
import com.school.erp.modules.auth.domain.RefreshToken;
import com.school.erp.modules.auth.infrastructure.PasswordResetTokenRepository;
import com.school.erp.modules.auth.infrastructure.RefreshTokenRepository;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final UserAccountRepository userAccountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final SecureTokenService secureTokenService;
	private final AuthMapper authMapper;
	private final AuthProperties authProperties;
	private final com.school.erp.common.security.JwtProperties jwtProperties;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public AuthenticationResponse login(LoginRequest request, ClientRequestInfo client) {
		UserAccount user = userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
				.orElseThrow(() -> invalidCredentials());
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		}
		catch (AuthenticationException ex) {
			user.recordFailedLogin(authProperties.maxFailedLoginAttempts(), authProperties.lockDuration());
			throw invalidCredentials();
		}

		if (!user.canAuthenticate()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "User account is not active.");
		}

		user.recordSuccessfulLogin();
		return issueSession(user, client);
	}

	@Transactional
	public void logout(LogoutRequest request) {
		String tokenHash = secureTokenService.hash(request.refreshToken());
		refreshTokenRepository.findByTokenHashAndDeletedFalse(tokenHash).ifPresent(RefreshToken::revoke);
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
		if (!user.canAuthenticate()) {
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
		userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email())
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

	private BusinessException invalidCredentials() {
		return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
	}
}
