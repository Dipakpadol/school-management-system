package com.school.erp.common.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.UserAccount;
import com.school.erp.modules.users.infrastructure.UserAccountRepository;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StringUtils;

public class SchoolJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
	private final UserAccountRepository userAccountRepository;

	public SchoolJwtGrantedAuthoritiesConverter() {
		this(null);
	}

	public SchoolJwtGrantedAuthoritiesConverter(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Override
	public Collection<GrantedAuthority> convert(Jwt source) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeAuthoritiesConverter.convert(source));
		if (addDatabaseAuthorities(source, authorities)) {
			return authorities;
		}
		addClaimAuthorities(source, authorities, "roles", "ROLE_");
		addClaimAuthorities(source, authorities, "authorities", "");
		return authorities;
	}

	private boolean addDatabaseAuthorities(Jwt source, Set<GrantedAuthority> authorities) {
		if (userAccountRepository == null) {
			return false;
		}
		UUID userId = parseSubject(source.getSubject());
		if (userId == null) {
			return true;
		}
		userAccountRepository.findWithRolesByIdAndDeletedFalse(userId)
				.ifPresent(user -> addUserAuthorities(user, authorities));
		return true;
	}

	private void addUserAuthorities(UserAccount user, Set<GrantedAuthority> authorities) {
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
	}

	private UUID parseSubject(String subject) {
		if (!StringUtils.hasText(subject)) {
			return null;
		}
		try {
			return UUID.fromString(subject);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private void addClaimAuthorities(Jwt source, Set<GrantedAuthority> authorities, String claimName, String prefix) {
		Object claim = source.getClaims().get(claimName);
		if (claim instanceof Collection<?> values) {
			values.stream()
					.map(Object::toString)
					.filter(StringUtils::hasText)
					.map(value -> prefixIfMissing(value, prefix))
					.map(SimpleGrantedAuthority::new)
					.forEach(authorities::add);
		}
		else if (claim instanceof String value && StringUtils.hasText(value)) {
			for (String authority : value.split(" ")) {
				if (StringUtils.hasText(authority)) {
					authorities.add(new SimpleGrantedAuthority(prefixIfMissing(authority, prefix)));
				}
			}
		}
	}

	private String prefixIfMissing(String value, String prefix) {
		if (!StringUtils.hasText(prefix) || value.startsWith(prefix)) {
			return value;
		}
		return prefix + value;
	}
}
