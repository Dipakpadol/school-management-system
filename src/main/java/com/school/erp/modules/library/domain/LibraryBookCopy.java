package com.school.erp.modules.library.domain;

import java.math.BigDecimal;
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
@Table(name = "library_book_copies")
@SQLRestriction("deleted = false")
public class LibraryBookCopy extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "book_id", nullable = false)
	private LibraryBook book;

	@Column(name = "accession_number", nullable = false, length = 60)
	private String accessionNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LibraryBookCopyStatus status = LibraryBookCopyStatus.AVAILABLE;

	@Column(name = "shelf_location", length = 80)
	private String shelfLocation;

	@Column(name = "acquired_on")
	private LocalDate acquiredOn;

	@Column(precision = 12, scale = 2)
	private BigDecimal price;

	@Column(name = "condition_note", length = 500)
	private String conditionNote;

	public LibraryBookCopy(
			LibraryBook book,
			String accessionNumber,
			String shelfLocation,
			LocalDate acquiredOn,
			BigDecimal price,
			String conditionNote,
			LibraryBookCopyStatus status) {
		update(book, accessionNumber, shelfLocation, acquiredOn, price, conditionNote, status);
	}

	public void update(
			LibraryBook book,
			String accessionNumber,
			String shelfLocation,
			LocalDate acquiredOn,
			BigDecimal price,
			String conditionNote,
			LibraryBookCopyStatus status) {
		this.book = book;
		this.accessionNumber = normalizeAccession(accessionNumber);
		this.shelfLocation = trimToNull(shelfLocation);
		this.acquiredOn = acquiredOn;
		this.price = price;
		this.conditionNote = trimToNull(conditionNote);
		this.status = status == null ? LibraryBookCopyStatus.AVAILABLE : status;
	}

	public void markIssued() {
		status = LibraryBookCopyStatus.ISSUED;
	}

	public void markAvailable() {
		status = LibraryBookCopyStatus.AVAILABLE;
	}

	public void markLost(String conditionNote) {
		status = LibraryBookCopyStatus.LOST;
		this.conditionNote = trimToNull(conditionNote);
	}

	public void markDamaged(String conditionNote) {
		status = LibraryBookCopyStatus.DAMAGED;
		this.conditionNote = trimToNull(conditionNote);
	}

	public void withdraw(String conditionNote) {
		status = LibraryBookCopyStatus.WITHDRAWN;
		this.conditionNote = trimToNull(conditionNote);
	}

	private String normalizeAccession(String value) {
		return value == null ? null : value.trim().toUpperCase();
	}

	private String trimToNull(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
