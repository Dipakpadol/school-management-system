package com.school.erp.modules.library.api.dto;

import java.util.List;
import java.util.UUID;

public record LibraryBookResponse(
		UUID id,
		String title,
		String isbn,
		UUID categoryId,
		String categoryName,
		UUID publisherId,
		String publisherName,
		List<LibraryAuthorResponse> authors,
		String edition,
		Integer publicationYear,
		String language,
		String description,
		String shelfLocation,
		boolean active) {
}
