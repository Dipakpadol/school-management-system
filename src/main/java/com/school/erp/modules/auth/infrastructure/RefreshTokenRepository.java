package com.school.erp.modules.auth.infrastructure;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.auth.domain.RefreshToken;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends BaseRepository<RefreshToken, UUID> {

	@EntityGraph(attributePaths = { "user", "user.roles", "user.roles.permissions" })
	Optional<RefreshToken> findByTokenHashAndDeletedFalse(String tokenHash);

	@Modifying
	@Query("""
			update RefreshToken token
			set token.revokedAt = :revokedAt
			where token.user.id = :userId
				and token.revokedAt is null
				and token.deleted = false
			""")
	int revokeActiveTokensForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
