package com.school.erp.modules.library.domain;

import java.math.BigDecimal;
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
@Table(name = "library_fines")
@SQLRestriction("deleted = false")
public class LibraryFine extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loan_id", nullable = false)
	private LibraryLoan loan;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount = BigDecimal.ZERO;

	@Column(length = 500)
	private String reason;

	@Column(name = "fine_date", nullable = false)
	private LocalDate fineDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LibraryFineStatus status = LibraryFineStatus.PENDING;

	@Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal paidAmount = BigDecimal.ZERO;

	@Column(name = "paid_at")
	private Instant paidAt;

	@Column(name = "collected_by", length = 100)
	private String collectedBy;

	@Column(name = "waived_at")
	private Instant waivedAt;

	@Column(name = "waived_by", length = 100)
	private String waivedBy;

	public LibraryFine(LibraryLoan loan, BigDecimal amount, String reason, LocalDate fineDate) {
		this.loan = loan;
		this.amount = amount == null ? BigDecimal.ZERO : amount;
		this.reason = trimToNull(reason);
		this.fineDate = fineDate;
		this.status = LibraryFineStatus.PENDING;
	}

	public void updateAmount(BigDecimal amount, String reason, LocalDate fineDate) {
		if (status != LibraryFineStatus.PENDING) {
			return;
		}
		this.amount = amount == null ? BigDecimal.ZERO : amount;
		this.reason = trimToNull(reason);
		this.fineDate = fineDate;
	}

	public void pay(BigDecimal paidAmount, String collectedBy) {
		this.paidAmount = paidAmount == null ? amount : paidAmount;
		this.collectedBy = trimToNull(collectedBy);
		this.paidAt = Instant.now();
		this.status = LibraryFineStatus.PAID;
	}

	public void waive(String waivedBy) {
		this.waivedBy = trimToNull(waivedBy);
		this.waivedAt = Instant.now();
		this.status = LibraryFineStatus.WAIVED;
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
