package com.school.erp.modules.library.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryBookCopy;
import com.school.erp.modules.library.domain.LibraryBookCopyStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBookCopyRepository extends BaseRepository<LibraryBookCopy, UUID> {

	Optional<LibraryBookCopy> findByAccessionNumberIgnoreCaseAndDeletedFalse(String accessionNumber);

	long countByStatusAndDeletedFalse(LibraryBookCopyStatus status);

	long countByBookIdAndDeletedFalse(UUID bookId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = { "book", "book.category", "book.publisher", "book.authors" })
	@Query("select copy from LibraryBookCopy copy where copy.id = :id and copy.deleted = false")
	Optional<LibraryBookCopy> findLockedById(@Param("id") UUID id);

	@EntityGraph(attributePaths = { "book", "book.category", "book.publisher" })
	@Query("""
			select copy
			from LibraryBookCopy copy
			join copy.book book
			where copy.deleted = false
			  and book.deleted = false
			  and (:bookId is null or book.id = :bookId)
			  and (:status is null or copy.status = :status)
			  and (:keyword is null
				or lower(copy.accessionNumber) like lower(concat('%', :keyword, '%'))
				or lower(book.title) like lower(concat('%', :keyword, '%')))
			""")
	Page<LibraryBookCopy> search(
			@Param("bookId") UUID bookId,
			@Param("status") LibraryBookCopyStatus status,
			@Param("keyword") String keyword,
			Pageable pageable);
}
