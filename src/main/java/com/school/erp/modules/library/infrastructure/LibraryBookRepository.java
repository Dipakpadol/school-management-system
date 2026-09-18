package com.school.erp.modules.library.infrastructure;

import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryBook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LibraryBookRepository extends BaseRepository<LibraryBook, UUID> {

	Optional<LibraryBook> findByIsbnIgnoreCaseAndDeletedFalse(String isbn);

	@EntityGraph(attributePaths = { "category", "publisher", "authors" })
	@Query("""
			select distinct book
			from LibraryBook book
			left join book.authors author
			where book.deleted = false
			  and (:keyword is null
				or lower(book.title) like lower(concat('%', :keyword, '%'))
				or lower(book.isbn) like lower(concat('%', :keyword, '%'))
				or lower(author.name) like lower(concat('%', :keyword, '%')))
			  and (:categoryId is null or book.category.id = :categoryId)
			  and (:publisherId is null or book.publisher.id = :publisherId)
			  and (:active is null or book.active = :active)
			""")
	Page<LibraryBook> search(
			@Param("keyword") String keyword,
			@Param("categoryId") UUID categoryId,
			@Param("publisherId") UUID publisherId,
			@Param("active") Boolean active,
			Pageable pageable);
}
