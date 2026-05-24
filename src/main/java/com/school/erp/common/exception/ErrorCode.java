package com.school.erp.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Request validation failed"),
	MALFORMED_REQUEST("MALFORMED_REQUEST", HttpStatus.BAD_REQUEST, "Request payload is invalid"),
	UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
	INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid username or password"),
	INVALID_TOKEN("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Token is invalid"),
	TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Token has expired"),
	FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "You do not have permission to perform this action"),
	RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Requested resource was not found"),
	CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Request conflicts with the current resource state"),
	PASSWORD_POLICY_VIOLATION("PASSWORD_POLICY_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "Password policy validation failed"),
	BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.UNPROCESSABLE_ENTITY, "Business rule validation failed"),
	INTERNAL_ERROR("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");

	private final String code;
	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(String code, HttpStatus status, String defaultMessage) {
		this.code = code;
		this.status = status;
		this.defaultMessage = defaultMessage;
	}

	public String code() {
		return code;
	}

	public HttpStatus status() {
		return status;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}
