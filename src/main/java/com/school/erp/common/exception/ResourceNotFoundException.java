package com.school.erp.common.exception;

public class ResourceNotFoundException extends BusinessException {

	public ResourceNotFoundException(String resourceName, Object identifier) {
		super(ErrorCode.RESOURCE_NOT_FOUND, resourceName + " not found for identifier: " + identifier);
	}
}
