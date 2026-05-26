package com.school.erp.common.importexport;

public record ImportErrorDto(
		int rowNumber,
		String fieldName,
		String errorMessage) {
}
