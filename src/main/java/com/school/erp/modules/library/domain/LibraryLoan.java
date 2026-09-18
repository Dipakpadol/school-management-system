package com.school.erp.modules.library.domain;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "library_loans")
@SQLRestriction("deleted = false")
public class LibraryLoan extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "copy_id", nullable = false)
	private LibraryBookCopy copy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "membership_id", nullable = false)
	private LibraryMembership membership;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "return_date")
	private LocalDate returnDate;

	@Column(name = "returned_at")
	private Instant returnedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LibraryLoanStatus status = LibraryLoanStatus.ACTIVE;

	@Column(name = "issued_by", length = 100)
	private String issuedBy;

	@Column(name = "returned_by", length = 100)
	private String returnedBy;

	@Column(length = 500)
	private String remarks;

	public LibraryLoan(
			LibraryBookCopy copy,
			LibraryMembership membership,
			LocalDate issueDate,
			LocalDate dueDate,
			String issuedBy,
			String remarks) {
		this.copy = copy;
		this.membership = membership;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
		this.issuedBy = trimToNull(issuedBy);
		this.remarks = trimToNull(remarks);
		this.status = LibraryLoanStatus.ACTIVE;
	}

	public boolean isOpen() {
		return status == LibraryLoanStatus.ACTIVE || status == LibraryLoanStatus.LOST;
	}

	public void markReturned(LocalDate returnDate, String returnedBy, String remarks) {
		this.returnDate = returnDate;
		this.returnedAt = Instant.now();
		this.returnedBy = trimToNull(returnedBy);
		this.remarks = trimToNull(remarks);
		this.status = LibraryLoanStatus.RETURNED;
	}

	public void markLost(String remarks) {
		this.status = LibraryLoanStatus.LOST;
		this.remarks = trimToNull(remarks);
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
