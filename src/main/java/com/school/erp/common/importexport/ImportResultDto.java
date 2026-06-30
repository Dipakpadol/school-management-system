package com.school.erp.common.importexport;

import java.util.List;
import java.util.UUID;

public record ImportResultDto(
		UUID batchId,
		int totalRows,
		int successRows,
		int failedRows,
		int warningRows,
		List<ImportErrorDto> errors) {

	public static ImportResultDto of(UUID batchId, int totalRows, int successRows, List<ImportErrorDto> errors) {
		List<ImportErrorDto> safeErrors = errors == null ? List.of() : List.copyOf(errors);
		int warningRows = (int) safeErrors.stream()
				.filter(ImportErrorDto::warning)
				.mapToInt(ImportErrorDto::rowNumber)
				.distinct()
				.count();
		int failedRows = (int) safeErrors.stream()
				.filter(error -> !error.warning())
				.mapToInt(ImportErrorDto::rowNumber)
				.distinct()
				.count();
		return new ImportResultDto(
				batchId,
				totalRows,
				successRows,
				failedRows,
				warningRows,
				safeErrors);
	}
}
