package com.school.erp.modules.library.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryLoan;
import com.school.erp.modules.library.domain.LibraryLoanStatus;
import com.school.erp.modules.library.domain.LibraryMemberType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryLoanRepository extends BaseRepository<LibraryLoan, UUID> {

	@EntityGraph(attributePaths = { "copy", "copy.book", "membership" })
	Optional<LibraryLoan> findFirstByCopyIdAndStatusAndDeletedFalse(UUID copyId, LibraryLoanStatus status);

	long countByMembershipIdAndStatusAndDeletedFalse(UUID membershipId, LibraryLoanStatus status);

	long countByStatusAndDueDateBeforeAndDeletedFalse(LibraryLoanStatus status, LocalDate dueDate);

	@EntityGraph(attributePaths = { "copy", "copy.book", "membership" })
	@Query("""
			select loan
			from LibraryLoan loan
			join loan.copy copy
			join copy.book book
			join loan.membership membership
			where loan.deleted = false
			  and (:status is null or loan.status = :status)
			  and (:membershipId is null or membership.id = :membershipId)
			  and (:bookId is null or book.id = :bookId)
			  and (:copyId is null or copy.id = :copyId)
			  and (:memberType is null or membership.memberType = :memberType)
			  and (:fromDate is null or loan.issueDate >= :fromDate)
			  and (:toDate is null or loan.issueDate <= :toDate)
			  and (:overdueOnly = false or (loan.status = com.school.erp.modules.library.domain.LibraryLoanStatus.ACTIVE and loan.dueDate < :today))
			""")
	Page<LibraryLoan> search(
			@Param("status") LibraryLoanStatus status,
			@Param("membershipId") UUID membershipId,
			@Param("bookId") UUID bookId,
			@Param("copyId") UUID copyId,
			@Param("memberType") LibraryMemberType memberType,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("overdueOnly") boolean overdueOnly,
			@Param("today") LocalDate today,
			Pageable pageable);

	@EntityGraph(attributePaths = { "copy", "copy.book", "membership" })
	@Query("""
			select loan
			from LibraryLoan loan
			join loan.copy copy
			join copy.book book
			join loan.membership membership
			where loan.deleted = false
			  and (:status is null or loan.status = :status)
			  and (:membershipId is null or membership.id = :membershipId)
			  and (:bookId is null or book.id = :bookId)
			  and (:memberType is null or membership.memberType = :memberType)
			  and (:fromDate is null or loan.issueDate >= :fromDate)
			  and (:toDate is null or loan.issueDate <= :toDate)
			  and (:overdueOnly = false or (loan.status = com.school.erp.modules.library.domain.LibraryLoanStatus.ACTIVE and loan.dueDate < :today))
			order by loan.issueDate desc, loan.dueDate desc
			""")
	List<LibraryLoan> reportRows(
			@Param("status") LibraryLoanStatus status,
			@Param("membershipId") UUID membershipId,
			@Param("bookId") UUID bookId,
			@Param("memberType") LibraryMemberType memberType,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			@Param("overdueOnly") boolean overdueOnly,
			@Param("today") LocalDate today);
}
