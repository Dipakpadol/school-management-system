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
@Table(name = "password_reset_tokens")
@SQLRestriction("deleted = false")
public class PasswordResetToken extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private UserAccount user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 128)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "used_at")
	private Instant usedAt;

	@Column(name = "requested_ip", length = 80)
	private String requestedIp;

	public PasswordResetToken(UserAccount user, String tokenHash, Instant expiresAt, String requestedIp) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
		this.requestedIp = requestedIp;
	}

	public boolean isExpired() {
		return expiresAt.isBefore(Instant.now());
	}

	public boolean isUsed() {
		return usedAt != null;
	}

	public boolean isUsable() {
		return !isUsed() && !isExpired();
	}

	public void markUsed() {
		usedAt = Instant.now();
	}
}
