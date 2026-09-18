package com.school.erp.modules.communications.infrastructure;

import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
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
}
