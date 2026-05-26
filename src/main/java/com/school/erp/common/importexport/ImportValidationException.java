package com.school.erp.common.importexport;

import lombok.Getter;

@Getter
public class ImportValidationException extends RuntimeException {

	private final ImportResultDto result;

	public ImportValidationException(ImportResultDto result) {
		super("Import validation failed.");
		this.result = result;
	}
}
