package com.school.erp.common.modules.application;

import com.school.erp.common.modules.api.dto.ModuleRecordResponse;
import com.school.erp.common.modules.domain.ModuleRecord;

import org.springframework.stereotype.Component;

@Component
public class ModuleRecordMapper {

	public ModuleRecordResponse toResponse(ModuleRecord record) {
		return new ModuleRecordResponse(
				record.getId(),
				record.getModuleName(),
				record.getRecordType(),
				record.getCode(),
				record.getName(),
				record.getDescription(),
				record.getStatus(),
				record.getParentId(),
				record.getOwnerId(),
				record.getRecordDate(),
				record.getAmount(),
				record.getMetadataJson(),
				record.isActive(),
				record.getCreatedAt(),
				record.getUpdatedAt(),
				record.getCreatedBy(),
				record.getUpdatedBy());
	}
}
