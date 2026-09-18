package com.school.erp.modules.library.api.dto;

import java.util.UUID;

public record LibraryCategoryResponse(
		UUID id,
		String name,
		String description,
		boolean active) {
}
