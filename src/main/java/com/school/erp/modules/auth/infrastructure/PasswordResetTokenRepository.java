package com.school.erp.modules.auth.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.auth.domain.PasswordResetToken;

import org.springframework.data.jpa.repository.EntityGraph;

public interface PasswordResetTokenRepository extends BaseRepository<PasswordResetToken, UUID> {

	@EntityGraph(attributePaths = "user")
	Optional<PasswordResetToken> findByTokenHashAndDeletedFalse(String tokenHash);
}
