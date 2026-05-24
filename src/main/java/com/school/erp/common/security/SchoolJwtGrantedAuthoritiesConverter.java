package com.school.erp.common.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.util.StringUtils;

public class SchoolJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

	@Override
	public Collection<GrantedAuthority> convert(Jwt source) {
		Set<GrantedAuthority> authorities = new LinkedHashSet<>(scopeAuthoritiesConverter.convert(source));
		addClaimAuthorities(source, authorities, "roles", "ROLE_");
		addClaimAuthorities(source, authorities, "authorities", "");
		return authorities;
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
