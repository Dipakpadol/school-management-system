package com.school.erp.modules.library.api.dto;

import java.util.UUID;

public record LibraryAuthorResponse(
		UUID id,
		String name,
		String biography,
		boolean active) {
}
