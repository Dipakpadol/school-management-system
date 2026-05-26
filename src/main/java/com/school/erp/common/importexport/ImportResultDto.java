package com.school.erp.common.importexport;

import java.util.List;
import java.util.UUID;

public record ImportResultDto(
		UUID batchId,
		int totalRows,
		int successRows,
		int failedRows,
		List<ImportErrorDto> errors) {

	public static ImportResultDto of(UUID batchId, int totalRows, int successRows, List<ImportErrorDto> errors) {
		return new ImportResultDto(
				batchId,
				totalRows,
				successRows,
				errors == null ? 0 : errors.size(),
				errors == null ? List.of() : List.copyOf(errors));
	}
}
