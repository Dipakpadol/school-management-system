package com.school.erp.common.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(nullable = false, updatable = false)
	private UUID id;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@CreatedBy
	@Column(name = "created_by", length = 100, updatable = false)
	private String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by", length = 100)
	private String updatedBy;

	@Column(nullable = false)
	private boolean deleted;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "deleted_by", length = 100)
	private String deletedBy;

	@Version
	@Column(nullable = false)
	private long version;

	public void softDelete(String actor) {
		if (!deleted) {
			deleted = true;
			deletedAt = Instant.now();
			deletedBy = actor;
		}
	}

	public void restore() {
		deleted = false;
		deletedAt = null;
		deletedBy = null;
	}
}
