package com.school.erp.modules.documents.domain;

import java.time.LocalDate;

import com.school.erp.common.domain.BaseEntity;
import com.school.erp.modules.academic.domain.AcademicYear;
import com.school.erp.modules.students.domain.Student;

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
@Table(name = "generated_documents")
@SQLRestriction("deleted = false")
public class GeneratedDocument extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "academic_year_id", nullable = false)
	private AcademicYear academicYear;

	@Enumerated(EnumType.STRING)
	@Column(name = "document_type", nullable = false, length = 40)
	private GeneratedDocumentType documentType;

	@Column(name = "document_number", nullable = false, length = 80)
	private String documentNumber;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(length = 300)
	private String purpose;

	@Column(length = 500)
	private String remarks;

	@Column(name = "file_name", nullable = false, length = 180)
	private String fileName;

	@Column(name = "content_type", nullable = false, length = 120)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(nullable = false)
	private byte[] content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private GeneratedDocumentStatus status = GeneratedDocumentStatus.GENERATED;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reprint_of_document_id")
	private GeneratedDocument reprintOf;

	public GeneratedDocument(
			Student student,
			AcademicYear academicYear,
			GeneratedDocumentType documentType,
			String documentNumber,
			LocalDate issueDate,
			String purpose,
			String remarks,
			String fileName,
			String contentType,
			byte[] content) {
		this.student = student;
		this.academicYear = academicYear;
		this.documentType = documentType;
		this.documentNumber = trim(documentNumber);
		this.issueDate = issueDate == null ? LocalDate.now() : issueDate;
		this.purpose = trimToNull(purpose);
		this.remarks = trimToNull(remarks);
		this.fileName = trim(fileName);
		this.contentType = trim(contentType);
		this.content = content == null ? new byte[0] : content;
		this.fileSize = this.content.length;
		this.status = GeneratedDocumentStatus.GENERATED;
	}

	public GeneratedDocument reprint(String documentNumber) {
		GeneratedDocument copy = new GeneratedDocument(
				student,
				academicYear,
				documentType,
				documentNumber,
				LocalDate.now(),
				purpose,
				remarks,
				fileName,
				contentType,
				content);
		copy.status = GeneratedDocumentStatus.REPRINTED;
		copy.reprintOf = this;
		return copy;
	}

	private String trim(String value) {
		return value == null ? null : value.trim();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
