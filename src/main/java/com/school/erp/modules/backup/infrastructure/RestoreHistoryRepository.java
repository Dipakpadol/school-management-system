package com.school.erp.modules.backup.infrastructure;

import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.backup.domain.RestoreHistory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RestoreHistoryRepository extends BaseRepository<RestoreHistory, UUID> {

	Page<RestoreHistory> findAllByDeletedFalse(Pageable pageable);
}
