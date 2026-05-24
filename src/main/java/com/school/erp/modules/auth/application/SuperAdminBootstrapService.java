package com.school.erp.modules.auth.application;

import com.school.erp.common.exception.BusinessException;
import com.school.erp.common.exception.ErrorCode;
import com.school.erp.common.exception.ResourceNotFoundException;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.RoleName;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.RoleRepository;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SuperAdminBootstrapService {

	private final SuperAdminBootstrapProperties properties;
	private final UserAccountRepository userAccountRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;

	@EventListener(ApplicationReadyEvent.class)
	@Transactional
	public void bootstrap() {
		if (!properties.enabled()) {
			return;
		}
		validateProperties();
		if (userAccountRepository.existsByEmailIgnoreCaseAndDeletedFalse(properties.email())) {
			return;
		}

		Role superAdminRole = roleRepository.findByNameAndDeletedFalse(RoleName.SUPER_ADMIN)
				.orElseThrow(() -> new ResourceNotFoundException("Role", RoleName.SUPER_ADMIN));
		UserAccount user = new UserAccount(
				properties.email(),
				properties.email(),
				passwordEncoder.encode(properties.password()),
				properties.firstName(),
				properties.lastName());
		user.addRole(superAdminRole);
		userAccountRepository.save(user);
	}

	private void validateProperties() {
		if (!StringUtils.hasText(properties.email()) || !StringUtils.hasText(properties.password())) {
			throw new BusinessException(
					ErrorCode.VALIDATION_ERROR,
					"Bootstrap super admin email and password are required when bootstrap is enabled.");
		}
		if (properties.password().length() < 12) {
			throw new BusinessException(
					ErrorCode.PASSWORD_POLICY_VIOLATION,
					"Bootstrap super admin password must be at least 12 characters.");
		}
	}
}
