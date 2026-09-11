package com.school.erp.modules.auth.application;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.school.erp.common.security.JwtProperties;
import com.school.erp.modules.users.domain.Permission;
import com.school.erp.modules.users.domain.Role;
import com.school.erp.modules.users.domain.UserAccount;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;

	public GeneratedAccessToken generateAccessToken(UserAccount user) {
		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl());

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuer(jwtProperties.issuer())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.subject(user.getId().toString())
				.claim("email", user.getEmail())
				.claim("username", user.getUsername())
				.claim("roles", roles(user))
				.claim("authorities", permissions(user));

		if (!jwtProperties.audiences().isEmpty()) {
			claims.audience(jwtProperties.audiences());
		}

		var headers = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims.build())).getTokenValue();
		return new GeneratedAccessToken(token, expiresAt);
	}

	public List<String> roles(UserAccount user) {
		Set<String> roles = new TreeSet<>();
		for (Role role : user.getRoles()) {
			if (role.isActive()) {
				roles.add(role.getName());
			}
		}
		return List.copyOf(roles);
	}

	public List<String> permissions(UserAccount user) {
		Set<String> permissions = new TreeSet<>();
		for (Role role : user.getRoles()) {
			if (!role.isActive()) {
				continue;
			}
			for (Permission permission : role.getPermissions()) {
				if (permission.isActive()) {
					permissions.add(permission.getCode());
				}
			}
		}
		return List.copyOf(permissions);
	}
}
