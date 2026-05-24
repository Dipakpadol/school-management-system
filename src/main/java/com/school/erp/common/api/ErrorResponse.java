package com.school.erp.common.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
		boolean success,
		String code,
		String message,
		int status,
		String path,
		String traceId,
		Instant timestamp,
		List<FieldErrorResponse> errors) {

	public static ErrorResponse of(
			String code,
			String message,
			int status,
			String path,
			String traceId,
			List<FieldErrorResponse> errors) {
		return new ErrorResponse(false, code, message, status, path, traceId, Instant.now(), errors);
	}
}
