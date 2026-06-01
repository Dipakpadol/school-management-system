package com.school.erp.modules.auth.api;

import java.util.UUID;

import com.school.erp.common.api.ApiResponse;
import com.school.erp.common.web.CorrelationIdFilter;
import com.school.erp.modules.auth.api.dto.AuthenticationResponse;
import com.school.erp.modules.auth.api.dto.ChangePasswordRequest;
import com.school.erp.modules.auth.api.dto.CurrentUserResponse;
import com.school.erp.modules.auth.api.dto.ForgotPasswordRequest;
import com.school.erp.modules.auth.api.dto.ForgotPasswordResponse;
import com.school.erp.modules.auth.api.dto.LoginRequest;
import com.school.erp.modules.auth.api.dto.LogoutRequest;
import com.school.erp.modules.auth.api.dto.RefreshTokenRequest;
import com.school.erp.modules.auth.api.dto.ResetPasswordRequest;
import com.school.erp.modules.auth.api.dto.SignupRequest;
import com.school.erp.modules.auth.api.dto.SignupResponse;
import com.school.erp.modules.auth.application.AuthService;
import com.school.erp.modules.auth.application.ClientRequestInfo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<SignupResponse>> signup(
			@Valid @RequestBody SignupRequest request,
			HttpServletRequest httpRequest) {
		SignupResponse response = authService.signup(request, ClientRequestInfo.from(httpRequest));
		return created(response, response.message(), httpRequest);
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
			@Valid @RequestBody LoginRequest request,
			HttpServletRequest httpRequest) {
		AuthenticationResponse response = authService.login(request, ClientRequestInfo.from(httpRequest));
		return ok(response, "Login successful", httpRequest);
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthenticationResponse>> refresh(
			@Valid @RequestBody RefreshTokenRequest request,
			HttpServletRequest httpRequest) {
		AuthenticationResponse response = authService.refresh(request, ClientRequestInfo.from(httpRequest));
		return ok(response, "Token refreshed", httpRequest);
	}

	@PostMapping("/logout")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<Void>> logout(
			@Valid @RequestBody LogoutRequest request,
			HttpServletRequest httpRequest) {
		authService.logout(request);
		return ok(null, "Logout successful", httpRequest);
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
			@Valid @RequestBody ForgotPasswordRequest request,
			HttpServletRequest httpRequest) {
		ForgotPasswordResponse response = authService.forgotPassword(request, ClientRequestInfo.from(httpRequest));
		return ok(response, "If the account exists, password reset instructions have been sent.", httpRequest);
	}

	@PostMapping("/reset-password")
	public ResponseEntity<ApiResponse<Void>> resetPassword(
			@Valid @RequestBody ResetPasswordRequest request,
			HttpServletRequest httpRequest) {
		authService.resetPassword(request);
		return ok(null, "Password reset successful", httpRequest);
	}

	@PostMapping("/change-password")
	@PreAuthorize("hasAuthority('AUTH_PASSWORD_CHANGE')")
	public ResponseEntity<ApiResponse<Void>> changePassword(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody ChangePasswordRequest request,
			HttpServletRequest httpRequest) {
		authService.changePassword(UUID.fromString(jwt.getSubject()), request);
		return ok(null, "Password changed successfully", httpRequest);
	}

	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','PRINCIPAL','TEACHER','ACCOUNTANT','RECEPTIONIST','STUDENT','PARENT','WARDEN')")
	public ResponseEntity<ApiResponse<CurrentUserResponse>> me(
			@AuthenticationPrincipal Jwt jwt,
			HttpServletRequest httpRequest) {
		var response = new CurrentUserResponse(
				UUID.fromString(jwt.getSubject()),
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("username"),
				listClaim(jwt, "roles"),
				listClaim(jwt, "authorities"));
		return ok(response, "Authenticated user context", httpRequest);
	}

	private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message, HttpServletRequest request) {
		return ResponseEntity.ok(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}

	private <T> ResponseEntity<ApiResponse<T>> created(T data, String message, HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
				data,
				message,
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID)));
	}

	private java.util.List<String> listClaim(Jwt jwt, String claimName) {
		java.util.List<String> values = jwt.getClaimAsStringList(claimName);
		return values == null ? java.util.List.of() : values;
	}
}
