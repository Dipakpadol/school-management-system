package com.school.erp.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import org.mockito.Mockito;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void paymentReferenceUniqueConstraintReturnsValidationError() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		when(request.getRequestURI()).thenReturn("/v1/fees/assignments/assignment-id/payments");
		DataIntegrityViolationException exception = new DataIntegrityViolationException(
				"could not execute statement",
				new RuntimeException("duplicate key value violates unique constraint \"ux_fee_payments_reference_active\""));

		var response = handler.handleDataIntegrityViolation(exception, request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().code()).isEqualTo("VALIDATION_ERROR");
		assertThat(response.getBody().message()).isEqualTo("Reference number already exists for this payment mode.");
	}
}
