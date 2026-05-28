package com.school.erp.common.exception;

import java.util.List;

import com.school.erp.common.api.ErrorResponse;
import com.school.erp.common.api.FieldErrorResponse;
import com.school.erp.common.importexport.ImportValidationException;
import com.school.erp.common.web.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage(), error.getRejectedValue()))
				.toList();
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), request, errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		List<FieldErrorResponse> errors = ex.getConstraintViolations().stream()
				.map(violation -> new FieldErrorResponse(
						violation.getPropertyPath().toString(),
						violation.getMessage(),
						violation.getInvalidValue()))
				.toList();
		return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), request, errors);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(ex.getErrorCode(), ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
		return build(ex.getErrorCode(), ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(ImportValidationException.class)
	ResponseEntity<ErrorResponse> handleImportValidation(ImportValidationException ex, HttpServletRequest request) {
		List<FieldErrorResponse> errors = ex.getResult().errors().stream()
				.map(error -> new FieldErrorResponse(
						"row[" + error.rowNumber() + "]." + error.fieldName(),
						error.errorMessage(),
						null))
				.toList();
		return build(ErrorCode.VALIDATION_ERROR, ex.getMessage(), request, errors);
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
		return build(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage(), request, List.of());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), request, List.of());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return build(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.defaultMessage(), request, List.of());
	}

	@ExceptionHandler({ HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class })
	ResponseEntity<ErrorResponse> handleBadHttpContract(Exception ex, HttpServletRequest request) {
		return build(ErrorCode.MALFORMED_REQUEST, ex.getMessage(), request, List.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
			DataIntegrityViolationException ex,
			HttpServletRequest request) {
		String message = ex.getMostSpecificCause() == null ? ex.getMessage() : ex.getMostSpecificCause().getMessage();
		if (message != null && message.contains("ux_fee_payments_reference_active")) {
			return build(
					ErrorCode.VALIDATION_ERROR,
					"Reference number already exists for this payment mode.",
					request,
					List.of());
		}
		return build(ErrorCode.CONFLICT, ErrorCode.CONFLICT.defaultMessage(), request, List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), request, List.of());
	}

	private ResponseEntity<ErrorResponse> build(
			ErrorCode errorCode,
			String message,
			HttpServletRequest request,
			List<FieldErrorResponse> errors) {
		HttpStatus status = errorCode.status();
		ErrorResponse response = ErrorResponse.of(
				errorCode.code(),
				message,
				status.value(),
				request.getRequestURI(),
				MDC.get(CorrelationIdFilter.CORRELATION_ID),
				errors);
		return ResponseEntity.status(status).body(response);
	}
}
