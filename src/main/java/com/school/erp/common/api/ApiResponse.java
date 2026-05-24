package com.school.erp.common.api;

import java.time.Instant;

public record ApiResponse<T>(
		boolean success,
		String message,
		T data,
		Instant timestamp,
		String path,
		String traceId) {

	public static <T> ApiResponse<T> success(T data, String message, String path, String traceId) {
		return new ApiResponse<>(true, message, data, Instant.now(), path, traceId);
	}
}
