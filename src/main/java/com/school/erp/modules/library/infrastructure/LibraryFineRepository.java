package com.school.erp.modules.library.infrastructure;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryFine;
import com.school.erp.modules.library.domain.LibraryFineStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryFineRepository extends BaseRepository<LibraryFine, UUID> {

	@EntityGraph(attributePaths = { "loan", "loan.copy", "loan.copy.book", "loan.membership" })
	Optional<LibraryFine> findByLoanIdAndDeletedFalse(UUID loanId);

	@Query("""
			select coalesce(sum(fine.amount - fine.paidAmount), 0)
			from LibraryFine fine
			where fine.deleted = false
			  and fine.status = com.school.erp.modules.library.domain.LibraryFineStatus.PENDING
			""")
	BigDecimal pendingAmount();

	@EntityGraph(attributePaths = { "loan", "loan.copy", "loan.copy.book", "loan.membership" })
	@Query("""
			select fine
			from LibraryFine fine
			join fine.loan loan
			join loan.copy copy
			join copy.book book
			join loan.membership membership
			where fine.deleted = false
			  and (:status is null or fine.status = :status)
			  and (:membershipId is null or membership.id = :membershipId)
			  and (:fromDate is null or fine.fineDate >= :fromDate)
			  and (:toDate is null or fine.fineDate <= :toDate)
			""")
	Page<LibraryFine> search(
			@Param("status") LibraryFineStatus status,
			@Param("membershipId") UUID membershipId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate,
			Pageable pageable);

	@EntityGraph(attributePaths = { "loan", "loan.copy", "loan.copy.book", "loan.membership" })
	@Query("""
			select fine
			from LibraryFine fine
			join fine.loan loan
			join loan.membership membership
			where fine.deleted = false
			  and (:status is null or fine.status = :status)
			  and (:membershipId is null or membership.id = :membershipId)
			  and (:fromDate is null or fine.fineDate >= :fromDate)
			  and (:toDate is null or fine.fineDate <= :toDate)
			order by fine.fineDate desc
			""")
	List<LibraryFine> reportRows(
			@Param("status") LibraryFineStatus status,
			@Param("membershipId") UUID membershipId,
			@Param("fromDate") LocalDate fromDate,
			@Param("toDate") LocalDate toDate);
}
