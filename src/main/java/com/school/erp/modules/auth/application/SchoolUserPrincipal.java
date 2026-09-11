package com.school.erp.modules.auth.application;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.UserAccount;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

public class SchoolUserPrincipal implements UserDetails {

	@Getter
	private final UUID id;
	@Getter
	private final String email;
	private final String username;
	private final String password;
	private final boolean enabled;
	private final boolean accountNonLocked;
	private final Set<GrantedAuthority> authorities;

	private SchoolUserPrincipal(
			UUID id,
			String email,
			String username,
			String password,
			boolean enabled,
			boolean accountNonLocked,
			Set<GrantedAuthority> authorities) {
		this.id = id;
		this.email = email;
		this.username = username;
		this.password = password;
		this.enabled = enabled;
		this.accountNonLocked = accountNonLocked;
		this.authorities = Set.copyOf(authorities);
	}

	public static SchoolUserPrincipal from(UserAccount user) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>();
		for (Role role : user.getRoles()) {
			if (!role.isActive()) {
				continue;
			}
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
			for (Permission permission : role.getPermissions()) {
				if (permission.isActive()) {
					authorities.add(new SimpleGrantedAuthority(permission.getCode()));
				}
			}
		}
		return new SchoolUserPrincipal(
				user.getId(),
				user.getEmail(),
				user.getUsername(),
				user.getPasswordHash(),
				user.isActive(),
				!user.isLocked(),
				authorities);
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}

	public String getAccountUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
