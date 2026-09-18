package com.school.erp.modules.library.infrastructure;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.library.domain.LibraryPublisher;

public interface LibraryPublisherRepository extends BaseRepository<LibraryPublisher, UUID> {

	List<LibraryPublisher> findAllByDeletedFalseOrderByNameAsc();

	Optional<LibraryPublisher> findByNameIgnoreCaseAndDeletedFalse(String name);
}
