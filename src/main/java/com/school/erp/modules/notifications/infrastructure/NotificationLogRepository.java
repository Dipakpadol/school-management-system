package com.school.erp.modules.notifications.infrastructure;

import java.time.Instant;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.notifications.domain.NotificationChannel;
import com.school.erp.modules.notifications.domain.NotificationLog;
import com.school.erp.modules.notifications.domain.NotificationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationLogRepository extends BaseRepository<NotificationLog, UUID> {

	@Query("""
			select log
			from NotificationLog log
			where log.deleted = false
			  and (:channel is null or log.channel = :channel)
			  and (:status is null or log.status = :status)
			  and (:referenceType is null or log.referenceType = :referenceType)
			  and (:referenceId is null or log.referenceId = :referenceId)
			""")
	Page<NotificationLog> search(
			@Param("channel") NotificationChannel channel,
			@Param("status") NotificationStatus status,
			@Param("referenceType") String referenceType,
			@Param("referenceId") String referenceId,
			Pageable pageable);

	boolean existsByReferenceTypeAndReferenceIdAndChannelAndStatusAndCreatedAtBetweenAndDeletedFalse(
			String referenceType,
			String referenceId,
			NotificationChannel channel,
			NotificationStatus status,
			Instant from,
			Instant to);
}
