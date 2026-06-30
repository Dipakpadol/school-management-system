package com.school.erp.modules.teachers.domain;

import java.time.Instant;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.Teacher;

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
@Table(name = "teacher_documents")
@SQLRestriction("deleted = false")
public class TeacherDocument extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "teacher_id", nullable = false)
	private Teacher teacher;

	@Column(name = "document_type", nullable = false, length = 80)
	private String documentType;

	@Column(name = "file_name", nullable = false, length = 180)
	private String fileName;

	@Column(name = "file_url", length = 500)
	private String fileUrl;

	@Column(name = "file_path", length = 500)
	private String filePath;

	@Column(name = "uploaded_at", nullable = false)
	private Instant uploadedAt = Instant.now();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private TeacherDocumentStatus status = TeacherDocumentStatus.ACTIVE;

	public TeacherDocument(
			Teacher teacher,
			String documentType,
			String fileName,
			String fileUrl,
			String filePath,
			TeacherDocumentStatus status) {
		this.teacher = teacher;
		update(documentType, fileName, fileUrl, filePath, status);
	}

	public void update(
			String documentType,
			String fileName,
			String fileUrl,
			String filePath,
			TeacherDocumentStatus status) {
		this.documentType = trim(documentType);
		this.fileName = trim(fileName);
		this.fileUrl = trimToNull(fileUrl);
		this.filePath = trimToNull(filePath);
		this.status = status == null ? TeacherDocumentStatus.ACTIVE : status;
	}

	private String trim(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	private String trimToNull(String value) {
		return trim(value);
	}
}
