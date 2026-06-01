package com.school.erp.modules.auth.application;

import java.time.Duration;

import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserStatus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.auth")
public record AuthProperties(
		int maxFailedLoginAttempts,
		Duration lockDuration,
		RoleName signupDefaultRole,
		UserStatus signupDefaultStatus) {

	public AuthProperties {
		maxFailedLoginAttempts = maxFailedLoginAttempts <= 0 ? 5 : maxFailedLoginAttempts;
		lockDuration = lockDuration == null ? Duration.ofMinutes(15) : lockDuration;
		signupDefaultRole = signupDefaultRole == null ? RoleName.PARENT : signupDefaultRole;
		signupDefaultStatus = signupDefaultStatus == null ? UserStatus.ACTIVE : signupDefaultStatus;
	}
}
