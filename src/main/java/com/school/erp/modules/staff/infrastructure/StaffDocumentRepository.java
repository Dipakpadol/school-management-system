package com.school.erp.modules.staff.infrastructure;

import java.util.List;
import java.util.UUID;

import com.school.erp.common.domain.BaseRepository;
import com.school.erp.modules.staff.domain.StaffDocument;

public interface StaffDocumentRepository extends BaseRepository<StaffDocument, UUID> {

	List<StaffDocument> findByStaffIdAndDeletedFalseOrderByUploadedAtDesc(UUID staffId);
}
