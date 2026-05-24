package com.school.erp.common.exception;

import java.util.Map;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.defaultMessage(), Map.of());
	}

	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode, message, Map.of());
	}

	public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.details = Map.copyOf(details);
	}
}
