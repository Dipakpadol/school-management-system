package com.school.erp.common.api;

public record FieldErrorResponse(String field, String message, Object rejectedValue) {
}
