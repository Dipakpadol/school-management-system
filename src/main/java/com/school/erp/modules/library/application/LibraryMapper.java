package com.school.erp.modules.library.application;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.school.erp.modules.library.api.dto.LibraryAuthorResponse;
import com.school.erp.modules.library.api.dto.LibraryBookCopyResponse;
import com.school.erp.modules.library.api.dto.LibraryBookResponse;
import com.school.erp.modules.library.api.dto.LibraryCategoryResponse;
import com.school.erp.modules.library.api.dto.LibraryFineResponse;
import com.school.erp.modules.library.api.dto.LibraryLoanResponse;
import com.school.erp.modules.library.api.dto.LibraryMembershipResponse;
import com.school.erp.modules.library.api.dto.LibraryPublisherResponse;
import com.school.erp.modules.library.domain.LibraryAuthor;
import com.school.erp.modules.library.domain.LibraryBook;
import com.school.erp.modules.library.domain.LibraryBookCategory;
import com.school.erp.modules.library.domain.LibraryBookCopy;
import com.school.erp.modules.library.domain.LibraryFine;
import com.school.erp.modules.library.domain.LibraryLoan;
import com.school.erp.modules.library.domain.LibraryMembership;
import com.school.erp.modules.library.domain.LibraryPublisher;

import org.springframework.stereotype.Component;

@Component
public class LibraryMapper {

	public LibraryCategoryResponse toCategoryResponse(LibraryBookCategory category) {
		return new LibraryCategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				category.isActive());
	}

	public LibraryAuthorResponse toAuthorResponse(LibraryAuthor author) {
		return new LibraryAuthorResponse(
				author.getId(),
				author.getName(),
				author.getBiography(),
				author.isActive());
	}

	public LibraryPublisherResponse toPublisherResponse(LibraryPublisher publisher) {
		return new LibraryPublisherResponse(
				publisher.getId(),
				publisher.getName(),
				publisher.getContactInfo(),
				publisher.isActive());
	}

	public LibraryBookResponse toBookResponse(LibraryBook book) {
		LibraryBookCategory category = book.getCategory();
		LibraryPublisher publisher = book.getPublisher();
		return new LibraryBookResponse(
				book.getId(),
				book.getTitle(),
				book.getIsbn(),
				category == null ? null : category.getId(),
				category == null ? null : category.getName(),
				publisher == null ? null : publisher.getId(),
				publisher == null ? null : publisher.getName(),
				book.getAuthors().stream()
						.map(this::toAuthorResponse)
						.sorted(java.util.Comparator.comparing(LibraryAuthorResponse::name))
						.toList(),
				book.getEdition(),
				book.getPublicationYear(),
				book.getLanguage(),
				book.getDescription(),
				book.getShelfLocation(),
				book.isActive());
	}

	public LibraryBookCopyResponse toCopyResponse(LibraryBookCopy copy) {
		return new LibraryBookCopyResponse(
				copy.getId(),
				copy.getBook().getId(),
				copy.getBook().getTitle(),
				copy.getAccessionNumber(),
				copy.getStatus(),
				copy.getShelfLocation(),
				copy.getAcquiredOn(),
				copy.getPrice(),
				copy.getConditionNote());
	}

	public LibraryMembershipResponse toMembershipResponse(
			LibraryMembership membership,
			String memberCode,
			String memberName) {
		return new LibraryMembershipResponse(
				membership.getId(),
				membership.getMemberType(),
				membership.memberId(),
				memberCode,
				memberName,
				membership.getMembershipNumber(),
				membership.getStartDate(),
				membership.getExpiryDate(),
				membership.isActive(),
				membership.getNotes());
	}

	public LibraryLoanResponse toLoanResponse(
			LibraryLoan loan,
			String memberName,
			boolean overdue,
			BigDecimal pendingFine) {
		LibraryBookCopy copy = loan.getCopy();
		LibraryBook book = copy.getBook();
		return new LibraryLoanResponse(
				loan.getId(),
				copy.getId(),
				copy.getAccessionNumber(),
				book.getId(),
				book.getTitle(),
				loan.getMembership().getId(),
				loan.getMembership().getMembershipNumber(),
				loan.getMembership().getMemberType(),
				memberName,
				loan.getIssueDate(),
				loan.getDueDate(),
				loan.getReturnDate(),
				loan.getStatus(),
				overdue,
				pendingFine == null ? BigDecimal.ZERO : pendingFine,
				loan.getRemarks());
	}

	public LibraryFineResponse toFineResponse(LibraryFine fine, String memberName) {
		LibraryLoan loan = fine.getLoan();
		return new LibraryFineResponse(
				fine.getId(),
				loan.getId(),
				loan.getCopy().getAccessionNumber(),
				loan.getCopy().getBook().getTitle(),
				loan.getMembership().getMembershipNumber(),
				memberName,
				fine.getAmount(),
				fine.getPaidAmount(),
				fine.getReason(),
				fine.getFineDate(),
				fine.getStatus());
	}

	public boolean isOverdue(LibraryLoan loan, LocalDate today) {
		return loan.getReturnDate() == null && loan.getDueDate().isBefore(today);
	}
}
