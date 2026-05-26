package com.school.erp.common.modules.api.dto;

public record ModuleRecordCountResponse(
		String moduleName,
		String recordType,
		long totalRecords,
		long activeRecords) {
}
