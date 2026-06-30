package com.school.erp.common.importexport;

public record ImportErrorDto(
		int rowNumber,
		String fieldName,
		String errorMessage,
		String severity) {

	public static final String ERROR = "ERROR";
	public static final String WARNING = "WARNING";

	public ImportErrorDto(int rowNumber, String fieldName, String errorMessage) {
		this(rowNumber, fieldName, errorMessage, ERROR);
	}

	public static ImportErrorDto warning(int rowNumber, String fieldName, String errorMessage) {
		return new ImportErrorDto(rowNumber, fieldName, errorMessage, WARNING);
	}

	public boolean warning() {
		return WARNING.equalsIgnoreCase(severity);
	}
}
