package com.school.erp.modules.auth.application;

import java.util.LinkedHashMap;
import java.util.Map;

import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthenticationFailureService {

	private static final String MODULE_NAME = "AUTH";
	private static final String ENTITY_NAME = "UserAccount";

	private final UserAccountRepository userAccountRepository;
	private final AuditLogService auditLogService;
	private final AuthProperties authProperties;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordFailedLogin(String email, ClientRequestInfo client) {
		String attemptedEmail = normalizeEmail(email);
		userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(attemptedEmail)
				.ifPresentOrElse(
						user -> recordKnownFailedLogin(user, attemptedEmail, client),
						() -> audit(null, attemptedEmail, null, failureMetadata(null, attemptedEmail, "UNKNOWN_ACCOUNT", client), client));
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordBlockedLogin(UserAccount user, String reason, ClientRequestInfo client) {
		audit(
				user,
				user.getEmail(),
				null,
				failureMetadata(user, user.getEmail(), reason, client),
				client);
	}

	private void recordKnownFailedLogin(UserAccount user, String attemptedEmail, ClientRequestInfo client) {
		user.unlockIfTemporaryLockExpired();
		Map<String, Object> oldValue = accountState(user);
		user.recordFailedLogin(authProperties.maxFailedLoginAttempts(), authProperties.lockDuration());
		userAccountRepository.save(user);
		audit(
				user,
				attemptedEmail,
				oldValue,
				failureMetadata(user, attemptedEmail, "BAD_CREDENTIALS", client),
				client);
	}

	private Map<String, Object> accountState(UserAccount user) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("failedLoginAttempts", user.getFailedLoginAttempts());
		metadata.put("status", user.getStatus().name());
		metadata.put("lockedUntil", user.getLockedUntil());
		return metadata;
	}

	private Map<String, Object> failureMetadata(UserAccount user, String attemptedEmail, String reason, ClientRequestInfo client) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		if (user != null) {
			metadata.putAll(accountState(user));
			metadata.put("userId", user.getId() == null ? null : user.getId().toString());
		}
		metadata.put("email", attemptedEmail);
		metadata.put("success", false);
		metadata.put("reason", reason);
		if (client != null) {
			metadata.put("ipAddress", client.ipAddress());
			metadata.put("userAgent", client.userAgent());
		}
		return metadata;
	}

	private void audit(
			UserAccount user,
			String attemptedEmail,
			Object oldValue,
			Object newValue,
			ClientRequestInfo client) {
		auditLogService.recordAs(
				new AuditLogEvent(
						MODULE_NAME,
						ENTITY_NAME,
						user == null || user.getId() == null ? null : user.getId().toString(),
						AuditAction.LOGIN_FAILED,
						oldValue,
						newValue),
				StringUtils.hasText(attemptedEmail) ? attemptedEmail : "anonymous",
				client == null ? null : client.ipAddress());
	}

	private String normalizeEmail(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}
}
