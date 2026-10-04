package com.school.erp.modules.communications.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.communications.domain.CommunicationAudienceType;
import com.school.erp.modules.communications.domain.CommunicationRecord;
import com.school.erp.modules.communications.domain.CommunicationStatus;
import com.school.erp.modules.communications.domain.CommunicationType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunicationRecordRepository extends BaseRepository<CommunicationRecord, UUID> {

	@Query("""
			select record
			from CommunicationRecord record
			where record.deleted = false
			  and (:type is null or record.type = :type)
			  and (:status is null or record.status = :status)
			""")
	Page<CommunicationRecord> search(
			@Param("type") CommunicationType type,
			@Param("status") CommunicationStatus status,
			Pageable pageable);

	@Query("""
			select record
			from CommunicationRecord record
			where record.deleted = false
			  and record.status = com.school.erp.modules.communications.domain.CommunicationStatus.PUBLISHED
			  and (record.publishAt is null or record.publishAt <= :now)
			  and (record.expiryAt is null or record.expiryAt >= :now)
			  and (
			    record.audienceType = com.school.erp.modules.communications.domain.CommunicationAudienceType.ALL
			    or record.audienceType in :audienceTypes
			    or (:classId is not null
			      and record.audienceType = com.school.erp.modules.communications.domain.CommunicationAudienceType.CLASS
			      and record.classId = :classId)
			    or (:classId is not null
			      and :sectionId is not null
			      and record.audienceType = com.school.erp.modules.communications.domain.CommunicationAudienceType.DIVISION
			      and record.classId = :classId
			      and record.sectionId = :sectionId)
			  )
			""")
	Page<CommunicationRecord> findVisibleForPortal(
			@Param("audienceTypes") Collection<CommunicationAudienceType> audienceTypes,
			@Param("classId") UUID classId,
			@Param("sectionId") UUID sectionId,
			@Param("now") Instant now,
			Pageable pageable);
}
