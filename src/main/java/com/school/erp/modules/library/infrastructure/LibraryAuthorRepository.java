package com.school.erp.modules.library.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryAuthor;

public interface LibraryAuthorRepository extends BaseRepository<LibraryAuthor, UUID> {

	List<LibraryAuthor> findAllByDeletedFalseOrderByNameAsc();

	List<LibraryAuthor> findByIdInAndDeletedFalse(Collection<UUID> ids);

	Optional<LibraryAuthor> findByNameIgnoreCaseAndDeletedFalse(String name);
}
