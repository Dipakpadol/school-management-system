package com.school.erp.modules.students.domain;

import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.util.StringUtils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "student_documents")
@SQLRestriction("deleted = false")
public class StudentDocument extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 40)
	private DocumentType documentType;

	@Column(name = "document_number", length = 80)
	private String documentNumber;

	@Column(name = "file_name", nullable = false, length = 180)
	private String fileName;

	@Column(name = "content_type", length = 120)
	private String contentType;

	@Column(name = "file_size")
	private Long fileSize;

	@Column(name = "storage_key", length = 300)
	private String storageKey;

	@Column(name = "file_url", length = 500)
	private String fileUrl;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 30)
	private DocumentVerificationStatus verificationStatus = DocumentVerificationStatus.PENDING;

	@Column(length = 500)
	private String remarks;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "verified_by", length = 100)
	private String verifiedBy;

	StudentDocument(
			Student student,
			DocumentType documentType,
			String documentNumber,
			String fileName,
			String contentType,
			Long fileSize,
			String storageKey,
			String fileUrl,
			String remarks) {
		this.student = student;
		this.documentType = documentType;
		this.documentNumber = trimToNull(documentNumber);
		this.fileName = fileName;
		this.contentType = trimToNull(contentType);
		this.fileSize = fileSize;
		this.storageKey = trimToNull(storageKey);
		this.fileUrl = trimToNull(fileUrl);
		this.remarks = trimToNull(remarks);
	}

	public void verify(String actor) {
		verificationStatus = DocumentVerificationStatus.VERIFIED;
		verifiedAt = Instant.now();
		verifiedBy = actor;
	}

	public void update(
			DocumentType documentType,
			String documentNumber,
			String fileName,
			String contentType,
			Long fileSize,
			String storageKey,
			String fileUrl,
			String remarks) {
		this.documentType = documentType;
		this.documentNumber = trimToNull(documentNumber);
		this.fileName = fileName;
		this.contentType = trimToNull(contentType);
		this.fileSize = fileSize;
		this.storageKey = trimToNull(storageKey);
		this.fileUrl = trimToNull(fileUrl);
		this.remarks = trimToNull(remarks);
		this.verificationStatus = DocumentVerificationStatus.PENDING;
		this.verifiedAt = null;
		this.verifiedBy = null;
	}

	public void reject(String actor, String remarks) {
		verificationStatus = DocumentVerificationStatus.REJECTED;
		verifiedAt = Instant.now();
		verifiedBy = actor;
		this.remarks = trimToNull(remarks);
	}

	private String trimToNull(String value) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		return value.trim();
	}
}
