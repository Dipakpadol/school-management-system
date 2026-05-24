package com.school.erp.modules.users.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "user_accounts")
@SQLRestriction("deleted = false")
public class UserAccount extends BaseEntity {

	@Column(nullable = false, unique = true, length = 160)
	private String email;

	@Column(nullable = false, unique = true, length = 80)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 120)
	private String passwordHash;

	@Column(name = "first_name", nullable = false, length = 80)
	private String firstName;

	@Column(name = "last_name", length = 80)
	private String lastName;

	@Column(name = "phone_number", length = 30)
	private String phoneNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private UserStatus status = UserStatus.ACTIVE;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts;

	@Column(name = "locked_until")
	private Instant lockedUntil;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Column(name = "password_changed_at")
	private Instant passwordChangedAt;

	@ManyToMany(fetch = FetchType.EAGER)
	@JoinTable(
			name = "user_roles",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "role_id"))
	private Set<Role> roles = new LinkedHashSet<>();

	public UserAccount(String email, String username, String passwordHash, String firstName, String lastName) {
		this.email = normalize(email);
		this.username = normalizeUsername(username);
		this.passwordHash = passwordHash;
		this.firstName = firstName;
		this.lastName = lastName;
		this.passwordChangedAt = Instant.now();
	}

	public String getDisplayName() {
		if (StringUtils.hasText(lastName)) {
			return firstName + " " + lastName;
		}
		return firstName;
	}

	public void addRole(Role role) {
		roles.add(role);
	}

	public void removeRole(RoleName roleName) {
		roles.removeIf(role -> role.getName() == roleName);
	}

	public boolean hasRole(RoleName roleName) {
		return roles.stream().anyMatch(role -> role.getName() == roleName);
	}

	public void replaceRoles(Set<Role> roles) {
		this.roles.clear();
		this.roles.addAll(roles);
	}

	public void updateProfile(
			String email,
			String username,
			String firstName,
			String lastName,
			String phoneNumber) {
		this.email = normalize(email);
		this.username = normalizeUsername(username);
		this.firstName = firstName;
		this.lastName = trimToNull(lastName);
		this.phoneNumber = trimToNull(phoneNumber);
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public boolean isLocked() {
		return status == UserStatus.LOCKED || (lockedUntil != null && lockedUntil.isAfter(Instant.now()));
	}

	public boolean canAuthenticate() {
		return isActive() && !isLocked() && !isDeleted();
	}

	public void recordSuccessfulLogin() {
		failedLoginAttempts = 0;
		lockedUntil = null;
		lastLoginAt = Instant.now();
	}

	public void recordFailedLogin(int maxAttempts, java.time.Duration lockDuration) {
		failedLoginAttempts++;
		if (failedLoginAttempts >= maxAttempts) {
			status = UserStatus.LOCKED;
			lockedUntil = Instant.now().plus(lockDuration);
		}
	}

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
		this.passwordChangedAt = Instant.now();
	}

	public void activate() {
		status = UserStatus.ACTIVE;
		lockedUntil = null;
		failedLoginAttempts = 0;
	}

	public void deactivate() {
		status = UserStatus.DISABLED;
		lockedUntil = null;
	}

	private String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase();
	}

	private String normalizeUsername(String value) {
		return value == null ? null : value.trim();
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
