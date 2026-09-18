package com.school.erp.modules.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryBookCategory;

public interface LibraryBookCategoryRepository extends BaseRepository<LibraryBookCategory, UUID> {

	List<LibraryBookCategory> findAllByDeletedFalseOrderByNameAsc();

	Optional<LibraryBookCategory> findByNameIgnoreCaseAndDeletedFalse(String name);
}
