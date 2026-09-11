package com.school.erp.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.audit.application.AuditAction;
import com.school.erp.common.audit.application.AuditLogEvent;
import com.school.erp.common.audit.application.AuditLogService;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.domain.UserStatus;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationFailureServiceTest {

	@Mock
	private UserAccountRepository userAccountRepository;

	@Mock
	private AuditLogService auditLogService;

	private AuthenticationFailureService service;

	@BeforeEach
	void setUp() {
		service = new AuthenticationFailureService(
				userAccountRepository,
				auditLogService,
				new AuthProperties(5, Duration.ofMinutes(15), RoleName.PARENT, UserStatus.ACTIVE));
	}

	@Test
	void recordFailedLoginLocksKnownUserAtMaxAttemptsAndAuditsFailure() {
		UserAccount user = user();
		ReflectionTestUtils.setField(user, "failedLoginAttempts", 4);
		when(userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse("admin@school.test"))
				.thenReturn(Optional.of(user));

		service.recordFailedLogin("Admin@School.Test", new ClientRequestInfo("127.0.0.1", "JUnit"));

		assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
		assertThat(user.getStatus()).isEqualTo(UserStatus.LOCKED);
		assertThat(user.getLockedUntil()).isNotNull();
		verify(userAccountRepository).save(user);

		ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
		verify(auditLogService).recordAs(eventCaptor.capture(), eq("admin@school.test"), eq("127.0.0.1"));
		assertThat(eventCaptor.getValue().action()).isEqualTo(AuditAction.LOGIN_FAILED);
		assertThat(eventCaptor.getValue().entityId()).isEqualTo(user.getId().toString());
		Map<?, ?> metadata = (Map<?, ?>) eventCaptor.getValue().newValue();
		assertThat(metadata.get("success")).isEqualTo(false);
		assertThat(metadata.get("reason")).isEqualTo("BAD_CREDENTIALS");
		assertThat(metadata.get("failedLoginAttempts")).isEqualTo(5);
	}

	@Test
	void recordFailedLoginAuditsUnknownUserWithoutMutatingAccounts() {
		when(userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse("missing@school.test"))
				.thenReturn(Optional.empty());

		service.recordFailedLogin("missing@school.test", new ClientRequestInfo("127.0.0.1", "JUnit"));

		ArgumentCaptor<AuditLogEvent> eventCaptor = ArgumentCaptor.forClass(AuditLogEvent.class);
		verify(auditLogService).recordAs(eventCaptor.capture(), eq("missing@school.test"), eq("127.0.0.1"));
		assertThat(eventCaptor.getValue().entityId()).isNull();
		Map<?, ?> metadata = (Map<?, ?>) eventCaptor.getValue().newValue();
		assertThat(metadata.get("success")).isEqualTo(false);
		assertThat(metadata.get("reason")).isEqualTo("UNKNOWN_ACCOUNT");
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
