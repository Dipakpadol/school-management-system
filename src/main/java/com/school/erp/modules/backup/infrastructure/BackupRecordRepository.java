package com.school.erp.modules.backup.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.backup.domain.BackupRecord;
import com.school.erp.modules.backup.domain.BackupStatus;
import com.school.erp.modules.backup.domain.BackupType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackupRecordRepository extends BaseRepository<BackupRecord, UUID> {

	@Query("""
			select backup
			from BackupRecord backup
			where backup.deleted = false
			  and (:status is null or backup.status = :status)
			  and (:backupType is null or backup.backupType = :backupType)
			  and (:keywordLike is null
			    or lower(backup.fileName) like :keywordLike
			    or lower(backup.createdByUser) like :keywordLike
			    or lower(backup.databaseVersion) like :keywordLike)
			""")
	Page<BackupRecord> search(
			@Param("status") BackupStatus status,
			@Param("backupType") BackupType backupType,
			@Param("keywordLike") String keywordLike,
			Pageable pageable);

	Optional<BackupRecord> findTopByStatusInAndDeletedFalseOrderByCompletedAtDesc(Collection<BackupStatus> statuses);

	Optional<BackupRecord> findTopByStatusAndDeletedFalseOrderByCompletedAtDesc(BackupStatus status);

	long countByStatusAndDeletedFalse(BackupStatus status);

	@Query("""
			select coalesce(sum(backup.sizeBytes), 0)
			from BackupRecord backup
			where backup.deleted = false
			  and backup.status in :statuses
			""")
	long totalSizeByStatusIn(@Param("statuses") Collection<BackupStatus> statuses);

	List<BackupRecord> findAllByStatusInAndDeletedFalseOrderByCompletedAtDesc(Collection<BackupStatus> statuses);
}
