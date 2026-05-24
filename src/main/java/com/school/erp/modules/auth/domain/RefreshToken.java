package com.school.erp.modules.auth.domain;

import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.users.domain.UserAccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_tokens")
@SQLRestriction("deleted = false")
public class RefreshToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	@Column(name = "replaced_by_token_hash", length = 128)
	private String replacedByTokenHash;

	@Column(name = "created_ip", length = 80)
	private String createdIp;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	public RefreshToken(UserAccount user, String tokenHash, Instant expiresAt, String createdIp, String userAgent) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.createdIp = createdIp;
		this.userAgent = userAgent;
	}

	public boolean isExpired() {
		return expiresAt.isBefore(Instant.now());
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isUsable() {
		return !isRevoked() && !isExpired();
	}

	public void revoke() {
		if (revokedAt == null) {
			revokedAt = Instant.now();
		}
	}

	public void revokeAndReplaceWith(String newTokenHash) {
		revoke();
		replacedByTokenHash = newTokenHash;
	}
}
