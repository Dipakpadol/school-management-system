package com.school.erp.common.importexport;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;

public record ExportRequestDto(
		String search,
		String moduleName,
		String format,
		List<String> columns,
		Map<String, String> filters,
		String sortBy,
		Sort.Direction direction) {
}
