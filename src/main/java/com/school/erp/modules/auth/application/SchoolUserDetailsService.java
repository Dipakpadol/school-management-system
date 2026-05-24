package com.school.erp.modules.auth.application;

import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchoolUserDetailsService implements UserDetailsService {

	private final UserAccountRepository userAccountRepository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userAccountRepository.findByEmailIgnoreCaseAndDeletedFalse(username)
				.map(SchoolUserPrincipal::from)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}
}
